package com.mycompany.mavenproject1;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author erick
 */

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Manejador implements Runnable {

    private final Socket cliente;
    private final int id;
    private PrintWriter salida;

    private String apodo;
    private Sala salaActual;

    public Manejador(Socket cliente, int id) {
        this.cliente = cliente;
        this.id = id;
        this.apodo = "Usuario" + id;
    }

    public PrintWriter getSalida() {
        return salida;
    }

    public String getApodo() {
        return apodo;
    }

    public void setApodo(String nuevoApodo) {
        this.apodo = nuevoApodo;
    }

    @Override
    public void run() {
        try {
            // CORREGIDO: UTF-8 explícito para que los acentos no se rompan (á, é, í, ó, ú, ñ)
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
            this.salida = new PrintWriter(
                    new OutputStreamWriter(cliente.getOutputStream(), StandardCharsets.UTF_8), true);

            ServidorMultihilo.USUARIOS.put(this.apodo, this);
            ServidorMultihilo.contadorHistorico.incrementAndGet();

            ServidorMultihilo.SALAS.putIfAbsent("general", new Sala("general"));
            this.salaActual = ServidorMultihilo.SALAS.get("general");
            this.salaActual.agregarMiembro(this);

            salida.println("Bienvenido " + apodo + ". Estás en la sala: " + salaActual.getNombre());
            salaActual.difundir("El usuario " + apodo + " ha entrado a la sala.", this);

            String linea;
            while ((linea = in.readLine()) != null) {
                if (linea.startsWith("/")) {
                    procesarComando(linea);
                } else {
                    salaActual.difundir(apodo + "> " + linea, this);
                    salida.println("Tú> " + linea);
                }
            }
        } catch (IOException e) {
            System.err.println("Conexión perdida con el cliente " + id + ": " + e.getMessage());
        } finally {
            desconectar();
        }
    }

    private void procesarComando(String linea) {
        String[] partes = linea.trim().split(" ", 3);
        String comando = partes[0].toLowerCase();

        switch (comando) {
            case "/nick":
                if (partes.length < 2) {
                    salida.println("Uso correcto: /nick <apodo>");
                    break;
                }
                String nuevoApodo = partes[1];

                // CORREGIDO: antes se usaba containsKey() y luego put() como dos pasos
                // separados, lo cual NO es atómico. Si dos clientes mandan /nick con el
                // mismo apodo al mismo tiempo, ambos podían pasar la validación antes de
                // que ninguno lo hubiera registrado todavía (condición de carrera).
                // putIfAbsent() resuelve "verificar y guardar" en una sola operación atómica.
                Manejador existente = ServidorMultihilo.USUARIOS.putIfAbsent(nuevoApodo, this);

                if (existente != null && existente != this) {
                    salida.println("Rechazado: El apodo '" + nuevoApodo + "' ya está en uso.");
                } else {
                    String apodoAnterior = this.apodo;
                    ServidorMultihilo.USUARIOS.remove(apodoAnterior);
                    this.apodo = nuevoApodo;

                    salida.println("Apodo cambiado exitosamente a: " + this.apodo);
                    salaActual.difundir("El usuario " + apodoAnterior + " ahora se llama " + this.apodo, this);
                }
                break;

            case "/salas":
                salida.println("--- Salas disponibles ---");
                for (Sala s : ServidorMultihilo.SALAS.values()) {
                    salida.println("- " + s.getNombre() + " (" + s.getMiembros().size() + " usuarios)");
                }
                break;

            case "/crear":
                if (partes.length < 2) {
                    salida.println("Uso correcto: /crear <sala>");
                    break;
                }
                String nuevaSala = partes[1];
                if (ServidorMultihilo.SALAS.putIfAbsent(nuevaSala, new Sala(nuevaSala)) != null) {
                    salida.println("Rechazado: La sala '" + nuevaSala + "' ya existe.");
                } else {
                    salida.println("Sala '" + nuevaSala + "' creada exitosamente.");
                    cambiarDeSala(nuevaSala);
                }
                break;

            case "/unirse":
                if (partes.length < 2) {
                    salida.println("Uso correcto: /unirse <sala>");
                    break;
                }
                String salaDestino = partes[1];
                if (!ServidorMultihilo.SALAS.containsKey(salaDestino)) {
                    salida.println("La sala '" + salaDestino + "' no existe. Usa /crear primero.");
                } else {
                    cambiarDeSala(salaDestino);
                }
                break;

            case "/quien":
                salida.println("--- Usuarios en la sala '" + salaActual.getNombre() + "' ---");
                for (Manejador m : salaActual.getMiembros()) {
                    salida.println("- " + m.getApodo());
                }
                break;

            case "/privado":
                if (partes.length < 3) {
                    salida.println("Uso correcto: /privado <apodo> <texto>");
                    break;
                }
                String destinatario = partes[1];
                String mensajePrivado = partes[2];

                Manejador mDestino = ServidorMultihilo.USUARIOS.get(destinatario);

                if (mDestino != null) {
                    mDestino.getSalida().println("[Privado de " + this.apodo + "]: " + mensajePrivado);
                    salida.println("[Privado para " + destinatario + "]: " + mensajePrivado);
                } else {
                    salida.println("El usuario '" + destinatario + "' no existe o no está conectado.");
                }
                break;

            case "/estado":
                salida.println("--- Estado del Servidor ---");
                salida.println("Usuarios conectados actualmente: " + ServidorMultihilo.USUARIOS.size());
                salida.println("Total histórico de conexiones: " + ServidorMultihilo.contadorHistorico.get());
                salida.println("Cantidad de salas activas: " + ServidorMultihilo.SALAS.size());
                break;

            case "/salir":
                salida.println("Desconectando del servidor...");
                try {
                    cliente.close();
                } catch (IOException e) {
                    System.err.println("Error al intentar salir: " + e.getMessage());
                }
                break;

            default:
                salida.println("Comando desconocido. Usa /nick, /salas, /crear, /unirse, /quien, /privado, /estado, /salir.");
                break;
        }
    }

    private void cambiarDeSala(String nombreNuevaSala) {
        if (salaActual.getNombre().equals(nombreNuevaSala)) {
            salida.println("Ya estás en la sala '" + nombreNuevaSala + "'.");
            return;
        }

        Sala nuevaSala = ServidorMultihilo.SALAS.get(nombreNuevaSala);

        salaActual.difundir("El usuario " + apodo + " ha salido de la sala.", this);
        salaActual.eliminarMiembro(this);

        this.salaActual = nuevaSala;
        this.salaActual.agregarMiembro(this);

        this.salaActual.difundir("El usuario " + apodo + " ha entrado a la sala.", this);
        salida.println("Te has unido a la sala: " + nombreNuevaSala);
    }

    public void desconectar() {
        ServidorMultihilo.USUARIOS.remove(this.apodo);

        if (salaActual != null) {
            salaActual.eliminarMiembro(this);
            salaActual.difundir("El usuario " + apodo + " se ha desconectado del servidor.", this);
        }

        try {
            if (!cliente.isClosed()) {
                cliente.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar el socket de " + apodo);
        }
    }
}