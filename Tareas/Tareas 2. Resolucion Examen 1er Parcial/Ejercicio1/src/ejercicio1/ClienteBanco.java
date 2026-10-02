/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ejercicio1;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 *
 * @author USUARIO
 */
class ClienteBanco {

    public static void main(String[] args) throws Exception {
        Registry registro = LocateRegistry.getRegistry("localhost", 1098);
        IOperador operador = (IOperador) registro.lookup("Operador");
        Voucher voucher = operador.ComprarTour("123", "T1", 3);
        System.out.println(voucher.isConfirmado() + " " + voucher.getMontoUSD() + " " + voucher.getMotivo());
    }

}
