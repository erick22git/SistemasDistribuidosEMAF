/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Trabajoenclaces2;

/**
 *
 * @author 72828
 */
import java.io.IOException;
import java.net.*;

public class Nodo2 {
    public static void main(String[] args) {
        // --- CONFIGURACIÓN: CAMBIA LA IP POR LA DE LA PC 3 ---
        String ipSiguiente = "10.173.29.198"; 
        int puertoPropio = 5002;
        int puertoSiguiente = 5003;

        try {
            try (DatagramSocket socket = new DatagramSocket(puertoPropio)) {
                System.out.println("NODO 2: Escuchando en puerto " + puertoPropio + "...");
                
                // 1. RECIBIR DEL NODO 1
                byte[] buffer = new byte[1024];
                DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length);
                socket.receive(paqueteRecibido);
                
                String data = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());
                String[] partes = data.split("\\|"); // [0]=texto, [1]=caracteres
                
                String texto = partes[0];
                int caracteres = Integer.parseInt(partes[1]);
                
                // 2. PROCESAMIENTO NODO 2
                int numPalabras = texto.trim().isEmpty() ? 0 : texto.trim().split("\\s+").length;
                String paridad = (caracteres % 2 == 0) ? "Par" : "Impar";
                
                // 3. ENVIAR AL NODO 3 (Formato: texto|caracteres|palabras|paridad)
                String mensajeSiguiente = texto + "|" + caracteres + "|" + numPalabras + "|" + paridad;
                byte[] bufferEnvio = mensajeSiguiente.getBytes();
                InetAddress ipDestino = InetAddress.getByName(ipSiguiente);
                
                DatagramPacket paqueteEnvio = new DatagramPacket(bufferEnvio, bufferEnvio.length, ipDestino, puertoSiguiente);
                socket.send(paqueteEnvio);
                
                System.out.println("NODO 2: Procesado y enviado al Nodo 3.");
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}