package com.mycompany.practicas2.soquetsrmi;

import java.io.Serializable;

/**
 * Representa una cuenta bancaria. Implementa Serializable porque viaja
 * dentro de RespuestaCuenta a traves de RMI.
 */
public class Cuenta implements Serializable {

    private Banco banco;
    private String nrocuenta;
    private String ci;
    private String nombres;
    private String apellidos;
    private Double saldo;

    public Cuenta() {
    }

    public Cuenta(Banco banco, String nrocuenta, String ci, String nombres, String apellidos, Double saldo) {
        this.banco = banco;
        this.nrocuenta = nrocuenta;
        this.ci = ci;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.saldo = saldo;
    }

    public Banco getBanco() { return banco; }
    public void setBanco(Banco banco) { this.banco = banco; }

    public String getNrocuenta() { return nrocuenta; }
    public void setNrocuenta(String nrocuenta) { this.nrocuenta = nrocuenta; }

    public String getCi() { return ci; }
    public void setCi(String ci) { this.ci = ci; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }

    @Override
    public String toString() {
        return "Cuenta{" + "banco=" + banco + ", nrocuenta=" + nrocuenta
                + ", ci=" + ci + ", nombres=" + nombres + ", apellidos=" + apellidos
                + ", saldo=" + saldo + '}';
    }
}