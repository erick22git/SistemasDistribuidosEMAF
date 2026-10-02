/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ejercicio1;

import java.io.Serializable;


/**
 *
 * @author USUARIO
 */
public class Pago implements Serializable{
    private boolean aprobado;
    private  String codigoAutorizacion, motivo;
    private double saldo;
    public Pago(){};

    public Pago(boolean aprobado, String codigoAutorizacion, String motivo, double saldo) {
        this.aprobado = aprobado;
        this.codigoAutorizacion = codigoAutorizacion;
        this.motivo = motivo;
        this.saldo = saldo;
    }

    public boolean isAprobado() {
        return aprobado;
    }

    public void setAprobado(boolean aprobado) {
        this.aprobado = aprobado;
    }

    public String getCodigoAutorizacion() {
        return codigoAutorizacion;
    }

    public void setCodigoAutorizacion(String codigoAutorizacion) {
        this.codigoAutorizacion = codigoAutorizacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

}
