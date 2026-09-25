/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author USUARIO
 */
public class Operador extends UnicastRemoteObject implements IOperador {

    public Operador() throws RemoteException {
        super();
    }

    @Override
    public Voucher procesarPago(String 