# Monopoly Distribuido — contexto del proyecto

Proyecto 1 de **Algoritmos y Estructuras de Datos 1**, Escuela de Ingeniería en
Computación, TEC, Semestre II-2026. Grupo de 4. Líder: José Sandí (@jsandia).

Monopoly simplificado con arquitectura cliente-servidor, estructuras de datos
lineales implementadas a mano, y un cajero electrónico con RFID.

---

## Cómo quiero que me ayudés

**Somos novatos en Java.** Veníamos de Python y C#. Explicá los conceptos nuevos
antes de usarlos (genéricos, herencia en Java, hilos, sockets). Prefiero entender
poco y bien que copiar mucho y no poder defenderlo.

**Podés escribir las estructuras de datos, pero tengo que poder defenderlas.**
El profesor puede pedirme modificar `ListaCircularDoble`, `ColaCircular`,
`ListaDoble` o `ListaSimple` en vivo durante la defensa, y entre eso y la
evaluación del código son el 35% de la nota. Así que cuando escribás o cambiés una
estructura: Javadoc completo, explicame cómo funciona (punteros, casos borde, por
qué cada decisión) y después haceme preguntas tipo examen sobre ese código.
También revisá lo que yo escriba y encontrame los bugs.

Para el resto del proyecto (servidor, sockets, Swing, reportes) también podés
escribir código, pero explicame qué hace.

**Antes de un cambio grande, proponelo primero.** No refactorices sin avisar.

---

## Reglas duras del enunciado

1. **Prohibido `java.util` en `estructuras/` y `modelo/`.** Nada de `List`,
   `ArrayList`, `LinkedList`, `Stack`, `Queue`, `PriorityQueue` ni equivalentes
   para sustituir las estructuras evaluadas. Si hace falta una colección, se
   implementa a mano. (`java.util.Random` para los dados sí se puede.)
2. **El cliente no modifica nada.** No puede tocar saldo, posición, propiedades,
   turno, dados ni transacciones. El cliente manda intenciones; el servidor valida
   y decide. Si el cliente calcula el movimiento, se pierde el criterio completo.
3. **Comunicación por sockets TCP** con protocolo propio. Nada de RMI ni HTTP.
4. **La tarjeta RFID solo identifica al jugador.** El saldo vive en el servidor.
5. **Clases mínimas obligatorias:** Juego, Servidor, Cliente, Jugador, Banco,
   Tablero, Casilla, Propiedad, CasillaEvento, CasillaEspecial, CartaEvento, Dado,
   Transaccion.
6. **La demo necesita 4 jugadores y 2 computadoras físicas** en red.

## Cómo se reparte la nota

| Criterio | % |
|---|---|
| Estructuras de datos y su uso dentro del juego | 20 |
| Lógica y funcionamiento del juego | 15 |
| Defensa y demostración (el profe pide operar las estructuras en vivo) | 15 |
| Programación orientada a objetos | 10 |
| Funcionamiento cliente-servidor | 10 |
| Módulo de cajero electrónico | 10 |
| Repositorio: 10+ commits significativos y distribuidos entre los 4 | 10 |
| Documentación, UML y calidad de código | 10 |

Cuando priorices trabajo, priorizá por esta tabla. Las estructuras primero.

---

## Decisiones técnicas ya tomadas

- **Java** con **Maven** (`groupId: cr.ac.tec`, `artifactId: monopoly`)
- **Java 26** (decidido el 1 de octubre de 2026). El proyecto se compila con JDK 26
  y no se va a bajar de versión. Quien quiera correrlo, incluido el profesor,
  necesita JDK 26 o superior.
- **Swing** para la interfaz (viene en el JDK, sin dependencias extra). No JavaFX.
- **Sockets TCP** con protocolo de texto por líneas (`COMANDO|arg1|arg2`), leído con
  `BufferedReader.readLine()` y escrito con `PrintWriter.println()`. Sin JSON.
- **Raspberry Pi Pico 2W** + lector RC522 + 2 displays de 7 segmentos + botón, en
  **MicroPython**. Como el Pico tiene WiFi, es un cliente TCP más: no necesita
  puente por USB.

## Estructura de paquetes

Los paquetes cuelgan directo de `src/main/java/`. **No se usa `cr.ac.tec.monopoly.*`**
(salvo en los tests, que viven en `src/test/java/cr/ac/tec/monopoly/...`).

```
src/main/java/
├── estructuras/   Nodo, ListaCircularDoble (package estructuras;)
├── modelo/        Jugador, Juego, Banco, Tablero, Casilla, Propiedad, CasillaEvento,
│                  CasillaEspecial, CartaEvento, Transaccion, ... (package modelo;)
├── cliente/       PanelTablero, VentanaTablero, VentanaHistorial (package cliente;)
├── protocolo/     Message (package protocolo;)
└── (por defecto)  GameServer, GameClient, Interfaz, ManejadorCajero, Discovery, ...

hardware/          MicroPython del Pico 2W
docs/              PROTOCOLO.md, UML, documento técnico
```

Cada integrante es dueño de sus paquetes; así se evitan conflictos de Git.
Las clases nuevas siguen esta misma organización; no mover nada a `cr.ac.tec.monopoly`.

## Las cuatro estructuras y dónde va cada una

| Estructura | Se usa en | Por qué esa |
|---|---|---|
| `ListaCircularDoble<T>` | Tablero (mín. 24 casillas) | Lo exige el enunciado. Avanzar N pasos son N saltos a `siguiente`; las cartas de retroceder usan `anterior` |
| `ColaCircular<T>` | Turnos de jugadores | Sacar del frente y meter al final *es* pasar el turno |
| `ColaCircular<T>` | Mazo de cartas de evento | "La carta usada pasa al final". Misma clase, otro tipo genérico |
| `ListaDoble<T>` | Historial de transacciones | Debe recorrerse desde la más antigua **y** desde la más reciente |
| `ListaSimple<T>` | Propiedades de cada jugador | Solo se agrega y se recorre |

## Protocolo (borrador — falta formalizarlo en docs/PROTOCOLO.md)

```
# Cliente → Servidor
CONECTAR|nombre|uidRFID
TIRAR_DADOS
COMPRAR_PROPIEDAD
NO_COMPRAR
TERMINAR_TURNO
CONSULTAR_ESTADO
CONSULTAR_TRANSACCIONES|filtro|valor
EXPORTAR_TXT

# Cajero (Pico 2W) → Servidor
DADOS_FISICOS|4|3
RFID|A4B2C1D9

# Servidor → Clientes (broadcast)
OK|mensaje
ERROR|CODIGO|mensaje
ESTADO|turno=7|enTurno=2|j1,1500,4,activo|...
DADOS|idJugador|d1|d2
MOVIMIENTO|idJugador|casillaDestino
TRANSACCION|id|turno|tipo|origen|destino|monto|descripcion
EVENTO|idCarta|texto
FIN|idGanador|patrimonio
```

Regla: después de cada acción importante el servidor manda un `ESTADO` completo
a todos los clientes.

## Convenciones de código

- **Nombres de clases, métodos y variables en español** (`lanzarDados`,
  `saldoActual`, `agregarAlFinal`). Es lo que usamos en todo el proyecto.
- Clases en `PascalCase`, métodos y variables en `camelCase`.
- Javadoc obligatorio en las estructuras: es parte del 10% de documentación.
- Todos los métodos de `Juego` y `Banco` que modifican estado van `synchronized`
  (el servidor tiene un hilo por cliente).

## Git

- Una rama por persona (`nombre/area`), merge a `main` mínimo 2 veces por semana.
- Cada quien commitea desde su propia cuenta: la rúbrica evalúa que los commits
  estén **distribuidos entre los 4**.
- Mensajes que digan qué se hizo: "Agrega insertarDespuesDe en ListaCircularDoble",
  no "cambios" ni "hola".
- Nunca subir `target/`.

---

## Estado actual (18 de septiembre de 2026)

Lo que existe:
- Proyecto Maven corriendo, repo en GitHub con los integrantes agregados
- `src/main/java/Interfaz.java` — pantalla de login en Swing, en el paquete por
  defecto (se queda ahí).
- `usuarios.txt`

Lo que **no** existe todavía y es lo que más pesa:
- Ninguna de las 4 estructuras de datos
- Ninguna clase del modelo (Jugador, Tablero, Casilla, Propiedad, Transaccion...)
- Servidor, sockets, protocolo formalizado
- Nada del cajero electrónico
- `docs/` y `hardware/` están vacíos

**Siguiente prioridad:** las estructuras y el modelo. El login no es un requisito
del enunciado; el tablero sobre lista circular doble sí, y vale 20%.
