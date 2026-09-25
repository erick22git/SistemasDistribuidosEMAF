/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ejercicio1;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author USUARIO
 */
public interface IOperador extends Remote {

    Voucher procesarPago(String codigoCompra, String motivo, double montoUSD) throws RemoteException;

    Migracion verificarMigracion(String nropass) throws RemoteException;

}