# Auditoria - Ejercicio 2: Control de aforo con JGroups

## Compilacion

`mvn clean compile` -> **BUILD SUCCESS** (Maven de NetBeans, JDK 21, `maven.compiler.release` 17).
La libreria `org.jgroups:jgroups:5.5.6.Final` se descargo realmente desde Maven Central.

## Pruebas

Cuatro procesos `NodoPuerta` (A, B, C, D) con aforo 10, alimentados por stdin con pausas y salida a archivos de log.
Se ejecuto con `-Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1` (canal `udp.xml` por defecto) por los adaptadores virtuales de la PC.
Todos los casos se verificaron en los logs de todos los nodos.

| # | Caso | Esperado | Obtenido |
|---|------|----------|----------|
| 1 | A y B arrancan | A coordinador; viewAccepted muestra entradas y coordinador | A: `Coordinador actual: A`, luego `ENTRO: B`. B: `Puertas en el grupo: [A, B]`, coordinador A. OK |
| 2 | A `/entrar 4`, B `/entrar 4` | `/estado` = 8/10 en todos | A, B y C (unida despues por state transfer) muestran 8/10. OK |
| 3 | C `/entrar 3` (8+3>10) | RECHAZADO solo en C | `RECHAZADO` solo en el log de C; A y B no imprimen rechazo; ocupacion sigue 8/10. OK |
| 4 | C `/entrar 2` | 10/10 y AFORO COMPLETO en las tres | A, B y C imprimen `ACEPTADO ... 10/10` y `*** AFORO COMPLETO ***`. OK |
| 5 | B `/salir 3` | 7/10 en todos, deja de estar completo | `SALIDA: B -3 -> 7/10` en A, B y C; `/estado` = 7/10 sin aviso de aforo completo. OK |
| 6 | D se une tarde | `/estado` = 7/10 de inmediato | D: `Estado recibido: 7/10` y `Ocupacion: 7/10`. OK |
| 7 | Se apaga el coordinador A | `SALIO: A`, nuevo coordinador, `/entrar` sigue funcionando | B, C y D: `SALIO: A` y `Coordinador actual: B`; B `/entrar 1` -> ACEPTADO 8/10 en todos. OK |
| 8 | Carrera: ocupacion 0, aforo 10, C y D `/entrar 6` casi a la vez | Un ACEPTADO, un RECHAZADO, ocupacion final 6 | `ACEPTADO: D +6 -> 6/10` y `RECHAZADO: C ... 6` solo en C; `/estado` = 6/10 en B, C y D. OK |

Antes del caso 8 se vacio la ocupacion con `/salir 8` desde D (8 -> 0).
Al terminar se mataron todos los procesos java y se borro `target/`.

## Como se resolvio la carrera

Solo el coordinador decide, pero la ocupacion se aplica cuando el ACEPTADO vuelve por multicast.
Si decidiera solo con la ocupacion ya aplicada, dos SOLICITUD seguidas se evaluarian con el mismo valor y ambas pasarian, superando el aforo.

El coordinador mantiene un contador `reservadas` con lo ya aceptado pero aun no aplicado:

- Decide con `ocupacion + reservadas + n <= aforo`; la comprobacion y la suma a `reservadas` ocurren juntas en un bloque `synchronized`.
- Al aceptar suma `n` a `reservadas`.
- Cuando su propio ACEPTADO regresa por `receive()` (origen = el coordinador), resta `n` a `reservadas`; en ese mismo momento `ocupacion` ya incluye `n`.

Asi la segunda solicitud de 6 personas ve `0 + 6 + 6 > 10` y se rechaza aunque el primer ACEPTADO aun no se haya aplicado.

## Notas

- El aforo de cada puerta es un argumento; decide el del coordinador vigente, asi que todas deben arrancar con el mismo valor.
- Si el coordinador cae, las solicitudes que tuviera en curso se pierden y el nuevo coordinador empieza con `reservadas = 0`.
