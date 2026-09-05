package bo.edu.usfx.jgroups;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;

/**
 * PARTE 1 - Paso 2: Primer nodo, unirse a un grupo y enviar al grupo.
 *
 * Fijate que no hay ServerSocket, no hay accept() y no aparece ninguna
 * direccion IP: el nodo solo conoce el nombre del grupo.
 *
 * Uso:
 *   mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.NodoBasico -Dexec.args="A"
 */
public class NodoBasico {

    public static void main(String[] args) throws Exception {
        String nombre = args.length > 0 ? args[0] : "nodo-" + (int) (Math.random() * 100);

        JChannel canal = new JChannel(); // usa udp.xml por defecto
        canal.name(nombre); // nombre logico del miembro

        canal.setReceiver(new Receiver() {
            @Override
            public void viewAccepted(View vista) {
                // Este metodo lo llama un HILO DE JGROUPS, no el hilo principal.
                System.out.println("** Nueva vista: " + vista);
            }

            @Override
            public void receive(Message msg) {
                // Este metodo tambien lo llama un hilo de JGroups.
                System.out.println("[" + msg.getSrc() + "] " + msg.getObject());
            }
        });

        canal.connect("ClusterSIS258"); // se une al grupo (o lo crea)
        System.out.println("Conectado como " + canal.getAddress()
                + " | coordinador: " + canal.getView().getCoord());

        for (int i = 1; i <= 5; i++) {
            // destino null = TODOS los miembros del grupo (multicast)
            canal.send(new ObjectMessage(null, "Hola #" + i + " desde " + nombre));
            Thread.sleep(3000);
        }

        canal.close(); // sale del grupo ordenadamente
    }
}
