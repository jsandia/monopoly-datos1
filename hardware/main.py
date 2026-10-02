# Dados electronicos + lector RFID RC522 + conexion con el servidor
# Display 7 segmentos doble de CATODO COMUN + boton + RC522
# Placa: Raspberry Pi Pico 2 W con MicroPython
#
# Nucleo 0 (bucle principal): display, boton, dados y red
# Nucleo 1 (hilo aparte):     lector RC522
#
# Conexiones:
#   GP0-GP6 -> segmentos a, b, c, d, e, f, g (resistencias de 1 kOhm)
#   GP7     -> catodo del digito 1
#   GP8     -> catodo del digito 2
#   GP15    -> boton (la otra pata a GND)
#   RC522:  SCK -> GP18, MISO -> GP16, MOSI -> GP19,
#           SDA (CS) -> GP17, RST -> GP22, 3.3V -> 3V3, GND -> GND
#
# IMPORTANTE: mfrc522.py y cajero_red.py tienen que estar guardados en la Pico.

from cajero_red import ConexionCajero, uid_a_texto
from machine import Pin
from mfrc522 import MFRC522
import _thread
import random
import time

# 1) Numeros de GPIO del display y del boton
PINES_SEGMENTOS = [0, 1, 2, 3, 4, 5, 6]   # a, b, c, d, e, f, g
PINES_CATODOS = [7, 8]                     # digito 1, digito 2
PIN_BOTON = 15

# 2) Numeros de GPIO del lector RC522
LECTOR_SCK = 18
LECTOR_MISO = 16
LECTOR_MOSI = 19
LECTOR_CS = 17
LECTOR_RST = 22

# 3) Valores logicos para catodo comun
SEGMENTO_ENCENDIDO = 1
SEGMENTO_APAGADO = 0
CATODO_ACTIVO = 0
CATODO_INACTIVO = 1

# 4) Tiempos (en milisegundos)
TIEMPO_POR_DIGITO_MS = 5
TIEMPO_ANTIRREBOTE_MS = 50
DURACION_TIRADA_MS = 800
VELOCIDAD_GIRO_MS = 60
INTERVALO_LECTURA_MS = 200         # pausa del hilo del lector entre lecturas
LECTURAS_VACIAS_PARA_OLVIDAR = 2

# 5) Tabla de numeros. Orden: (a, b, c, d, e, f, g)
DIGITOS = {
    1: (0, 1, 1, 0, 0, 0, 0),
    2: (1, 1, 0, 1, 1, 0, 1),
    3: (1, 1, 1, 1, 0, 0, 1),
    4: (0, 1, 1, 0, 0, 1, 1),
    5: (1, 0, 1, 1, 0, 1, 1),
    6: (1, 0, 1, 1, 1, 1, 1),
    "-": (0, 0, 0, 0, 0, 0, 1),
}

# 6) Pines del display
segmentos = []
for numero in PINES_SEGMENTOS:
    segmentos.append(Pin(numero, Pin.OUT, value=SEGMENTO_APAGADO))

catodos = []
for numero in PINES_CATODOS:
    catodos.append(Pin(numero, Pin.OUT, value=CATODO_INACTIVO))

# 7) Boton y LED integrado
boton = Pin(PIN_BOTON, Pin.IN, Pin.PULL_UP)
led = Pin("LED", Pin.OUT)

# 8) Lector RC522
lector = MFRC522(spi_id=0, sck=LECTOR_SCK, miso=LECTOR_MISO,
                 mosi=LECTOR_MOSI, cs=LECTOR_CS, rst=LECTOR_RST)

# 9) Variables compartidas entre los dos nucleos
tarjeta_nueva = None               # el hilo pone aqui el UID de una tarjeta nueva
candado = _thread.allocate_lock()  # evita que ambos nucleos la usen a la vez


# 10) Funciones del display
def apagar_digitos():
    for catodo in catodos:
        catodo.value(CATODO_INACTIVO)


def escribir_segmentos(valor):
    patron = DIGITOS[valor]
    for i in range(7):
        if patron[i] == 1:
            segmentos[i].value(SEGMENTO_ENCENDIDO)
        else:
            segmentos[i].value(SEGMENTO_APAGADO)


def mostrar(valores):
    for i in range(2):
        apagar_digitos()
        escribir_segmentos(valores[i])
        catodos[i].value(CATODO_ACTIVO)
        time.sleep_ms(TIEMPO_POR_DIGITO_MS)
    apagar_digitos()


# 11) Funcion para leer una tarjeta
def leer_tarjeta():
    # Devuelve el UID como texto hexadecimal (ej: "A3F29B11") o None
    lector.init()
    (estado, tipo_tarjeta) = lector.request(lector.REQIDL)
    if estado == lector.OK:
        (estado, uid) = lector.SelectTagSN()
        if estado == lector.OK:
            return uid_a_texto(uid)
    return None


# 12) Hilo que corre en el NUCLEO 1: solo lee tarjetas
def hilo_lector():
    global tarjeta_nueva
    ultimo_uid = None
    lecturas_vacias = 0

    while True:
        uid_actual = leer_tarjeta()   # puede tardar, pero aqui no molesta

        if uid_actual is None:
            lecturas_vacias = lecturas_vacias + 1
            if lecturas_vacias >= LECTURAS_VACIAS_PARA_OLVIDAR:
                ultimo_uid = None
        else:
            lecturas_vacias = 0
            if uid_actual != ultimo_uid:
                ultimo_uid = uid_actual
                with candado:
                    tarjeta_nueva = uid_actual

        # En este nucleo SI podemos dormir: no afecta al display
        time.sleep_ms(INTERVALO_LECTURA_MS)


# 13) Conexion con el servidor (Wi-Fi + TCP), antes de arrancar el hilo.
# Toda la red se maneja desde el nucleo 0.
cajero = ConexionCajero()
if not cajero.conectar():
    print("Se sigue sin servidor: los dados funcionan pero no se envia nada")

# 14) Arrancamos el hilo del lector en el nucleo 1
_thread.start_new_thread(hilo_lector, ())

# 15) Variables de estado de los dados
valores = ["-", "-"]
estado_anterior = 1
ultimo_cambio_boton = 0
tirando = False
inicio_tirada = 0
ultimo_giro = 0

print("Dados listos. Presiona el boton para tirar o acerca una tarjeta.")

# 16) Bucle principal en el NUCLEO 0: display, boton, dados y red
while True:
    # a) Revisar si el servidor mando algo (no se queda esperando)
    for mensaje in cajero.revisar_mensajes():
        print(f"Servidor dice: {mensaje}")

    # b) Refrescar el display SIEMPRE
    mostrar(valores)

    ahora = time.ticks_ms()

    # c) Leer el boton con antirrebote
    estado_actual = boton.value()
    if estado_actual != estado_anterior:
        tiempo_desde_cambio = time.ticks_diff(ahora, ultimo_cambio_boton)
        if estado_actual == 0 and tiempo_desde_cambio > TIEMPO_ANTIRREBOTE_MS and not tirando:
            print("Boton presionado")
            tirando = True
            inicio_tirada = ahora
            led.on()
        ultimo_cambio_boton = ahora
        estado_anterior = estado_actual

    # d) Animacion y resultado de los dados
    if tirando:
        if time.ticks_diff(ahora, ultimo_giro) > VELOCIDAD_GIRO_MS:
            valores = [random.randint(1, 6), random.randint(1, 6)]
            ultimo_giro = ahora

        if time.ticks_diff(ahora, inicio_tirada) > DURACION_TIRADA_MS:
            dado1 = random.randint(1, 6)
            dado2 = random.randint(1, 6)
            valores = [dado1, dado2]
            print(f"Dado 1: {dado1} | Dado 2: {dado2} | Suma: {dado1 + dado2}")
            led.off()
            tirando = False
            cajero.enviar(f"DADOS;{dado1};{dado2}")

    # e) Revisar si el nucleo 1 encontro una tarjeta nueva
    uid_para_mostrar = None
    with candado:
        if tarjeta_nueva is not None:
            uid_para_mostrar = tarjeta_nueva
            tarjeta_nueva = None

    if uid_para_mostrar is not None:
        print(f"Tarjeta detectada! UID: {uid_para_mostrar}")
        cajero.enviar(f"TARJETA;{uid_para_mostrar}")
