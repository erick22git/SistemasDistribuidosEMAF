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
public class Migracion implements Serializable{
    private boolean PAIS;
    private String nropass;

    public Migracion(boolean PAIS, String nropass) {
        this.PAIS = PAIS;
        this.nropass = nropass;
    }

    public boolean isPAIS() {
        return PAIS;
    }

    public void setPAIS(boolean PAIS) {
        this.PAIS = PAIS;
    }

    public String getNropass() {
        return nropass;
    }

    public void setNropass(String nropass) {
        this.nropass = nropass;
    }
     
    
}
