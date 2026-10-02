/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ejercicio1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 *
 * @author USUARIO
 */
public class ServidorAntifraude {

    public static void main(String[] args) {
        int port = 6789;
        try {
            DatagramSocket socketUDP = new DatagramSocket(port);
            System.out.println("Servidor Antifraude escuchando en el puerto " + port);
            byte[] bufer = new byte[1000];
            while (true) {
                DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
                socketUDP.receive(peticion);
                String cadena = new String(peticion.getData(), 0, peticion.getLength());
                String response = procesar(cadena);
                byte[] mensaje = response.getBytes();
                DatagramPacket respuesta = new DatagramPacket(mensaje, mensaje.length, peticion.getAddress(), peticion.getPort());
                socketUDP.send(respuesta);
            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }

    public static String procesar(String cadena) {
        String[] comando = cadena.split(":");
        String consulta = comando[1];
        String[] comando2 = consulta.split("-");
        int monto = Integer.parseInt(comando2[1]);
        if (monto > 1000) {
            return "alto";
        }
        return "bajo";
    }

}
