# Practica 3 - Comunicacion indirecta con JGroups

Sistema distribuido sin servidor central: comunicacion en grupo con
JGroups. Incluye la Parte 1 (desarrollo guiado paso a paso) y la Parte
2 (ejercicio propuesto: subasta distribuida RemateUSFX).

## Requisitos

- JDK 17 o superior
- Maven 3.8 o superior
- Conexion a internet la primera vez (para descargar JGroups 5.5.6.Final, ~3 MB)

## Como abrir en NetBeans

1. `File -> Open Project`
2. Selecciona la carpeta `Practica 3` (la que tiene el `pom.xml`)
3. NetBeans reconoce el proyecto Maven automaticamente
4. Click derecho en el proyecto -> `Clean and Build`
   - La primera vez va a descargar JGroups desde Maven Central; espera a
     que termine
   - Debe terminar con `BUILD SUCCESS`

## Estructura de clases

```
src/main/java/bo/edu/usfx/jgroups/
├── NodoBasico.java       <- Parte 1, Paso 2
├── ChatGrupo.java        <- Parte 1, Paso 3
├── PizarraGrupo.java     <- Parte 1, Pasos 4, 5 y 6 (membresia, estado, unicast)
├── ContadorRPC.java      <- Parte 1, Paso 7 (RpcDispatcher)
└── remate/               <- Parte 2: subasta distribuida RemateUSFX
    ├── TipoMensaje.java      (enum del protocolo)
    ├── MensajeRemate.java    (mensaje serializable del protocolo)
    ├── Puja.java             (modelo)
    ├── Subasta.java          (modelo)
    ├── NodoRemate.java       (nodo/receptor: toda la logica JGroups)
    └── ConsolaRemate.java    (interfaz de consola, clase con el main)
```

Ver `PROTOCOLO.md` para el detalle completo del protocolo de mensajes
de RemateUSFX (que enviar, cuando, y que hace cada nodo al recibirlo).

## Como correr cada clase (dentro de NetBeans)

Click derecho sobre el archivo -> `Run File`. Para pasar argumentos
(el nombre del nodo), configura `Project Properties -> Run -> Arguments`
o usa `Run File` y edita la configuracion de esa ejecucion.

Por linea de comandos (Maven), el patron general es:

```powershell
mvn exec:java -Dexec.mainClass=<clase-completa> -Dexec.args="<nombre>"
```

### Parte 1 - Paso a paso

```powershell
mvn compile

# Paso 2
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.NodoBasico -Dexec.args="A"

# Paso 3
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.ChatGrupo -Dexec.args="ana"

# Pasos 4, 5, 6
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.PizarraGrupo -Dexec.args="A"

# Paso 7
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.ContadorRPC -Dexec.args="A"
```

### Parte 2 - RemateUSFX

```powershell
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.remate.ConsolaRemate -Dexec.args="ana"
```

Comandos disponibles dentro de la consola:
```
/crear <articulo> <precio_base> <segundos>
/subastas
/pujar <articulo> <monto>
/estado <articulo>
/quien
/ganadas
/extender <articulo> <segundos>   (bonus)
/salir
```

## UDP vs TCP (Paso 8 / requisito 10)

Por defecto todas las clases usan `udp.xml` (multicast IP). Para forzar
TCP con lista fija de miembros (necesario si el multicast esta
bloqueado en la red, algo comun en redes de campus/Wi-Fi compartido):

```powershell
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.PizarraGrupo -Dexec.args="A" ^
    -Dconfig=tcp.xml -Djgroups.bind_addr=192.168.1.25 ^
    -Djgroups.tcpping.initial_hosts=192.168.1.25[7800],192.168.1.30[7800]
```

Mismo patron para `ContadorRPC` y para `remate.ConsolaRemate` (todas
leen la propiedad `config` con `System.getProperty("config", "udp.xml")`).

## Firewall (para pruebas entre dos equipos)

PowerShell como administrador, en AMBOS equipos:

```powershell
netsh advfirewall firewall add rule name="Java JGroups" dir=in action=allow program="C:\Program Files\Java\jdk-17\bin\java.exe" enable=yes
netsh advfirewall firewall add rule name="JGroups TCP" dir=in action=allow protocol=TCP localport=7800-7802
netsh advfirewall firewall add rule name="JGroups UDP" dir=in action=allow protocol=UDP localport=45588
```

(ajusta la ruta del `java.exe` a tu instalacion real de JDK)

## Checklist de pruebas (Parte 1)

- [ ] Tres nodos conversan sin ningun proceso servidor, desde 2 equipos distintos
- [ ] Al cerrar el coordinador, los demas siguen funcionando y muestran al nuevo coordinador
- [ ] Un nodo que entra tarde recibe el historial completo
- [ ] Un mensaje privado llega solo a su destinatario
- [ ] `inc` desde un equipo modifica el contador en todos los nodos de ambos equipos
- [ ] Sabes cambiar entre udp.xml y tcp.xml

## Checklist de pruebas (Parte 2 - RemateUSFX)

Ver la tabla de 7 casos de prueba en `Hoja_de_Respuestas.docx`, seccion
"Parte 2". Cada uno debe documentarse con captura de pantalla.

## Entregables (segun el enunciado)

- [ ] Codigo fuente completo en el repositorio, carpeta `Practica 3`
- [ ] README indicando como ejecutar con UDP y con TCP (este archivo)
- [ ] Informe breve (max. 4 paginas): diagrama del protocolo, justificacion
      de la solucion para pujas simultaneas, traspaso de coordinador, capturas
- [ ] Video de max. 3 minutos entre dos equipos, incluyendo la caida del coordinador
- [ ] Hoja de respuestas completa (Parte 1 y Parte 2)
