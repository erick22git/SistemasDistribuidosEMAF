package bo.edu.usfx.jgroups;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.Util;

/**
 * PARTE 1 - Pasos 4, 5 y 6: membresia, estado replicado y mensajes unicast.
 *
 * Uso (UDP, por defecto):
 *   mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.PizarraGrupo -Dexec.args="A"
 *
 * Uso (TCP, para el Paso 8 entre dos equipos):
 *   mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.PizarraGrupo -Dexec.args="A" ^
 *       -Dconfig=tcp.xml -Djgroups.bind_addr=192.168.1.25 ^
 *       -Djgroups.tcpping.initial_hosts=192.168.1.25[7800],192.168.1.30[7800]
 */
public class PizarraGrupo implements Receiver {

    private JChannel canal;
    private final String nombre;

    // ---- Paso 4: membresia ----
    private View vistaAnterior;

    // ---- Paso 5: ESTADO REPLICADO. Cada nodo tiene su copia; JGroups las
    // mantiene iguales via getState()/setState(). El acceso se protege con
    // synchronized porque un hilo de JGroups (receive) puede modificarla al
    // mismo tiempo que otro hilo la esta leyendo (getState o /historial).
    private final List<String> historial = new ArrayList<>();

    public PizarraGrupo(String nombre) {
        this.nombre = nombre;
    }

    // ================== Membresia (Paso 4) ==================

    @Override
    public void viewAccepted(View vista) {
        if (vistaAnterior != null) {
            Address[][] cambios = View.diff(vistaAnterior, vista);
            for (Address a : cambios[0]) System.out.println("** ENTRO: " + a);
            for (Address a : cambios[1]) System.out.println("** SALIO: " + a);
        }
        vistaAnterior = vista;
        System.out.println("** Vista " + vista.getViewId().getId()
                + " | coordinador: " + vista.getCoord()
                + " | miembros: " + vista.getMembers());
    }

    // ================== Mensajes y estado replicado (Paso 5 y 6) ==================

    @Override
    public void receive(Message msg) {
        String texto = msg.getSrc() + ": " + msg.getObject();
        synchronized (historial) {
            historial.add(texto);
        }
        String tipo = (msg.getDest() == null) ? "" : "(privado) ";
        System.out.println(tipo + texto);
    }

    @Override
    public void getState(OutputStream salida) throws Exception {
        // Lo ejecuta el nodo que YA tiene el estado (normalmente el coordinador)
        synchronized (historial) {
            Util.objectToStream(historial, new DataOutputStream(salida));
        }
        System.out.println("** Estado enviado a un nuevo miembro (" + historial.size() + " lineas)");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setState(InputStream entrada) throws Exception {
        // Lo ejecuta el nodo que ACABA de entrar
        List<String> recibido = Util.objectFromStream(new DataInputStream(entrada));
        synchronized (historial) {
            historial.clear();
            historial.addAll(recibido);
        }
        System.out.println("** Estado recibido: " + recibido.size() + " lineas");
        recibido.forEach(l -> System.out.println("   " + l));
    }

    // ================== Ciclo de vida ==================

    public void iniciar() throws Exception {
        // -Dconfig=tcp.xml cambia la pila de protocolos sin tocar codigo (Paso 8)
        canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(nombre);
        canal.setReceiver(this);
        canal.connect("PizarraSIS258");
        canal.getState(null, 10000); // pide el estado al coordinador (max 10 s)
        leerTeclado();
        canal.close();
    }

    private void leerTeclado() throws Exception {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Comandos: /quien /historial /privado <nombre> <texto> /salir");
        String linea;
        while ((linea = teclado.readLine()) != null) {
            if (linea.equals("/salir")) {
                break;
            } else if (linea.equals("/quien")) {
                System.out.println("Miembros: " + canal.getView().getMembers()
                        + " | yo: " + canal.getAddress());
            } else if (linea.equals("/historial")) {
                synchronized (historial) {
                    historial.forEach(System.out::println);
                }
            } else if (linea.startsWith("/privado ")) {
                enviarPrivado(linea);
            } else {
                canal.send(new ObjectMessage(null, linea)); // multicast al grupo
            }
        }
    }

    // ================== Unicast (Paso 6) ==================

    private void enviarPrivado(String linea) throws Exception {
        String[] partes = linea.split(" ", 3);
        if (partes.length < 3) {
            System.out.println("Uso: /privado <nombre> <texto>");
            return;
        }
        Address destino = buscarPorNombre(partes[1]);
        if (destino == null) {
            System.out.println("No existe el miembro " + partes[1]);
            return;
        }
        canal.send(new ObjectMessage(destino, partes[2])); // unicast: solo a ese miembro
    }

    private Address buscarPorNombre(String nombreBuscado) {
        for (Address a : canal.getView().getMembers()) {
            if (a.toString().equals(nombreBuscado)) return a; // nombre logico = toString()
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        String nombre = args.length > 0 ? args[0] : "anonimo";
        new PizarraGrupo(nombre).iniciar();
    }
}
