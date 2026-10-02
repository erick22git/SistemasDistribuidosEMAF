/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ejercicio1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author USUARIO
 */
public class ServerMigracion {

    public static void main(String[] args) throws IOException {
        int port = 5002;
        ServerSocket servidor = new ServerSocket(port);
        System.out.println("Servidor Migracion escuchando en el puerto " + port);
        while (true) {
            Socket cliente = servidor.accept();
            BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintStream salida = new PrintStream(cliente.getOutputStream());
            String recibido = entrada.readLine();
            String respuesta = procesarSolicitud(recibido);
            salida.println(respuesta);
            cliente.close();
        }
    }

    public static String procesarSolicitud(String cadena) {
        String[] comando = cadena.split(":");
        if ("123".equals(comando[1])) {
            return "valido:Bolivia";
        }
        return "invalido:";
    }

}
