package bo.edu.usfx.sockets.nodo3;

import java.net.*;

public class Nodo3 {
    public static void main(String[] args) {
        String ipNodo1 = "10.173.29.168"; // IP de la PC del Nodo 1
        int puertoPropio = 5003;
        int puertoSiguiente = 5001;

        try (DatagramSocket socket = new DatagramSocket(puertoPropio)) {
            System.out.println("NODO 3: Escuchando en puerto " + puertoPropio + "...");

            byte[] buffer = new byte[1024];
            DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
            socket.receive(paquete);

            String data = new String(paquete.getData(), 0, paquete.getLength());
            String[] partes = data.split("\\|"); // texto|carac|palabras|paridad

            // PROCESAMIENTO
            String textoMayus = partes[0].toUpperCase();
            int vocales = 0;
            for (char c : partes[0].toLowerCase().toCharArray()) {
                if ("aeiouáéíóú".indexOf(c) != -1) vocales++;
            }

            // RESUMEN
            String resumen = "MAYÚSCULAS: " + textoMayus + "\n" +
                             "CARACTERES: " + partes[1] + " (" + partes[3] + ")\n" +
                             "PALABRAS: " + partes[2] + "\n" +
                             "VOCALES: " + vocales;

            byte[] bufferEnvio = resumen.getBytes();
            InetAddress destino = InetAddress.getByName(ipNodo1);
            DatagramPacket paqueteEnvio = new DatagramPacket(bufferEnvio, bufferEnvio.length, destino, puertoSiguiente);
            socket.send(paqueteEnvio);
            System.out.println("NODO 3: Resumen enviado al Nodo 1.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}