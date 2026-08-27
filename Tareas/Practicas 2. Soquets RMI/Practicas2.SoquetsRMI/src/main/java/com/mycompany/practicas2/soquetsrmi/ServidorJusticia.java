package com.mycompany.practicas2.soquetsrmi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class ServidorJusticia extends UnicastRemoteObject implements IServidorJusticia {

    private static String IP_MERCANTIL = "10.170.34.48";
    private static final int PUERTO_MERCANTIL_TCP = 6001;

    private static String IP_BCP = "10.170.34.48";
    private static final int PUERTO_BCP_UDP = 6002;

    protected ServidorJusticia() throws RemoteException {
        super();
    }

    @Override
    public RespuestaCuenta ConsultarCuentas(String ci, String nombres, String apellidos) throws RemoteException {
        ArrayList<Cuenta> cuentas = new ArrayList<>();

        String respuestaMercantil = consultarMercantilTCP(ci);
        cuentas.addAll(parsearCuentas(respuestaMercantil, Banco.MERCANTIL, ci, nombres, apellidos));

        String respuestaBCP = consultarBcpUDP(ci);
        cuentas.addAll(parsearCuentas(respuestaBCP, Banco.BCP, ci, nombres, apellidos));

        if (cuentas.isEmpty()) {
            return new RespuestaCuenta(true, "No se encontraron cuentas para el CI " + ci, cuentas);
        }
        return new RespuestaCuenta(false, "Consulta exitosa", cuentas);
    }

    @Override
    public RespuestaCuenta Congelar(String nrocuenta, Banco banco, Double monto) throws RemoteException {
        String resultado;
        if (banco == Banco.MERCANTIL) {
            resultado = congelarMercantilTCP(nrocuenta, monto);
        } else {
            resultado = congelarBcpUDP(nrocuenta, monto);
        }

        if (resultado.startsWith("OK")) {
            return new RespuestaCuenta(false, "Cuenta " + nrocuenta + " congelada por " + monto, new ArrayList<>());
        } else {
            return new RespuestaCuenta(true, "No se pudo congelar: " + resultado, new ArrayList<>());
        }
    }

    // ---- Mercantil (TCP) ----
    private String consultarMercantilTCP(String ci) {
        try (Socket socket = new Socket(IP_MERCANTIL, PUERTO_MERCANTIL_TCP)) {
            PrintStream salida = new PrintStream(socket.getOutputStream());
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            salida.println(ci);
            salida.println("buscar");

            String respuesta = entrada.readLine();
            return respuesta == null ? "" : respuesta;

        } catch (IOException e) {
            System.out.println("Error consultando Mercantil (TCP): " + e.getMessage());
            return "";
        }
    }

    private String congelarMercantilTCP(String nrocuenta, Double monto) {
        try (Socket socket = new Socket(IP_MERCANTIL, PUERTO_MERCANTIL_TCP)) {
            PrintStream salida = new PrintStream(socket.getOutputStream());
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            salida.println("-");
            salida.println("congelar");
            salida.println(nrocuenta);
            salida.println(monto);

            String respuesta = entrada.readLine();
            return respuesta == null ? "ERROR:sin respuesta" : respuesta;

        } catch (IOException e) {
            return "ERROR:" + e.getMessage();
        }
    }

    // ---- BCP (UDP) ----
    private String consultarBcpUDP(String ci) {
        return enviarUDP("buscar:" + ci);
    }

    private String congelarBcpUDP(String nrocuenta, Double monto) {
        String r = enviarUDP("congelar:" + nrocuenta + "-" + monto);
        return r.isEmpty() ? "ERROR:sin respuesta" : r;
    }

    private String enviarUDP(String mensajeSalida) {
        try {
            DatagramSocket socketUDP = new DatagramSocket();
            byte[] datos = mensajeSalida.getBytes();
            InetAddress direccion = InetAddress.getByName(IP_BCP);

            DatagramPacket peticion = new DatagramPacket(datos, datos.length, direccion, PUERTO_BCP_UDP);
            socketUDP.send(peticion);

            byte[] buffer = new byte[2000];
            DatagramPacket respuesta = new DatagramPacket(buffer, buffer.length);
            socketUDP.setSoTimeout(5000);
            socketUDP.receive(respuesta);

            socketUDP.close();
            return new String(respuesta.getData(), 0, respuesta.getLength());

        } catch (IOException e) {
            System.out.println("Error consultando BCP (UDP): " + e.getMessage());
            return "";
        }
    }

    // ---- Parseo comun ----
    private ArrayList<Cuenta> parsearCuentas(String cadena, Banco banco, String ci, String nombres, String apellidos) {
        ArrayList<Cuenta> resultado = new ArrayList<>();
        if (cadena == null || cadena.trim().isEmpty()) {
            return resultado;
        }

        String[] items = cadena.split(":");
        for (String item : items) {
            String[] partes = item.split("-");
            if (partes.length == 2) {
                String nrocuenta = partes[0];
                Double saldo = Double.parseDouble(partes[1]);
                resultado.add(new Cuenta(banco, nrocuenta, ci, nombres, apellidos, saldo));
            }
        }
        return resultado;
    }

    // ---- Main ----
    public static void main(String[] args) {
        // IMPORTANTE: fuerza a RMI a anunciar esta IP especifica a los
        // clientes remotos. Sin esto, Java puede elegir automaticamente
        // otra interfaz de red de la maquina (ej: VirtualBox Host-Only,
        // 192.168.56.x) y los clientes en otras redes no podran conectarse.
        System.setProperty("java.rmi.server.hostname", "10.170.34.168");

        if (args.length > 0) {
            IP_MERCANTIL = args[0];
        }
        if (args.length > 1) {
            IP_BCP = args[1];
        }

        try {
            ServidorJusticia servidor = new ServidorJusticia();
            Registry registro = LocateRegistry.createRegistry(1099);
            registro.rebind("Justicia", servidor);

            System.out.println("Servidor Justicia listo (RMI puerto 1099).");
            System.out.println("Anunciando hostname RMI: " + System.getProperty("java.rmi.server.hostname"));
            System.out.println("Banco Mercantil (TCP) en: " + IP_MERCANTIL + ":" + PUERTO_MERCANTIL_TCP);
            System.out.println("Banco BCP (UDP) en: " + IP_BCP + ":" + PUERTO_BCP_UDP);

        } catch (Exception e) {
            System.out.println("Error al iniciar Servidor Justicia: " + e.getMessage());
        }
    }
}