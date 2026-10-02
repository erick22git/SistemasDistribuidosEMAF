/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ejercicio1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author USUARIO
 */
public class Banco extends UnicastRemoteObject implements IBanco {

    public Banco() throws RemoteException {
        super();
    }

    @Override
    public Pago Debitar(String pasaporte, double montoUSD) throws RemoteException {
        String riesgo = consultarAntifraude(pasaporte, montoUSD);
        if (riesgo.equals("bajo")) {
            return new Pago(true, "AA3", "sin riesgo", montoUSD);
        }
        return new Pago(false, "", "tiene riesgo", montoUSD);
    }

    private String consultarAntifraude(String pasaporte, double montoUSD) {
        int puerto = 6789;
        try {
            DatagramSocket socketUDP = new DatagramSocket();
            String mensajeTexto = "antifraude:" + pasaporte + "-" + (int) montoUSD;
            byte[] mensaje = mensajeTexto.getBytes();
            InetAddress hostServidor = InetAddress.getByName("localhost");

            DatagramPacket peticion = new DatagramPacket(mensaje, mensaje.length, hostServidor, puerto);
            socketUDP.send(peticion);

            byte[] bufer = new byte[1000];
            DatagramPacket respuesta = new DatagramPacket(bufer, bufer.length);
            socketUDP.receive(respuesta);

            String cadena = new String(respuesta.getData(), 0, respuesta.getLength());
            socketUDP.close();
            return cadena;
        } catch (IOException e) {
            return "bajo";
        }
    }

}
