package bo.edu.usfx.jgroups.remate;

import java.io.Serializable;

/**
 * PARTE 2 - Mensaje del protocolo (requisito 3 del enunciado).
 *
 * En vez de mandar texto separado por simbolos (como en las Practicas 1
 * y 2), aqui cada mensaje es un objeto Java serializable con un tipo
 * explicito (TipoMensaje). JGroups lo serializa solo al envolverlo en
 * un ObjectMessage.
 *
 * Es una unica clase con campos opcionales (segun el tipo) en vez de
 * una jerarquia de subclases, para mantenerlo simple. Cada metodo
 * "fabrica" estatico deja claro que campos importan para cada tipo.
 */
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

    public String motivo; // usado en RECHAZO

    private MensajeRemate(TipoMensaje tipo) {
        this.tipo = tipo;
    }

    // ---- Fabricas: una por cada tipo de mensaje, para no armar el objeto a mano ----

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
