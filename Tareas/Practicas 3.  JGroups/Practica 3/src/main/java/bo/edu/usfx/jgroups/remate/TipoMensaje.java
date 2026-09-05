package bo.edu.usfx.jgroups.remate;

/**
 * PARTE 2 - Protocolo de mensajes (requisito 3 del enunciado).
 *
 * Cada valor representa un tipo de mensaje que viaja envuelto en
 * MensajeRemate. Ver PROTOCOLO.md para la tabla completa de quien
 * envia cada uno, por que via (unicast/multicast) y que hace el
 * receptor.
 *
 * Patron general del protocolo: "proponer" (unicast al coordinador) vs
 * "aceptar" (multicast del coordinador a todos). Ningun nodo aplica un
 * cambio a su estado local solo por proponerlo - solo lo aplica cuando
 * le llega la version ACEPTADA/CIERRE, que siempre la origina el
 * coordinador. Esto evita que dos nodos diverjan si proponen algo al
 * mismo tiempo (requisito 5).
 */
public enum TipoMensaje {

    // ---- Creacion de subastas ----
    PROPUESTA_SUBASTA,   // nodo -> coordinador (unicast)
    NUEVA_SUBASTA,       // coordinador -> todos (multicast)

    // ---- Pujas ----
    PROPUESTA_PUJA,      // nodo -> coordinador (unicast)
    PUJA_ACEPTADA,       // coordinador -> todos (multicast)

    // ---- Cierre ----
    CIERRE,              // coordinador -> todos (multicast), automatico al expirar el tiempo

    // ---- Extension de tiempo (bonus) ----
    PROPUESTA_EXTENSION, // nodo (debe ser el creador) -> coordinador (unicast)
    EXTENSION,           // coordinador -> todos (multicast)

    // ---- Rechazo generico ----
    RECHAZO              // coordinador -> nodo que propuso (unicast)
}
