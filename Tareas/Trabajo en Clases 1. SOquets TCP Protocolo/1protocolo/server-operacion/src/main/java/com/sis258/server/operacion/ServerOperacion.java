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
        int port = 5003;
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Servidor de operaciones (interactivo) en el puerto " + port);
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
            String peticion = fromClient.readLine();
            System.out.println("Peticion: " + peticion);

            String header;
            while ((header = fromClient.readLine()) != null && !header.isEmpty()) {
                // se descartan las cabeceras
            }

            String cuerpo = construirPagina(peticion);

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

    private static String construirPagina(String peticion) {
        String resultadoHtml = "";

        if (peticion != null && peticion.contains("?")) {
            try {
                String ruta = peticion.split(" ")[1];
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
                    case "suma": resultado = num1 + num2;
                        resultadoHtml = "<p><b>Resultado: " + resultado + "</b></p>";
                        break;
                    case "resta": resultado = num1 - num2;
                        resultadoHtml = "<p><b>Resultado: " + resultado + "</b></p>";
                        break;
                    case "multiplicacion": resultado = num1 * num2;
                        resultadoHtml = "<p><b>Resultado: " + resultado + "</b></p>";
                        break;
                    case "division":
                        if (num2 == 0) {
                            resultadoHtml = "<p style='color:red'>Error: division entre cero</p>";
                        } else {
                            resultado = num1 / num2;
                            resultadoHtml = "<p><b>Resultado: " + resultado + "</b></p>";
                        }
                        break;
                    default:
                        resultadoHtml = "<p style='color:red'>Operacion no valida</p>";
                }
            } catch (Exception e) {
                resultadoHtml = "<p style='color:red'>Datos invalidos</p>";
            }
        }
        return paginaCompleta(resultadoHtml);
    }

    private static String paginaCompleta(String resultadoHtml) {
        return "<html><head><title>Calculadora Interactiva</title></head>"
             + "<body>"
             + "<h1>Calculadora Interactiva</h1>"
             + "<form method='GET' action='/'>"
             + "Numero 1: <input type='number' name='num1' step='any' required><br>"
             + "Numero 2: <input type='number' name='num2' step='any' required><br>"
             + "Operacion: "
             + "<select name='operacion'>"
             + "<option value='suma'>Suma</option>"
             + "<option value='resta'>Resta</option>"
             + "<option value='multiplicacion'>Multiplicacion</option>"
             + "<option value='division'>Division</option>"
             + "</select><br><br>"
             + "<input type='submit' value='Calcular'>"
             + "</form>"
             + resultadoHtml
             + "</body></html>";
    }
}