package bo.edu.usfx.jgroups.remate;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.Util;

public class NodoRemate implements Receiver {

    private JChannel canal;
    private final String miNombre;

    private final Map<String, Subasta> subastas = new ConcurrentHashMap<>();

    private final Map<String, ScheduledFuture<?>> temporizadores = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final AtomicBoolean eraCoordinador = new AtomicBoolean(false);

    public NodoRemate(String nombre) {
        this.miNombre = nombre;
    }

    public void iniciar() throws Exception {
        canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(miNombre);
        canal.setReceiver(this);
        canal.connect("RemateSIS258");
        canal.getState(null, 10000);
    }

    public void cerrar() {
        scheduler.shutdownNow();
        canal.close();
    }

    public String getMiNombre() {
        return miNombre;
    }

    public Address getMiDireccion() {
        return canal.getAddress();
    }

    public boolean soyCoordinador() {
        return canal.getAddress().equals(canal.getView().getCoord());
    }

    @Override
    public void viewAccepted(View vista) {
        boolean soyCoordinadorAhora = canal.getAddress() != null
                && canal.getAddress().equals(vista.getCoord());

        System.out.println("** Vista " + vista.getViewId().getId()
                + " | coordinador: " + vista.getCoord()
                + " | miembros: " + vista.getMembers());

        boolean antes = eraCoordinador.getAndSet(soyCoordinadorAhora);

        if (soyCoordinadorAhora && !antes) {
            System.out.println("** Ahora YO soy el coordinador. Retomando temporizadores...");
            retomarTemporizadoresComoNuevoCoordinador();
        }
        if (!soyCoordinadorAhora && antes) {
            cancelarTodosMisTemporizadores();
        }
    }

    private void retomarTemporizadoresComoNuevoCoordinador() {
        long ahora = System.currentTimeMillis();
        for (Subasta s : subastas.values()) {
            if (s.isCerrada()) continue;

            long restanteMs = s.getInstanteCierreMillis() - ahora;
            if (restanteMs <= 0) {
                cerrarSubastaComoCoordinador(s.getArticulo());
            } else {
                programarCierre(s.getArticulo(), restanteMs);
            }
        }
    }

    private void cancelarTodosMisTemporizadores() {
        temporizadores.values().forEach(f -> f.cancel(false));
        temporizadores.clear();
    }

    @Override
    public void getState(OutputStream salida) throws Exception {
        Map<String, Subasta> copia = new LinkedHashMap<>(subastas);
        Util.objectToStream(copia, new DataOutputStream(salida));
        System.out.println("** Estado enviado a un nuevo miembro (" + copia.size() + " subastas)");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setState(InputStream entrada) throws Exception {
        Map<String, Subasta> recibido = Util.objectFromStream(new DataInputStream(entrada));
        subastas.clear();
        subastas.putAll(recibido);
        System.out.println("** Estado recibido: " + recibido.size() + " subastas");
        recibido.values().forEach(s -> System.out.println("   " + s));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void receive(Message msg) {
        Object payload = msg.getObject();
        if (!(payload instanceof MensajeRemate)) {
            return;
        }
        MensajeRemate m = (MensajeRemate) payload;

        switch (m.tipo) {
            case PROPUESTA_SUBASTA:
            case PROPUESTA_PUJA:
            case PROPUESTA_EXTENSION:
                if (soyCoordinador()) procesarPropuesta(m, msg.getSrc());
                break;
            case NUEVA_SUBASTA:
                aplicarNuevaSubasta(m);
                break;
            case PUJA_ACEPTADA:
                aplicarPujaAceptada(m);
                break;
            case CIERRE:
                aplicarCierre(m);
                break;
            case EXTENSION:
                aplicarExtension(m);
                break;
            case RECHAZO:
                System.out.println("** RECHAZADO (" + m.articulo + "): " + m.motivo);
                break;
        }
    }

    private void procesarPropuesta(MensajeRemate m, Address remitente) {
        switch (m.tipo) {
            case PROPUESTA_SUBASTA:
                procesarPropuestaSubasta(m, remitente);
                break;
            case PROPUESTA_PUJA:
                procesarPropuestaPuja(m, remitente);
                break;
            case PROPUESTA_EXTENSION:
                procesarPropuestaExtension(m, remitente);
                break;
            default:
                break;
        }
    }

    private void procesarPropuestaSubasta(MensajeRemate m, Address remitente) {
        if (subastas.containsKey(m.articulo)) {
            enviarRechazo(remitente, m.articulo, "ya existe una subasta con ese articulo");
            return;
        }
        if (m.precioBase <= 0 || m.segundosDuracion <= 0) {
            enviarRechazo(remitente, m.articulo, "precio o duracion invalidos");
            return;
        }
        long instanteCierre = System.currentTimeMillis() + (m.segundosDuracion * 1000L);
        multicast(MensajeRemate.nuevaSubasta(m.articulo, m.precioBase, m.creador, instanteCierre));
    }

    private void procesarPropuestaPuja(MensajeRemate m, Address remitente) {
        Subasta s = subastas.get(m.articulo);
        if (s == null) {
            enviarRechazo(remitente, m.articulo, "la subasta no existe");
            return;
        }
        if (s.isCerrada()) {
            enviarRechazo(remitente, m.articulo, "la subasta ya cerro");
            return;
        }
        if (m.monto <= s.getMontoActual()) {
            enviarRechazo(remitente, m.articulo, "el monto debe superar " + s.getMontoActual());
            return;
        }
        multicast(MensajeRemate.pujaAceptada(m.articulo, m.monto, m.pujador));
    }

    private void procesarPropuestaExtension(MensajeRemate m, Address remitente) {
        Subasta s = subastas.get(m.articulo);
        if (s == null) {
            enviarRechazo(remitente, m.articulo, "la subasta no existe");
            return;
        }
        if (s.isCerrada()) {
            enviarRechazo(remitente, m.articulo, "la subasta ya cerro, no se puede extender");
            return;
        }
        if (!s.getCreador().equals(m.creador)) {
            enviarRechazo(remitente, m.articulo, "solo el creador puede extender la subasta");
            return;
        }
        long nuevoInstante = s.getInstanteCierreMillis() + (m.segundosDuracion * 1000L);
        multicast(MensajeRemate.extension(m.articulo, nuevoInstante));
    }

    private void cerrarSubastaComoCoordinador(String articulo) {
        Subasta s = subastas.get(articulo);
        if (s == null || s.isCerrada()) {
            return;
        }
        Puja mejor = s.getMejorPuja();
        String ganador = mejor != null ? mejor.getPujador() : null;
        Double montoFinal = mejor != null ? mejor.getMonto() : null;
        multicast(MensajeRemate.cierre(articulo, ganador, montoFinal));
    }

    private void aplicarNuevaSubasta(MensajeRemate m) {
        Subasta s = new Subasta(m.articulo, m.precioBase, m.creador, m.instanteCierreMillis);
        subastas.put(m.articulo, s);
        System.out.println("** Nueva subasta: " + s);

        if (soyCoordinador()) {
            long restante = m.instanteCierreMillis - System.currentTimeMillis();
            programarCierre(m.articulo, Math.max(restante, 0));
        }
    }

    private void aplicarPujaAceptada(MensajeRemate m) {
        Subasta s = subastas.get(m.articulo);
        if (s == null) return;

        synchronized (s) {
            s.getHistorial().add(new Puja(m.pujador, m.monto, System.currentTimeMillis()));
        }
        System.out.println("** Puja aceptada en '" + m.articulo + "': " + m.pujador + " -> " + m.monto);
    }

    private void aplicarCierre(MensajeRemate m) {
        Subasta s = subastas.get(m.articulo);
        if (s == null) return;

        s.setCerrada(true);
        s.setGanador(m.ganador);
        s.setMontoFinal(m.montoFinal);

        ScheduledFuture<?> f = temporizadores.remove(m.articulo);
        if (f != null) f.cancel(false);

        if (m.ganador != null) {
            System.out.println("** CIERRE: '" + m.articulo + "' ganada por " + m.ganador + " en " + m.montoFinal);
        } else {
            System.out.println("** CIERRE: '" + m.articulo + "' sin pujas, quedo desierta");
        }
    }

    private void aplicarExtension(MensajeRemate m) {
        Subasta s = subastas.get(m.articulo);
        if (s == null) return;

        s.setInstanteCierreMillis(m.instanteCierreMillis);
        System.out.println("** Extension aplicada a '" + m.articulo + "'. Nuevo cierre en "
                + s.segundosRestantes() + "s");

        if (soyCoordinador()) {
            ScheduledFuture<?> anterior = temporizadores.remove(m.articulo);
            if (anterior != null) anterior.cancel(false);
            long restante = m.instanteCierreMillis - System.currentTimeMillis();
            programarCierre(m.articulo, Math.max(restante, 0));
        }
    }

    private void programarCierre(String articulo, long retrasoMs) {
        ScheduledFuture<?> anterior = temporizadores.remove(articulo);
        if (anterior != null) anterior.cancel(false);

        ScheduledFuture<?> futuro = scheduler.schedule(() -> {
            if (soyCoordinador()) {
                cerrarSubastaComoCoordinador(articulo);
            }
        }, retrasoMs, TimeUnit.MILLISECONDS);

        temporizadores.put(articulo, futuro);
    }

    private void multicast(MensajeRemate m) {
        try {
            canal.send(new ObjectMessage(null, m));
        } catch (Exception e) {
            System.out.println("Error enviando multicast: " + e.getMessage());
        }
    }

    private void unicastAlCoordinador(MensajeRemate m) {
        try {
            Address coord = canal.getView().getCoord();
            if (coord.equals(canal.getAddress())) {
                procesarPropuesta(m, canal.getAddress());
            } else {
                canal.send(new ObjectMessage(coord, m));
            }
        } catch (Exception e) {
            System.out.println("Error enviando al coordinador: " + e.getMessage());
        }
    }

    private void enviarRechazo(Address destino, String articulo, String motivo) {
        if (destino.equals(canal.getAddress())) {
            System.out.println("** RECHAZADO (" + articulo + "): " + motivo);
            return;
        }
        try {
            canal.send(new ObjectMessage(destino, MensajeRemate.rechazo(articulo, motivo)));
        } catch (Exception e) {
            System.out.println("Error enviando rechazo: " + e.getMessage());
        }
    }

    public void crearSubasta(String articulo, double precioBase, int segundos) {
        unicastAlCoordinador(MensajeRemate.propuestaSubasta(articulo, precioBase, segundos, miNombre));
    }

    public void pujar(String articulo, double monto) {
        unicastAlCoordinador(MensajeRemate.propuestaPuja(articulo, monto, miNombre));
    }

    public void extender(String articulo, int segundos) {
        unicastAlCoordinador(MensajeRemate.propuestaExtension(articulo, segundos, miNombre));
    }

    public void listarSubastas() {
        if (subastas.isEmpty()) {
            System.out.println("(no hay subastas todavia)");
            return;
        }
        subastas.values().forEach(s -> System.out.println(s));
    }

    public void estado(String articulo) {
        Subasta s = subastas.get(articulo);
        if (s == null) {
            System.out.println("No existe la subasta '" + articulo + "'");
            return;
        }
        System.out.println(s);
        synchronized (s) {
            if (s.getHistorial().isEmpty()) {
                System.out.println("  (sin pujas)");
            } else {
                for (Puja p : s.getHistorial()) {
                    System.out.println("  " + p);
                }
            }
        }
    }

    public void quien() {
        System.out.println("Miembros: " + canal.getView().getMembers()
                + " | coordinador: " + canal.getView().getCoord()
                + " | yo: " + canal.getAddress());
    }

    public void ganadas() {
        boolean alguna = false;
        for (Subasta s : subastas.values()) {
            if (s.isCerrada() && miNombre.equals(s.getGanador())) {
                System.out.println(s.getArticulo() + " -> " + s.getMontoFinal());
                alguna = true;
            }
        }
        if (!alguna) {
            System.out.println("(no ganaste ninguna subasta todavia)");
        }
    }
}
