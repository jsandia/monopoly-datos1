# Conexion del cajero (Pico 2 W) con el servidor Java por Wi-Fi + TCP.
# Se guarda en la Pico junto a main.py y mfrc522.py.
#
# Uso desde main.py:
#   from cajero_red import ConexionCajero, uid_a_texto
#   cajero = ConexionCajero()
#   cajero.conectar()
#   cajero.enviar(f"DADOS;{dado1};{dado2}")
#   for mensaje in cajero.revisar_mensajes(): ...

import network
import socket
import select
import time

# 1) Datos de la red y del servidor
NOMBRE_RED = "NOMBRE_DE_LA_RED"     # solo redes de 2.4 GHz
CLAVE_RED = "CLAVE_DE_LA_RED"
IP_SERVIDOR = "192.168.0.10"        # IP de la computadora con el servidor Java
PUERTO_SERVIDOR = 5050              # 5000 lo ocupa AirPlay en macOS

# 2) Tiempos
TIEMPO_MAXIMO_WIFI_S = 30           # con señal debil puede tardar mas de 15 s
TIEMPO_MAXIMO_ENVIO_S = 2           # cuanto puede tardar un envio


def conectar_wifi():
    # Devuelve True si la Pico quedo conectada al Wi-Fi, False si no
    wlan = network.WLAN(network.STA_IF)   # STA = la Pico se conecta a una red existente

    # Reiniciar el chip Wi-Fi por si quedo trabado del soft reboot de Thonny
    wlan.active(False)
    time.sleep(1)
    wlan.active(True)
    wlan.config(pm=0xa11140)   # sin ahorro de energia: mas estable con señal debil

    print(f"Conectando a la red {NOMBRE_RED}...")
    wlan.connect(NOMBRE_RED, CLAVE_RED)
    inicio = time.time()
    while not wlan.isconnected():
        if time.time() - inicio > TIEMPO_MAXIMO_WIFI_S:
            # Estados: -1 fallo, -2 red no encontrada, -3 clave incorrecta
            print(f"No se pudo conectar al Wi-Fi. Estado final: {wlan.status()}")
            return False
        time.sleep(0.5)

    print(f"Wi-Fi conectado. IP de la Pico: {wlan.ifconfig()[0]}")
    return True


def uid_a_texto(uid):
    # Convierte la lista de bytes del lector en texto hexadecimal
    # Ejemplo: [163, 242, 155, 17] -> "A3F29B11"
    return "".join([f"{byte:02X}" for byte in uid])


class ConexionCajero:
    def __init__(self):
        self.socket = None
        self.poller = None
        self.pendiente = b""   # bytes recibidos que todavia no forman una linea completa

    def conectado(self):
        return self.socket is not None

    def conectar(self):
        # Devuelve True si quedo conectado al servidor, False si algo fallo
        if not conectar_wifi():
            return False
        try:
            direccion = socket.getaddrinfo(IP_SERVIDOR, PUERTO_SERVIDOR)[0][-1]
            self.socket = socket.socket()
            self.socket.settimeout(TIEMPO_MAXIMO_ENVIO_S)
            self.socket.connect(direccion)
        except OSError as error:
            # ECONNREFUSED: servidor apagado / ETIMEDOUT: IP mala o firewall
            print(f"No se pudo conectar al servidor {IP_SERVIDOR}:{PUERTO_SERVIDOR} -> {error}")
            self.cerrar()
            return False

        # El poller permite preguntar "¿llego algo?" sin quedarse esperando,
        # asi el display de 7 segmentos no parpadea mientras revisamos la red
        self.poller = select.poll()
        self.poller.register(self.socket, select.POLLIN)

        print("Conectado al servidor")
        return self.enviar("HOLA;CAJERO")

    def enviar(self, mensaje):
        # Manda una linea al servidor. Devuelve True si se pudo enviar
        if self.socket is None:
            print(f"Sin conexion, no se envio: {mensaje}")
            return False
        try:
            self.socket.write(f"{mensaje}\n".encode())
            return True
        except OSError as error:
            print(f"Error al enviar ({error}), se cierra la conexion")
            self.cerrar()
            return False

    def revisar_mensajes(self):
        # Devuelve una lista con las lineas completas que llegaron (puede ser vacia).
        # Nunca se queda esperando: si no hay nada, regresa de inmediato.
        if self.socket is None:
            return []
        if not self.poller.poll(0):   # 0 ms = no esperar
            return []
        try:
            datos = self.socket.recv(256)
        except OSError as error:
            print(f"Error al recibir ({error}), se cierra la conexion")
            self.cerrar()
            return []
        if not datos:   # b"" significa que el servidor cerro la conexion
            print("El servidor cerro la conexion")
            self.cerrar()
            return []

        # Los datos pueden llegar partidos o con varias lineas juntas:
        # guardamos todo y sacamos solo las lineas que ya terminaron en \n
        self.pendiente = self.pendiente + datos
        lineas = []
        while b"\n" in self.pendiente:
            linea, self.pendiente = self.pendiente.split(b"\n", 1)
            lineas.append(linea.decode().strip())
        return lineas

    def cerrar(self):
        if self.socket is not None:
            try:
                self.socket.close()
            except OSError:
                pass
        self.socket = None
        self.poller = None
        self.pendiente = b""
