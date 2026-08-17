package com.mycompany.mavenproject1;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author erick
 */

import java.io.*;
import java.net.*;

public class ClienteChat {
    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "10.177.94.132";
        Socket socket = new Socket(host, 5000);
        System.out.println("Conectado. Puerto local: " + socket.getLocalPort());

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

        Thread receptor = new Thread(() -> {
            try {
                String s;
                while ((s = in.readLine()) != null) {
                    System.out.println(s);
                }
            } catch (IOException e) {
                System.out.println("Conexion terminada");
            }
        }, "hilo-receptor");
        receptor.setDaemon(true);
        receptor.start();

        String texto;
        while ((texto = teclado.readLine()) != null) {
            out.println(texto);
        }
        socket.close();
    }
}
