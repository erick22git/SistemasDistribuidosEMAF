package com.mycompany.practicas2.soquetsrmi;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Objeto de respuesta que Justicia devuelve al Juez via RMI.
 */
public class RespuestaCuenta implements Serializable {

    private boolean error;
    private String mensaje;
    private ArrayList<Cuenta> cuentas;

    public RespuestaCuenta() {
        this.cuentas = new ArrayList<>();
    }

    public RespuestaCuenta(boolean error, String mensaje, ArrayList<Cuenta> cuentas) {
        this.error = error;
        this.mensaje = mensaje;
        this.cuentas = cuentas;
    }

    public boolean isError() { return error; }
    public void setError(boolean error) { this.error = error; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public ArrayList<Cuenta> getCuentas() { return cuentas; }
    public void setCuentas(ArrayList<Cuenta> cuentas) { this.cuentas = cuentas; }
}