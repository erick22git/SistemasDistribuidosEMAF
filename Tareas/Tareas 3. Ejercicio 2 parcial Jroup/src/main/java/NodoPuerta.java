import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.Util;

public class NodoPuerta implements Receiver {

    private JChannel canal;
    private final String nombre;
    
    private final int aforo;

    private final AtomicInteger ocupacion = new AtomicInteger(0);
    private final AtomicInteger reservadas = new AtomicInteger(0);

    private View vistaAnterior;

    public NodoPuerta(String nombre, int aforo) {
        this.nombre = nombre;
        this.aforo = aforo;
    }

    @Override
    public void viewAccepted(View vista) {
        if (vistaAnterior == null) {
            System.out.println("** Puertas en el grupo: " + vista.getMembers());
        } else {
            Address[][] cambios = View.diff(vistaAnterior, vista);
            for (Address a : cambios[0]) System.out.println("** ENTRO: " + a);
            for (Address a : cambios[1]) System.out.println("** SALIO: " + a);
        }
        System.out.println("** Coordinador actual: " + vista.getCoord());
        vistaAnterior = vista;
    }

    @Override
    public void receive(Message msg) {
        MensajeAforo m = msg.getObject();
        switch (m.getTipo()) {
            case SOLICITUD:
                if (soyCoordinador()) procesarSolicitud(msg.getSrc(), m);
                break;
            case ACEPTADO:
                int total = ocupacion.addAndGet(m.getPersonas());
                if (msg.getSrc().equals(canal.getAddress())) {
                    synchronized (reservadas) {
                        reservadas.updateAndGet(r -> Math.max(0, r - m.getPersonas()));
                    }
                }
                System.out.println("ACEPTADO: " + m.getPuerta() + " +" + m.getPersonas()
                        + " -> " + total + "/" + aforo);
                if (total >= aforo) System.out.println("*** AFORO COMPLETO ***");
                break;
            case RECHAZADO:
                System.out.println("RECHAZADO: " + m.getPuerta() + " no puede ingresar "
                        + m.getPersonas() + " (aforo " + aforo + ")");
                break;
            case SALIDA:
                int nuevo = ocupacion.updateAndGet(o -> Math.max(0, o - m.getPersonas()));
                System.out.println("SALIDA: " + m.getPuerta() + " -" + m.getPersonas()
                        + " -> " + nuevo + "/" + aforo);
                break;
        }
    }

    private boolean soyCoordinador() {
        Address yo = canal.getAddress();
        return yo != null && yo.equals(canal.getView().getCoord());
    }

    private void procesarSolicitud(Address remitente, MensajeAforo m) {
        try {
            int n = m.getPersonas();
            boolean cabe;
            synchronized (reservadas) {
                cabe = ocupacion.get() + reservadas.get() + n <= aforo;
                if (cabe) reservadas.addAndGet(n);
            }
            if (cabe) {
                canal.send(new ObjectMessage(null,
                        new MensajeAforo(MensajeAforo.Tipo.ACEPTADO, m.getPuerta(), n)));
            } else {
                MensajeAforo rechazo = new MensajeAforo(MensajeAforo.Tipo.RECHAZADO, m.getPuerta(), n);
                if (remitente.equals(canal.getAddress())) {
                    receive(new ObjectMessage(remitente, rechazo));
                } else {
                    canal.send(new ObjectMessage(remitente, rechazo));
                }
            }
        } catch (Exception e) {
            System.out.println("Error al procesar solicitud: " + e);
        }
    }

    @Override
    public void getState(OutputStream salida) throws Exception {
        Util.objectToStream(ocupacion.get(), new DataOutputStream(salida));
    }

    @Override
    public void setState(InputStream entrada) throws Exception {
        int recibido = Util.objectFromStream(new DataInputStream(entrada));
        ocupacion.set(recibido);
        System.out.println("** Estado recibido: " + recibido + "/" + aforo);
        if (recibido >= aforo) System.out.println("*** AFORO COMPLETO ***");
    }

    public void iniciar() throws Exception {
        canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(nombre);
        canal.setReceiver(this);
        canal.connect("AforoSIS258");
        canal.getState(null, 10000);
        leerTeclado();
        canal.close();
    }

    private void leerTeclado() throws Exception {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Puerta " + nombre + " (aforo " + aforo + "). Comandos: /entrar n  /salir n  /estado");
        String linea;
        while ((linea = teclado.readLine()) != null) {
            linea = linea.trim();
            if (linea.isEmpty()) continue;
            String[] p = linea.split("\\s+");
            if (p[0].equals("/estado") && p.length == 1) {
                estado();
            } else if ((p[0].equals("/entrar") || p[0].equals("/salir")) && p.length == 2) {
                int n = leerEntero(p[1]);
                if (n <= 0) {
                    System.out.println("n debe ser un entero mayor que 0");
                } else if (p[0].equals("/entrar")) {
                    entrar(n);
                } else {
                    salir(n);
                }
            } else {
                System.out.println("Comando no valido. Comandos: /entrar n  /salir n  /estado");
            }
        }
    }

    private int leerEntero(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void estado() {
        int o = ocupacion.get();
        System.out.println("Ocupacion: " + o + "/" + aforo);
        if (o >= aforo) System.out.println("*** AFORO COMPLETO ***");
    }

    private void entrar(int n) throws Exception {
        MensajeAforo solicitud = new MensajeAforo(MensajeAforo.Tipo.SOLICITUD, nombre, n);
        Address coord = canal.getView().getCoord();
        if (coord.equals(canal.getAddress())) {
            procesarSolicitud(canal.getAddress(), solicitud);
        } else {
            canal.send(new ObjectMessage(coord, solicitud));
        }
    }

    private void salir(int n) throws Exception {
        if (n > ocupacion.get()) {
            System.out.println("Error: no pueden salir " + n + " personas, ocupacion actual " + ocupacion.get());
            return;
        }
        canal.send(new ObjectMessage(null, new MensajeAforo(MensajeAforo.Tipo.SALIDA, nombre, n)));
    }

    public static void main(String[] args) throws Exception {
        int aforo = -1;
        if (args.length == 2) {
            try {
                aforo = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                aforo = -1;
            }
        }
        if (aforo <= 0) {
            System.out.println("Uso: java NodoPuerta <nombre> <aforoMaximo>  (aforoMaximo entero > 0)");
            return;
        }
        new NodoPuerta(args[0], aforo).iniciar();
    }
}
