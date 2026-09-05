package bo.edu.usfx.jgroups.remate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Subasta implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String articulo;
    private final double precioBase;
    private final String creador;
    private long instanteCierreMillis;
    private boolean cerrada;
    private String ganador;
    private Double montoFinal;

    private final List<Puja> historial = new ArrayList<>();

    public Subasta(String articulo, double precioBase, String creador, long instanteCierreMillis) {
        this.articulo = articulo;
        this.precioBase = precioBase;
        this.creador = creador;
        this.instanteCierreMillis = instanteCierreMillis;
        this.cerrada = false;
    }

    public String getArticulo() {
        return articulo;
    }

    public double getPrecioBase() {
        return precioBase;
    }

    public String getCreador() {
        return creador;
    }

    public long getInstanteCierreMillis() {
        return instanteCierreMillis;
    }

    public void setInstanteCierreMillis(long instanteCierreMillis) {
        this.instanteCierreMillis = instanteCierreMillis;
    }

    public boolean isCerrada() {
        return cerrada;
    }

    public void setCerrada(boolean cerrada) {
        this.cerrada = cerrada;
    }

    public String getGanador() {
        return ganador;
    }

    public void setGanador(String ganador) {
        this.ganador = ganador;
    }

    public Double getMontoFinal() {
        return montoFinal;
    }

    public void setMontoFinal(Double montoFinal) {
        this.montoFinal = montoFinal;
    }

    public List<Puja> getHistorial() {
        return historial;
    }

    public Puja getMejorPuja() {
        if (historial.isEmpty()) {
            return null;
        }
        return historial.get(historial.size() - 1);
    }

    public double getMontoActual() {
        Puja mejor = getMejorPuja();
        return mejor != null ? mejor.getMonto() : precioBase;
    }

    public long segundosRestantes() {
        long restante = (instanteCierreMillis - System.currentTimeMillis()) / 1000;
        return Math.max(restante, 0);
    }

    @Override
    public String toString() {
        String estado = cerrada ? "CERRADA" : (segundosRestantes() + "s restantes");
        Puja mejor = getMejorPuja();
        String pujaTexto = mejor != null ? (mejor.getPujador() + " (" + mejor.getMonto() + ")") : "sin pujas";
        return articulo + " | base=" + precioBase + " | mejor=" + pujaTexto + " | " + estado;
    }
}
