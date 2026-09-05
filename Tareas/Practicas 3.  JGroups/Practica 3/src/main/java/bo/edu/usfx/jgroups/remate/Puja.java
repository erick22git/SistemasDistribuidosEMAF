package bo.edu.usfx.jgroups.remate;

import java.io.Serializable;

public class Puja implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String pujador;
    private final double monto;
    private final long instanteMillis;

    public Puja(String pujador, double monto, long instanteMillis) {
        this.pujador = pujador;
        this.monto = monto;
        this.instanteMillis = instanteMillis;
    }

    public String getPujador() {
        return pujador;
    }

    public double getMonto() {
        return monto;
    }

    public long getInstanteMillis() {
        return instanteMillis;
    }

    @Override
    public String toString() {
        return pujador + " -> " + monto;
    }
}
