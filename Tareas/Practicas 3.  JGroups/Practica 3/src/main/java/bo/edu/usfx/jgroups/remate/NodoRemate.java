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

/**
 * PARTE 2 - Nodo/receptor de la subasta distribuida RemateUSFX.
 *
 * Responsabilidad unica de esta clase: manejar el canal JGroups, el
 * estado replicado (subastas) y la logica del protocolo. La interfaz
 * de consola (leer teclado, parsear comandos) vive aparte en
 * ConsolaRemate, que solo llama a los metodos publicos de aqui.
 *
 * ===================== DISENO (para el informe) =====================
 *
 * PATRON "proponer / aceptar" para resolver pujas simultaneas
 * (requisito 5): el multicast normal de JGroups garantiza orden FIFO
 * por EMISOR (NAKACK2), pero NO garantiza un orden total entre
 * mensajes de DOS emisores distintos. Si dos nodos pujan el mismo
 * monto "al mismo tiempo", cada uno podria ver los mensajes en un
 * orden diferente y aceptar pujas distintas como ganadoras.
 *
 * Elegimos la opcion (a) del enunciado: las propuestas (crear subasta,
 * pujar, extender) se mandan por UNICAST al coordinador. El
 * coordinador es un unico proceso, asi que procesa las propuestas que
 * le llegan una por una (secuencialmente) y decide cual es valida. La
 * decision se difunde por MULTICAST a todo el grupo. Ningun nodo -ni
 * siquiera el propio coordinador- aplica un cambio a su copia local
 * del estado hasta que llega el mensaje de "aceptado" (NUEVA_SUBASTA,
 * PUJA_ACEPTADA, CIERRE, EXTENSION). Como todos reciben exactamente
 * los mismos mensajes de aceptacion, en el mismo orden (el orden en
 * que el coordinador los emitio), todos los nodos convergen al mismo
 * estado.
 *
 * Ventaja sobre SEQUENCER: no requiere tocar la pila de protocolos
 * (mas simple de configurar y de explicar). Desventaja: el
 * coordinador es un cuello de botella para escribir (no para leer) y
 * si cambia de coordinador a mitad de una decision, esa propuesta se
 * pierde y el proponente debe reintentar (aceptable para esta
 * practica).
 *
 * TRASPASO DE COORDINADOR (requisito 6): cada nodo determina si es
 * coordinador comparando canal.getAddress() con vista.getCoord()
 * dentro de viewAccepted(). Cuando un nodo detecta que ACABA de
 * convertirse en coordinador, recorre todas las subastas abiertas de
 * su copia del estado replicado y reprograma sus temporizadores
 * (ScheduledExecutorService) segun el instante de cierre absoluto ya
 * guardado en cada Subasta. Si una subasta ya debio cerrarse mientras
 * no habia coordinador (por ejemplo, el anterior cayo justo antes),
 * el nuevo coordinador la cierra inmediatamente al detectarlo.
 *
 * ESTADO COMPARTIDO Y CONCURRENCIA (requisito 8):
 *  - Map<String, Subasta> subastas: ConcurrentHashMap. Lo tocan el
 *    hilo de JGroups (receive/getState/setState), el hilo del teclado
 *    (ConsolaRemate) y los hilos del scheduler (cierres automaticos).
 *    ConcurrentHashMap permite lecturas/escrituras concurrentes sin
 *    bloquear todo el mapa.
 *  - List<Puja> historial dentro de cada Subasta: se modifica siempre
 *    desde dentro de un bloque sincronizado sobre la propia Subasta,
 *    porque una Subasta puede recibir una nueva puja (hilo JGroups)
 *    justo cuando se esta serializando para getState() (tambien hilo
 *    de JGroups, pero de otra invocacion) o leyendo para /estado
 *    (hilo del teclado).
 *  - Map<String, ScheduledFuture<?>> temporizadores: ConcurrentHashMap,
 *    por la misma razon que subastas.
 *  - AtomicBoolean eraCoordinador: se lee y escribe desde el hilo de
 *    JGroups en viewAccepted(); AtomicBoolean evita condiciones de
 *    carrera si dos vistas llegaran muy seguidas.
 */
public class NodoRemate implements Receiver {

    private JChannel canal;
    private final String miNombre;

    // ESTADO REPLICADO principal (requisito 4)
    private final Map<String, Subasta> subastas = new ConcurrentHashMap<>();

    // Temporizadores de cierre, solo tienen entradas en el nodo que ES coordinador
    private final Map<String, ScheduledFuture<?>> temporizadores = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final AtomicBoolean eraCoordinador = new AtomicBoolean(false);

    public NodoRemate(String nombre) {
        this.miNombre = nombre;
    }

    // ================== Ciclo de vida ==================

    public void iniciar() throws Exception {
        canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(miNombre);
        canal.setReceiver(this);
        canal.connect("RemateSIS258");
        canal.getState(null, 10000); // recibe el estado completo si ya hay subastas
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

    // ================== Membresia (requisito 6) ==================

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
            // Muy raro en JGroups (el coordinador es el mas antiguo), pero por
            // seguridad cancelamos nuestros temporizadores si dejamos de serlo.
            cancelarTodosMisTemporizadores();
        }
    }

    private void retomarTemporizadoresComoNuevoCoordinador() {
        long ahora = System.currentTimeMillis();
        for (Subasta s : subastas.values()) {
            if (s.isCerrada()) continue;

            long restanteMs = s.getInstanteCierreMillis() - ahora;
            if (restanteMs <= 0) {
                // Expiro mientras no habia coordinador: cerrar ya mismo.
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

    // ================== Estado replicado: getState / setState ==================

    @Override
    public void getState(OutputStream salida) throws Exception {
        // Snapshot defensivo: copiamos a un LinkedHashMap fuera de la seccion
        // sincronizada minima posible, para no bloquear mucho tiempo.
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

    // ================== Recepcion de mensajes del protocolo ==================

    @Override
    @SuppressWarnings("unchecked")
    public void receive(Message msg) {
        Object payload = msg.getObject();
        if (!(payload instanceof MensajeRemate)) {
            return; // mensaje de otro tipo/practica, se ignora
        }
        MensajeRemate m = (MensajeRemate) payload;

        switch (m.tipo) {
            case PROPUESTA_SUBASTA:
            case PROPUESTA_PUJA:
            case PROPUESTA_EXTENSION:
                // Solo el coordinador valida y decide (ver procesarPropuesta)
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

    /** Punto de entrada unico para procesar cualquier PROPUESTA_*, venga de la red o de mi mismo. */
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

    // ---------- Lado COORDINADOR: valida propuestas y decide ----------

    private void procesarPropuestaSubasta(MensajeRemate m, Address remitente) {
        if (subastas.containsKey(m.articulo)) {
            enviarRechazo(remitente, m.articulo, "ya existe una subasta con ese articulo");
            return;
        }
        if (m.precioBase <= 0 || m.segundosDuracion <= 0) {
            enviarRechazo(remitente, m.articulo, "precio o duracion invalidos");
            return;
        }
        // El COORDINADOR calcula el instante de cierre absoluto: todos los
        // nodos van a usar este mismo valor, sin importar su reloj local.
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

    /** Lo llama el propio coordinador cuando expira un temporizador (o al retomar uno vencido). */
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

    // ---------- Lado TODOS LOS NODOS: aplican lo que decidio el coordinador ----------
    // (el propio coordinador tambien pasa por aqui, al recibir su propio multicast)

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
        if (s == null) return; // no deberia pasar si NUEVA_SUBASTA siempre llega antes

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

    // ================== Temporizadores (solo los usa el coordinador) ==================

    private void programarCierre(String articulo, long retrasoMs) {
        ScheduledFuture<?> anterior = temporizadores.remove(articulo);
        if (anterior != null) anterior.cancel(false);

        ScheduledFuture<?> futuro = scheduler.schedule(() -> {
            // Revalidamos: puede que ya no sea coordinador cuando dispare el timer
            if (soyCoordinador()) {
                cerrarSubastaComoCoordinador(articulo);
            }
        }, retrasoMs, TimeUnit.MILLISECONDS);

        temporizadores.put(articulo, futuro);
    }

    // ================== Envio de mensajes ==================

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
                // Yo mismo soy el coordinador: proceso directo, sin pasar por la red.
                // OJO: aqui se pasa canal.getAddress() como "remitente" directamente,
                // NO se construye un ObjectMessage falso -- un ObjectMessage recien
                // construido con new no trae puesto el src (eso lo asigna el
                // transporte real al enviarlo), asi que usarlo aqui daria un
                // remitente null/incorrecto para una eventual respuesta de rechazo.
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
            // Me rechazo a mi mismo (soy coordinador y proponente a la vez):
            // no hace falta mandarlo por la red, solo mostrarlo.
            System.out.println("** RECHAZADO (" + articulo + "): " + motivo);
            return;
        }
        try {
            canal.send(new ObjectMessage(destino, MensajeRemate.rechazo(articulo, motivo)));
        } catch (Exception e) {
            System.out.println("Error enviando rechazo: " + e.getMessage());
        }
    }

    // ================== API publica para ConsolaRemate ==================

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
