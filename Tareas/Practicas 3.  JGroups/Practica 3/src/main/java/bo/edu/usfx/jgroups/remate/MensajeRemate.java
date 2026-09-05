package bo.edu.usfx.jgroups.remate;

import java.io.Serializable;

public class MensajeRemate implements Serializable {

    private static final long serialVersionUID = 1L;

    public final TipoMensaje tipo;

    public String articulo;
    public double precioBase;
    public int segundosDuracion;
    public String creador;

    public double monto;
    public String pujador;

    public long instanteCierreMillis;

    public String ganador;
    public Double montoFinal;

    public String motivo;

    private MensajeRemate(TipoMensaje tipo) {
        this.tipo = tipo;
    }

    public static MensajeRemate propuestaSubasta(String articulo, double precioBase, int segundos, String creador) {
        MensajeRemate m = new MensajeRemate(TipoMensaje.PROPUESTA_SUBASTA);
        m.articulo = articulo;
        m.precioBase = precioBase;
        m.segundosDuracion = segundos;
        m.creador = creador;
        return m;
    }

    public static MensajeRemate nuevaSubasta(String articulo, double precioBase, String creador, long instanteCierreMillis) {
        MensajeRemate m = new MensajeRemate(TipoMensaje.NUEVA_SUBASTA);
        m.articulo = articulo;
        m.precioBase = precioBase;
        m.creador = creador;
        m.instanteCierreMillis = instanteCierreMillis;
        return m;
    }

    public static MensajeRemate propuestaPuja(String articulo, double monto, String pujador) {
        MensajeRemate m = new MensajeRemate(TipoMensaje.PROPUESTA_PUJA);
        m.articulo = articulo;
        m.monto = monto;
        m.pujador = pujador;
        return m;
    }

    public static MensajeRemate pujaAceptada(String articulo, double monto, String pujador) {
        MensajeRemate m = new MensajeRemate(TipoMensaje.PUJA_ACEPTADA);
        m.articulo = articulo;
        m.monto = monto;
        m.pujador = pujador;
        return m;
    }

    public static MensajeRemate cierre(String articulo, String ganador, Double montoFinal) {
        MensajeRemate m = new MensajeRemate(TipoMensaje.CIERRE);
        m.articulo = articulo;
        m.ganador = ganador;
        m.montoFinal = montoFinal;
        return m;
    }

    public static MensajeRemate propuestaExtension(String articulo, int segundos, String creador) {
        MensajeRemate m = new MensajeRemate(TipoMensaje.PROPUESTA_EXTENSION);
        m.articulo = articulo;
        m.segundosDuracion = segundos;
        m.creador = creador;
        return m;
    }

    public static MensajeRemate extension(String articulo, long nuevoInstanteCierreMillis) {
        MensajeRemate m = new MensajeRemate(TipoMensaje.EXTENSION);
        m.articulo = articulo;
        m.instanteCierreMillis = nuevoInstanteCierreMillis;
        return m;
    }

    public static MensajeRemate rechazo(String articulo, String motivo) {
        MensajeRemate m = new MensajeRemate(TipoMensaje.RECHAZO);
        m.articulo = articulo;
        m.motivo = motivo;
        return m;
    }

    @Override
    public String toString() {
        return "MensajeRemate{" + tipo + ", articulo=" + articulo + "}";
    }
}
