package bo.edu.usfx.jgroups.remate;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConsolaRemate {

    public static void main(String[] args) throws Exception {
        String nombre = args.length > 0 ? args[0] : "anonimo";

        NodoRemate nodo = new NodoRemate(nombre);
        nodo.iniciar();

        System.out.println("Conectado como " + nombre + " (" + nodo.getMiDireccion() + ")");
        imprimirAyuda();

        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String linea;
        while ((linea = teclado.readLine()) != null) {
            linea = linea.trim();
            if (linea.isEmpty()) continue;

            try {
                if (linea.equals("/salir")) {
                    break;

                } else if (linea.equals("/subastas")) {
                    nodo.listarSubastas();

                } else if (linea.equals("/quien")) {
                    nodo.quien();

                } else if (linea.equals("/ganadas")) {
                    nodo.ganadas();

                } else if (linea.equals("/ayuda")) {
                    imprimirAyuda();

                } else if (linea.startsWith("/crear ")) {
                    String[] p = linea.split(" ");
                    if (p.length != 4) {
                        System.out.println("Uso: /crear <articulo> <precio_base> <segundos>");
                    } else {
                        nodo.crearSubasta(p[1], Double.parseDouble(p[2]), Integer.parseInt(p[3]));
                    }

                } else if (linea.startsWith("/pujar ")) {
                    String[] p = linea.split(" ");
                    if (p.length != 3) {
                        System.out.println("Uso: /pujar <articulo> <monto>");
                    } else {
                        nodo.pujar(p[1], Double.parseDouble(p[2]));
                    }

                } else if (linea.startsWith("/estado ")) {
                    String[] p = linea.split(" ", 2);
                    nodo.estado(p[1]);

                } else if (linea.startsWith("/extender ")) {
                    String[] p = linea.split(" ");
                    if (p.length != 3) {
                        System.out.println("Uso: /extender <articulo> <segundos>");
                    } else {
                        nodo.extender(p[1], Integer.parseInt(p[2]));
                    }

                } else {
                    System.out.println("Comando desconocido. Escriba /ayuda para ver la lista.");
                }

            } catch (NumberFormatException e) {
                System.out.println("Numero invalido: " + e.getMessage());
            }
        }

        nodo.cerrar();
        System.out.println("Desconectado.");
    }

    private static void imprimirAyuda() {
        System.out.println("Comandos disponibles:");
        System.out.println("  /crear <articulo> <precio_base> <segundos>");
        System.out.println("  /subastas");
        System.out.println("  /pujar <articulo> <monto>");
        System.out.println("  /estado <articulo>");
        System.out.println("  /quien");
        System.out.println("  /ganadas");
        System.out.println("  /extender <articulo> <segundos>   (bonus)");
        System.out.println("  /salir");
    }
}
