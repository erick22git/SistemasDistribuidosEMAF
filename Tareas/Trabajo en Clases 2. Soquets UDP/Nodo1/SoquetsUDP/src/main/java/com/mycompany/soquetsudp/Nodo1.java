/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.soquetsudp;

import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 * NODO 1 - Generacion
 * Pide texto al usuario, cuenta caracteres, lo manda al Nodo 2,
 * y espera la respuesta final que viene del Nodo 3.
 *
 * @author erick
 */
public class Nodo1 {

    static final int PUERTO_NODO1 = 5001;
    static final int PUERTO_NODO2 = 5002;

    public static void main(String[] args) {
        // IP del equipo donde corre Nodo 2 (cambiar segun donde este tu compañero)
        String ipNodo2 = args.length > 0 ? args[0] : "10.173.29.208";

        Scanner sc = new Scanner(System.in);

        try {
            // El socket se crea con puerto FIJO (5001), para que Nodo3 sepa
            // exactamente a donde responder al final.
            DatagramSocket socketUDP = new DatagramSocket(PUERTO_NODO1);

            System.out.print("Introduzca una palabra o frase: ");
            String texto = sc.nextLine();

            int cantidadCaracteres = texto.length();
            System.out.println("Cantidad de caracteres: " + cantidadCaracteres);

            // Obtenemos nuestra propia IP para incluirla en el mensaje,
            // asi Nodo3 sabe donde enviarnos la respuesta final.
            String miIp = InetAddress.getLocalHost().getHostAddress();

            // Armamos el mensaje segun el protocolo acordado:
            // texto|cantidadCaracteres|ipNodo1|puertoNodo1
            String mensaje = texto + "|" + cantidadCaracteres + "|" + miIp + "|" + PUERTO_NODO1;

            byte[] datos = mensaje.getBytes();
            InetAddress direccionNodo2 = InetAddress.getByName(ipNodo2);

            DatagramPacket peticion = new DatagramPacket(
                    datos, datos.length, direccionNodo2, PUERTO_NODO2);

            socketUDP.send(peticion);
            System.out.println("Mensaje enviado al Nodo 2 (" + ipNodo2 + ":" + PUERTO_NODO2 + ")");
            System.out.println("Esperando respuesta final del Nodo 3...");

            // Nos quedamos escuchando en nuestro propio puerto (5001)
            // hasta que Nodo3 nos responda con el resumen final.
            byte[] bufer = new byte[2000];
            DatagramPacket respuestaFinal = new DatagramPacket(bufer, bufer.length);
            socketUDP.receive(respuestaFinal);

            String resultado = new String(
                    respuestaFinal.getData(), 0, respuestaFinal.getLength());

            System.out.println("\n===== RESULTADO FINAL =====");
            System.out.println(resultado);

            socketUDP.close();

        } catch (SocketException e) {
            System.out.println("Error de socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error de IO: " + e.getMessage());
        }
    }
}