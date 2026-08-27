package com.mycompany.practicas2.soquetsrmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Contrato RMI entre el Cliente Juez y el Servidor Justicia.
 * Definida por SEBAS, implementada por ERICK en ServidorJusticia.
 */
public interface IServidorJusticia extends Remote {

    /**
     * Consulta en ambos bancos (Mercantil por TCP, BCP por UDP) las cuentas
     * asociadas a una persona.
     */
    RespuestaCuenta ConsultarCuentas(String ci, String nombres, String apellidos) throws RemoteException;

    /**
     * Ordena el congelamiento de una cuenta especifica en el banco indicado.
     */
    RespuestaCuenta Congelar(String nrocuenta, Banco banco, Double monto) throws RemoteException;
}
