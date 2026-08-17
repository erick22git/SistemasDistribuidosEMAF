/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mavenproject1;

/**
 *
 * @author erick
 */
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Sala {

    private final String nombre;
    private final Set<Manejador> miembros = new CopyOnWriteArraySet<>();

    public Sala(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public void agregarMiembro(Manejador m) {
        miembros.add(m);
    }

    public void eliminarMiembro(Manejador m) {
        miembros.remove(m);
    }

    public Set<Manejador> getMiembros() {
        return miembros;
    }

    public void difundir(String mensaje, Manejador remitente) {
        for (Manejador m : miembros) {
            if (m != remitente && m.getSalida() != null) {
                m.getSalida().println(mensaje);
            }
        }
    }
}
