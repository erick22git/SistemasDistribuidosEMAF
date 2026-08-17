/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.sis258.client.operacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class ClientOperacion {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = 5002;
        try {
            Socket socket = new Socket(host, port);

            PrintStream toServer = new PrintStream(socket.getOutputStream());
            BufferedReader fromServer = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            BufferedReader teclado = new BufferedReader(
                    new InputStreamReader(System.in));

            System.out.println("Conectado al servidor interactivo.");

            // El servidor hace 3 preguntas (primer numero, segundo numero, operacion).
            // Por cada pregunta: la mostramos y enviamos la respuesta del usuario.
            for (int i = 0; i < 3; i++) {
                String pregunta = fromServer.readLine();
                System.out.println("Servidor: " + pregunta);
                System.out.print("> ");
                String respuesta = teclado.readLine();
                toServer.println(respuesta);
            }

            // Ultima linea: el resultado
            String resultado = fromServer.readLine();
            System.out.println("Servidor: " + resultado);

            socket.close();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}