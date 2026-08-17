/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.sis258.server.operacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerOperacion {

    public static void main(String[] args) {
        int port = 5002;
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Servidor de operaciones (protocolo) en el puerto " + port);
            while (true) {
                Socket client = server.accept();
                System.out.println("Cliente conectado: " + client.getInetAddress());
                Thread hilo = new Thread(() -> atenderCliente(client));
                hilo.start();
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    private static void atenderCliente(Socket client) {
        try (
            BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintStream toClient = new PrintStream(client.getOutputStream())
        ) {
            String peticion = fromClient.readLine(); // ej: GET /?operacion=suma&num1=5&num2=3 HTTP/1.1
            System.out.println("Peticion: " + peticion);

            String header;
            while ((header = fromClient.readLine()) != null && !header.isEmpty()) {
                // se descartan las cabeceras
            }

            String cuerpo = procesarSolicitud(peticion);

            toClient.println("HTTP/1.1 200 OK");
            toClient.println("Content-Type: text/html; charset=UTF-8");
            toClient.println("Content-Length: " + cuerpo.getBytes().length);
            toClient.println("Connection: close");
            toClient.println();
            toClient.println(cuerpo);
            toClient.flush();

        } catch (IOException e) {
            System.out.println("Error con cliente: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException e) {}
        }
    }

    // Protocolo: /?operacion=suma&num1=5&num2=3
    public static String procesarSolicitud(String peticion) {
        try {
            if (peticion == null) return "<h1>Peticion vacia</h1>";

            String ruta = peticion.split(" ")[1]; // ej: "/?operacion=suma&num1=5&num2=3"

            if (!ruta.contains("?")) {
                return "<h1>Servidor de Operaciones</h1>"
                     + "<p>Use el formato: /?operacion=suma&num1=5&num2=3</p>"
                     + "<p>Operaciones: suma, resta, multiplicacion, division</p>";
            }

            String query = ruta.substring(ruta.indexOf("?") + 1);
            Map<String, String> params = new HashMap<>();
            for (String par : query.split("&")) {
                String[] kv = par.split("=");
                if (kv.length == 2) params.put(kv[0], kv[1]);
            }

            String operacion = params.get("operacion");
            double num1 = Double.parseDouble(params.get("num1"));
            double num2 = Double.parseDouble(params.get("num2"));
            double resultado;

            switch (operacion) {
                case "suma": resultado = num1 + num2; break;
                case "resta": resultado = num1 - num2; break;
                case "multiplicacion": resultado = num1 * num2; break;
                case "division":
                    if (num2 == 0) return "<h1>Error: division entre cero</h1>";
                    resultado = num1 / num2; break;
                default: return "<h1>Error: operacion no valida</h1>";
            }

            return "<h1>Resultado: " + resultado + "</h1>"
                 + "<p>" + operacion + "(" + num1 + ", " + num2 + ")</p>";

        } catch (Exception e) {
            return "<h1>Error: formato invalido</h1>"
                 + "<p>Use: /?operacion=suma&num1=5&num2=3</p>";
        }
    }
}