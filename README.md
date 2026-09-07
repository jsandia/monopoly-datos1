# Monopoly Distribuido

Proyecto 1 — Algoritmos y Estructuras de Datos 1
Escuela de Ingeniería en Computación, TEC — Semestre II-2026

Versión simplificada de Monopoly con arquitectura cliente-servidor, estructuras
de datos lineales propias y un cajero electrónico (dado + lector RFID).

## Integrantes

| Nombre | GitHub | Dueño de |
|---|---|---|
| José Sandí | @jsandia | `servidor/` + protocolo + integración |
| _(pendiente)_ | | `estructuras/` |
| _(pendiente)_ | | `modelo/` + `cliente/` |
| _(pendiente)_ | | `hardware/` |

## Cómo correrlo

Requisitos: JDK instalado y Maven (viene con IntelliJ).

```bash
# 1. Levantar el servidor (una sola máquina, la del organizador)
mvn -q compile exec:java -Dexec.mainClass="cr.ac.tec.monopoly.servidor.Servidor"

# 2. Cada jugador levanta su cliente y se conecta a la IP del servidor
mvn -q compile exec:java -Dexec.mainClass="cr.ac.tec.monopoly.cliente.Cliente"
```

> Todavía no existen esas clases. Este README se va llenando conforme avanza el proyecto.

## Estructura del repositorio

```
src/main/java/cr/ac/tec/monopoly/
├── estructuras/   Nodo, ListaCircularDoble, ColaCircular, ListaDoble, ListaSimple
├── modelo/        Jugador, Tablero, Casilla, Propiedad, CasillaEvento,
│                  CasillaEspecial, CartaEvento, Dado, Transaccion
├── servidor/      Servidor, ManejadorCliente, Banco, Juego, Protocolo
├── cliente/       Cliente, ConexionServidor, VentanaJuego, PanelTablero
└── util/          ReporteTxt

hardware/          Código MicroPython del Pico 2W (RC522 + displays + botón)
docs/              PROTOCOLO.md, UML, documento técnico
```

## Reglas del equipo

1. **Prohibido `java.util` en `estructuras/` y `modelo/`.** El enunciado no permite
   `List`, `LinkedList`, `Stack`, `Queue` ni `PriorityQueue` para sustituir las
   estructuras evaluadas. Si hace falta una colección, se implementa.
2. **Cada quien es dueño de sus paquetes.** No se edita el paquete de otro sin avisar.
   Así se evitan los conflictos de Git.
3. **Una rama por persona** (`jota/servidor`, etc.). Merge a `main` mínimo dos veces
   por semana.
4. **Cada quien commitea desde su propia cuenta.** La rúbrica evalúa que los commits
   estén distribuidos entre los cuatro. Verificar con `git config user.email`.
5. **Mensajes de commit que digan qué se hizo:** "Agrega insertarDespuesDe en
   ListaCircularDoble", no "cambios".
6. **El servidor manda.** El cliente pide acciones; nunca calcula ni modifica saldo,
   posición, propiedades, turno, dados ni transacciones.

## Estado

- [x] Proyecto Maven creado y corriendo
- [ ] Protocolo definido (`docs/PROTOCOLO.md`)
- [ ] Estructuras de datos implementadas
- [ ] Modelo del juego
- [ ] Partida completa en consola, sin red
- [ ] Servidor y clientes por sockets TCP
- [ ] Interfaz Swing
- [ ] Cajero electrónico
- [ ] Exportación del historial a TXT
- [ ] Documento técnico y UML
