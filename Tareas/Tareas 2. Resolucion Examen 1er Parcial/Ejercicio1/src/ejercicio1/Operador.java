/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ejercicio1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

/**
 *
 * @author USUARIO
 */
public class Operador extends UnicastRemoteObject implements IOperador {

    public Operador() throws RemoteException {
        super();
    }

    @Override
    public Voucher ComprarTour(String pasaporte, String codigoTour, int personas) throws RemoteException {
        String migracion = consultarMigracion(pasaporte);
        String[] respuesta = migracion.split(":");

        if (!respuesta[0].equalsIgnoreCase("valido")) {
            return new Voucher(false, "", "Pasaporte invalido", 0, new ArrayList<>());
        }

        double precio;
        if (respuesta[1].equalsIgnoreCase("Bolivia")) {
            precio = personas * 180 * 0.5;
        } else {
            precio = personas * 180;
        }

        try {
            Registry registro = LocateRegistry.getRegistry(1099);
            IBanco banco = (IBanco) registro.lookup("Banco");
            Pago pago = banco.Debitar(pasaporte, precio);
            ArrayList<Pago> pagos = new ArrayList<>();
            pagos.add(pago);
            if (pago.isAprobado()) {
                return new Voucher(true, "C-001", "", precio, pagos);
            }
            return new Voucher(false, "", "No se aprobo el credito", precio, pagos);
        } catch (NotBoundException e) {
            return new Voucher(false, "", "Banco no disponible", 0, new ArrayList<>());
        }
    }

    private String consultarMigracion(String pasaporte) {
        int port = 5002;
        try (Socket cliente = new Socket("localhost", port)) {
            PrintStream salida = new PrintStream(cliente.getOutputStream());
            BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            salida.println("pasaporte:" + pasaporte);
            return entrada.readLine();
        } catch (IOException e) {
            return "invalido:";
        }
    }

}
