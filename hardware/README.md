# Cajero electrónico (Raspberry Pi Pico 2 W)

El cajero es el módulo físico del proyecto: funciona como **dado electrónico**
(dos displays de 7 segmentos y un botón) y como **lector de tarjetas RFID**
(RC522). Se conecta al servidor Java por Wi-Fi y TCP, como un cliente más.

La tarjeta **solo identifica al jugador**: el saldo vive siempre en el servidor.

## Archivos

| Archivo | Qué hace |
|---|---|
| `main.py` | Programa principal. Núcleo 0: display, botón, dados y red. Núcleo 1: lector RC522. |
| `cajero_red.py` | Conexión Wi-Fi + TCP con el servidor (`ConexionCajero`). **Aquí se configuran la red y la IP.** |
| `mfrc522.py` | Librería del lector RC522, sin modificar, de [danjperron/micropython-mfrc522](https://github.com/danjperron/micropython-mfrc522). |

## Componentes

- Raspberry Pi Pico 2 W con MicroPython (firmware **RPI_PICO2_W**, no el de la Pico W original)
- Lector RFID RC522 y al menos 2 tarjetas o llaveros
- Display doble de 7 segmentos de **cátodo común**
- Botón pulsador
- 7 resistencias de 1 kΩ (una por segmento)

## Conexiones

### Display de 7 segmentos

| Pico 2 W | Display |
|---|---|
| GP0 – GP6 | Segmentos a, b, c, d, e, f, g (cada uno con su resistencia de 1 kΩ) |
| GP7 | Cátodo del dígito 1 |
| GP8 | Cátodo del dígito 2 |

Los dos dígitos comparten los 7 segmentos. El programa enciende un dígito a la
vez, 5 ms cada uno (multiplexado), tan rápido que el ojo ve los dos encendidos.

### Botón

| Pico 2 W | Botón |
|---|---|
| GP15 | Una pata |
| GND | La otra pata |

Se usa la resistencia *pull-up* interna: suelto = 1, presionado = 0.

### Lector RC522

| RC522 | Pico 2 W |
|---|---|
| SDA (CS) | GP17 |
| SCK | GP18 |
| MOSI | GP19 |
| MISO | GP16 |
| RST | GP22 |
| 3.3V | 3V3 (**nunca 5V**) |
| GND | GND |

## Cómo cargarlo en la Pico

1. Instalar el firmware de MicroPython para **RPI_PICO2_W**.
2. Abrir `cajero_red.py` y cambiar `NOMBRE_RED`, `CLAVE_RED` e `IP_SERVIDOR`.
   **No subir esos datos reales a Git**: en el repositorio quedan valores de ejemplo.
3. Con Thonny, guardar en la Pico los tres archivos: `mfrc522.py`, `cajero_red.py` y `main.py`.
4. Arrancar primero el servidor Java y después la Pico (o reiniciarla). El
   LED integrado se enciende mientras el dado está girando.

La IP de la computadora servidor se ve con `ipconfig` (Windows) o
`ipconfig getifaddr en0` (macOS). La Pico solo usa redes de **2.4 GHz** y no
soporta redes empresariales como eduroam: lo más simple es usar el hotspot de
un celular, con la Pico y las computadoras conectadas a él.

## Protocolo con el servidor

Texto, **una línea por mensaje** (termina en `\n`), partes separadas por `;`.
El servidor escucha al cajero en el puerto **5050** (`Configuracion.PUERTO_CAJERO`).

| Dirección | Mensaje | Significado |
|---|---|---|
| Pico → Servidor | `HOLA;CAJERO` | Saludo al conectarse |
| Pico → Servidor | `DADOS;d1;d2` | Resultado de una tirada (1 a 6 cada uno) |
| Pico → Servidor | `TARJETA;UID` | Se leyó una tarjeta; UID en hexadecimal, por ejemplo `A3F29B11` |
| Servidor → Pico | `BIENVENIDO` | Respuesta al saludo |
| Servidor → Pico | `OK_DADOS` | La tirada se aplicó al jugador en turno |
| Servidor → Pico | `ERROR;NO_HAY_PARTIDA` | Todavía no empezó la partida |
| Servidor → Pico | `ERROR;PARTIDA_TERMINADA` | La partida ya terminó |
| Servidor → Pico | `ERROR;YA_TIRO` | El jugador en turno ya tiró en este turno |
| Servidor → Pico | `ERROR;DADO_INVALIDO` | Algún dado fuera de 1 a 6 |
| Servidor → Pico | `ERROR;FORMATO_DADOS` / `ERROR;FORMATO_TARJETA` | Mensaje mal formado |
| Servidor → Pico | `OK_TARJETA` | Tarjeta recibida |

El cajero no decide nada: solo manda lo que pasó (dados o tarjeta) y el
servidor valida. Una misma tarjeta sirve para registrar al jugador antes de
empezar y para autorizar pagos durante la partida.

## Errores comunes

| Síntoma | Causa probable |
|---|---|
| No conecta al Wi-Fi (estado -2) | Red no encontrada: nombre mal escrito o red de 5 GHz |
| No conecta al Wi-Fi (estado -3) | Clave incorrecta |
| `ECONNREFUSED` | El servidor Java no está corriendo |
| `ETIMEDOUT` | IP incorrecta, firewall, o la red aísla a los dispositivos |
| `ImportError: mfrc522` | La librería no está guardada en la Pico |
| No lee tarjetas | Cableado, o el lector alimentado con 5V en vez de 3.3V |
| Un dígito del display no se ve | Cátodo mal conectado (GP7 / GP8) o display de ánodo común |
