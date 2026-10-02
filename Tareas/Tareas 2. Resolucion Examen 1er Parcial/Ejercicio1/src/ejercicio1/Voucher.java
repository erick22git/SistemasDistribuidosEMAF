/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ejercicio1;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author USUARIO
 */
public class Voucher implements Serializable{
    private boolean confirmado;
    private String codigoCompra,motivo;
    private double montoUSD;
     private ArrayList<Pago>pagos;
     public Voucher(){pagos=new ArrayList<>();}

    public Voucher(boolean confirmado, String codigoCompra, String motivo, double montoUSD, ArrayList<Pago> pagos) {
        this.confirmado = confirmado;
        this.codigoCompra = codigoCompra;
        this.motivo = motivo;
        this.montoUSD = montoUSD;
        this.pagos = pagos;
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public void setConfirmado(boolean confirmado) {
        this.confirmado = confirmado;
    }

    public String getCodigoCompra() {
        return codigoCompra;
    }

    public void setCodigoCompra(String codigoCompra) {
        this.codigoCompra = codigoCompra;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public double getMontoUSD() {
        return montoUSD;
    }

    public void setMontoUSD(double montoUSD) {
        this.montoUSD = montoUSD;
    }

    public ArrayList<Pago> getPagos() {
        return pagos;
    }

    public void setPagos(ArrayList<Pago> pagos) {
        this.pagos = pagos;
    }

      
}
