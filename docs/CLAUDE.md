# CLAUDE.md — Proyecto 1: Monopoly distribuido

## Sobre el equipo y cómo trabajar conmigo

- Soy Jota, estudiante de Ingeniería en Computadores del TEC (Costa Rica) y líder de un grupo de 4.
- Curso: Algoritmos y Estructuras de Datos 1, Semestre II-2026. Entrega aproximada: 4-5 semanas desde septiembre 2026.
- **Somos principiantes en Java e IntelliJ IDEA.** Mi nivel de Python es principiante.
- **Siempre explicar las cosas por pasos y a detalle**: qué hace cada bloque de código y por qué.
- Responder en español.
- Antes de hacer cambios grandes, explicar el plan y esperar confirmación.

## Entorno

- Java 21, IntelliJ IDEA, macOS.
- Interfaz del cliente: **Java Swing**.
- Hardware: **Raspberry Pi Pico 2W** (chip RP2350) con MicroPython, programada desde **Thonny**.

## Convenciones de código

- Nombres de variables, métodos y clases **en español** (ej: `procesarMensaje`, `ManejadorCajero`, `tirar_dados`).
- Java: clases pequeñas con una sola responsabilidad (principios SOLID).
- Python/MicroPython: f-strings para formatear texto; prefiero concatenar listas en vez de `.append()`.
- El proyecto debe usar **estructuras lineales** (listas enlazadas, pilas, colas) implementadas por nosotros donde el enunciado lo pida.
- Comentarios en español explicando la lógica no obvia.

## Arquitectura

Cliente-servidor por sockets TCP:

```
[Botón dado] ─┐
              ├─► Pico 2W ──Wi-Fi (TCP, puerto 5000)──► Servidor Java ──► Clientes Swing
[Lector RFID]─┘
```

- La Pico 2W es el **cajero electrónico** (dado + lector RFID). Funciona como un cliente más del servidor, sin pantalla.
- Cada conexión se atiende en su propio `Thread`.
- Al conectarse, cada cliente manda una primera línea para identificarse. Si es `HOLA;CAJERO`, el servidor la atiende con `ManejadorCajero`; si no, es un cliente Swing.
- Los métodos que modifican el estado del juego (mover ficha, cobrar, etc.) deben ser `synchronized`, porque varios hilos los usan.

## Protocolo del cajero (Pico ↔ Servidor)

Mensajes de texto, **uno por línea** (terminan en `\n`), partes separadas por `;`.

| Dirección | Mensaje | Significado |
|---|---|---|
| Pico → Servidor | `HOLA;CAJERO` | La Pico se identifica al conectarse |
| Pico → Servidor | `DADOS;d1;d2` | Tirada de dos dados (valores 1-6) |
| Pico → Servidor | `TARJETA;UID` | Tarjeta RFID leída, UID en hexadecimal (ej: `A3F29B11`) |
| Servidor → Pico | `BIENVENIDO` | Respuesta al saludo |
| Servidor → Pico | `OK_DADOS` / `OK_TARJETA` | Confirmaciones |

En Java se procesa con `linea.split(";")` y un `switch` sobre `partes[0]`.

Pendiente: asociar cada UID de tarjeta a un jugador (idea: guardar el UID en cada nodo de la lista enlazada de jugadores).

## Hardware de la Pico 2W

Firmware: MicroPython para la placa **RPI_PICO2_W** (no el de la Pico W original).
Librería RFID: `mfrc522.py` del repositorio `danjperron/micropython-mfrc522`, guardada en la Pico.

| MFRC522 | Pico 2W |
|---|---|
| SDA (CS) | GP17 |
| SCK | GP18 |
| MOSI | GP19 |
| MISO | GP16 |
| RST | GP20 |
| GND | GND |
| 3.3V | 3V3 (pin 36), nunca 5V |

- Botón del dado: GP15 a GND, con pull-up interno (`Pin.PULL_UP`, presionado = 0).
- LED integrado (`Pin("LED")`) encendido = conectado al servidor.
- `main.py` en la Pico: conecta Wi-Fi → abre socket → loop que revisa botón, RFID y mensajes del servidor. Usa `settimeout(0.1)` para que `recv()` no bloquee.

## Red y pruebas

- La Pico solo usa Wi-Fi de **2.4 GHz** y no soporta redes empresariales (eduroam). Usar **hotspot del celular** con la Mac y la Pico conectadas ahí.
- IP de la Mac: `ipconfig getifaddr en0` en la Terminal.
- Permitir conexiones entrantes a Java en el firewall de macOS.
- Orden de prueba: primero ejecutar el servidor (`PruebaServidor`), después `main.py` en la Pico.

### Errores comunes

| Error | Causa probable |
|---|---|
| No conecta al Wi-Fi | Clave mal escrita o red de 5 GHz |
| `ECONNREFUSED` / `ETIMEDOUT` | IP incorrecta, servidor apagado, firewall o red que aísla dispositivos |
| `ImportError: mfrc522` | Librería no guardada en la Pico |
| No lee tarjetas | Cableado, alimentación a 3.3V |

## Clases de prueba ya diseñadas

- `ManejadorCajero` (implements `Runnable`): lee líneas con `BufferedReader`, responde con `PrintWriter`, método `procesarMensaje`.
- `PruebaServidor`: `ServerSocket` en el puerto 5000, crea un `Thread` por conexión.

## Siguiente paso

Integrar `ManejadorCajero` al servidor real del Monopoly (revisar cómo recibe conexiones actualmente).
