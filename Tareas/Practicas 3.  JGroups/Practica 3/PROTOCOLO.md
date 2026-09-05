# Protocolo de mensajes - RemateUSFX

Todos los mensajes son instancias de `MensajeRemate` (Serializable),
con un campo `tipo` del enum `TipoMensaje`. Se envian envueltos en un
`ObjectMessage` de JGroups.

**Patron general:** cada operacion que modifica el estado sigue el
esquema "proponer / aceptar":

1. Un nodo cualquiera manda una **PROPUESTA_\*** por **UNICAST** al
   coordinador actual (`canal.getView().getCoord()`).
2. Solo el coordinador procesa las PROPUESTA_\*. Si son validas,
   responde difundiendo la version aceptada por **MULTICAST** (destino
   `null`) a todo el grupo.
3. Todos los nodos - incluido el propio coordinador, que tambien recibe
   su propio multicast - aplican el cambio a su copia local del estado
   SOLO cuando llega el mensaje de aceptacion, nunca al proponer.

Este patron evita que dos nodos diverjan si proponen algo al mismo
tiempo (ver Observacion D.2 de la Hoja de Respuestas).

---

## Tabla de mensajes

| Tipo | Quien lo envia | Via | Que hace el receptor |
|---|---|---|---|
| `PROPUESTA_SUBASTA` | Cualquier nodo | Unicast -> coordinador | El coordinador valida que el articulo no exista y que precio/duracion sean validos |
| `NUEVA_SUBASTA` | Coordinador | Multicast -> todos | Cada nodo agrega la subasta a su estado local; si el receptor es coordinador, programa el temporizador de cierre |
| `PROPUESTA_PUJA` | Cualquier nodo | Unicast -> coordinador | El coordinador valida que la subasta exista, no este cerrada, y el monto supere la mejor puja actual |
| `PUJA_ACEPTADA` | Coordinador | Multicast -> todos | Cada nodo agrega la puja al historial local de esa subasta |
| `CIERRE` | Coordinador (automatico, al vencer el temporizador) | Multicast -> todos | Cada nodo marca la subasta como cerrada, guarda ganador/monto final, cancela su temporizador si tenia uno |
| `PROPUESTA_EXTENSION` (bonus) | El creador de la subasta | Unicast -> coordinador | El coordinador valida que sea el creador y que la subasta no este cerrada |
| `EXTENSION` (bonus) | Coordinador | Multicast -> todos | Cada nodo actualiza el instante de cierre; si es coordinador, reprograma el temporizador |
| `RECHAZO` | Coordinador | Unicast -> quien propuso | Se imprime el motivo en la consola de quien propuso |

---

## Por que el instante de cierre es absoluto, no relativo

`NUEVA_SUBASTA` y `EXTENSION` llevan `instanteCierreMillis`: un valor
de tiempo absoluto (epoch millis), calculado siempre por el
**coordinador** en el momento de aceptar la propuesta. Ningun nodo
calcula su propio instante de cierre a partir de "segundos restantes".

Esto resuelve dos problemas a la vez:

1. **Relojes de equipos distintos:** si cada equipo calculara el cierre
   sumando los segundos a SU propio reloj local, dos equipos con
   relojes desincronizados cerrarian la misma subasta en momentos
   distintos. Al ser el coordinador quien decide y todos comparten ese
   mismo valor absoluto, todos coinciden.
2. **Cambio de coordinador:** el nuevo coordinador puede calcular
   cuanto tiempo REALMENTE falta (`instanteCierreMillis - ahora`) sin
   importar cuanto tiempo estuvo el grupo sin coordinador.

---

## Traspaso de responsabilidad al nuevo coordinador

Ver `NodoRemate.viewAccepted()`: cada nodo compara su propia direccion
con `vista.getCoord()` en cada cambio de vista. Al detectar que
**acaba** de convertirse en coordinador (no que ya lo era), recorre
todas las subastas abiertas de su copia del estado replicado y:

- Si el instante de cierre ya paso (la subasta debio cerrarse mientras
  no habia coordinador), la cierra de inmediato.
- Si no, programa un temporizador con el tiempo que realmente falta.

---

## Estado replicado y transferencia (getState / setState)

El estado completo es `Map<String, Subasta>` (cada `Subasta` incluye su
`List<Puja>`). Se serializa entero con `Util.objectToStream` /
`Util.objectFromStream`, igual que el `historial` de `PizarraGrupo` en
la Parte 1, pero con un mapa de objetos en vez de una lista de texto.

Un nodo que se conecta llama a `canal.getState(null, 10000)` justo
despues de `connect()`, y recibe la copia completa del coordinador
actual antes de poder pujar.
