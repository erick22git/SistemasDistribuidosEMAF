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

            System.out.println("Conectado al servidor de operaciones.");
            System.out.println("Formato: operacion numero1 numero2");
            System.out.println("Operaciones: suma, resta, multiplicacion, division");
            System.out.print("Ingrese la operacion: ");

            String peticion = teclado.readLine();
            toServer.println(peticion);

            String respuesta = fromServer.readLine();
            System.out.println("Servidor: " + respuesta);

            socket.close();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}