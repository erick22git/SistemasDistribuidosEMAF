/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mavenproject1;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClienteBasico{

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "10.177.94.132";

        try {
            Socket socket = new Socket(host, 5000); 
            System.out.println("Conectado. Puerto local: " + socket.getLocalPort());

            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedReader teclado = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8));

            Thread hiloLectura = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String mensajeServidor;
                        while ((mensajeServidor = in.readLine()) != null) {
                            System.out.println(mensajeServidor);
                        }
                    } catch (IOException e) {
                        System.out.println("Conexión cerrada por el servidor.");
                    }
                }
            });
            hiloLectura.setDaemon(true); 
            hiloLectura.start();

            String texto;
            while ((texto = teclado.readLine()) != null) {
                out.println(texto);
                if (texto.trim().equalsIgnoreCase("/salir")) {
                    break; 
                }
            }

            socket.close();

        } catch (ConnectException e) {
            System.out.println("No se pudo conectar. ¿Está el servidor encendido en " + host + ":5000 ?");
        } catch (UnknownHostException e) {
            System.out.println("No se encontró el host: " + host);
        } catch (IOException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
    }
}