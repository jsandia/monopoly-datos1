package config;

/**
 * Todos los valores ajustables del juego en un solo lugar.
 *
 * Se usan sin crear objetos, directo con el nombre de la clase:
 * {@code Configuracion.MULTA_CARCEL}. Eso es lo que hace "static": la
 * constante pertenece a la clase, no a cada objeto. Y "final" significa
 * que no se puede cambiar mientras el programa corre.
 *
 * Está en su propio paquete porque la usan el modelo, el cliente y el
 * servidor (que está en el paquete por defecto, y Java no deja importar
 * clases del paquete por defecto desde otros paquetes).
 *
 * Lo que NO está aquí, porque son datos y ya tienen su lugar:
 * las 40 casillas (Tablero), las tablas de alquiler (Propiedad)
 * y los textos de las cartas (CartaEvento).
 */
public final class Configuracion {

    // Constructor privado: nadie puede hacer "new Configuracion()"
    private Configuracion() {
    }

    // =========================================================
    // PARTIDA
    // =========================================================

    /** Jugadores por partida (2 por computadora). */
    public static final int CANTIDAD_JUGADORES = 4;

    /** Al pasar este número de turnos la partida termina. */
    public static final int MAX_TURNOS = 100;

    // =========================================================
    // DINERO (montos del Monopoly original)
    // =========================================================

    public static final int SALDO_INICIAL = 1500;

    /** Lo que cobra un jugador cada vez que pasa por Salida. */
    public static final int PREMIO_SALIDA = 200;

    // =========================================================
    // TABLERO Y CÁRCEL
    // =========================================================

    /** Casillas del tablero clásico. */
    public static final int CASILLAS_TABLERO = 40;

    /** Índice de la casilla "Cárcel / De visita". */
    public static final int POSICION_CARCEL = 10;

    /** Lo que se paga para salir de la cárcel. */
    public static final int MULTA_CARCEL = 50;

    /** Al tercer intento sin dobles se paga la multa obligatoriamente. */
    public static final int MAX_INTENTOS_CARCEL = 3;

    // =========================================================
    // BANCO Y CONSTRUCCIONES
    // =========================================================

    public static final int SALDO_BANCO = 1_000_000;
    public static final int CASAS_BANCO = 32;
    public static final int HOTELES_BANCO = 12;

    /** Casas por propiedad antes de poder cambiarlas por un hotel. */
    public static final int MAX_CASAS_POR_PROPIEDAD = 4;

    /** Interés que se paga al quitar una hipoteca (10 = 10%). */
    public static final int INTERES_HIPOTECA_PORCENTAJE = 10;

    // =========================================================
    // RED
    // =========================================================

    /** Puerto TCP donde el servidor recibe a las computadoras. No usar 5000: en macOS lo ocupa AirPlay. */
    public static final int PUERTO_JUEGO = 5100;

    /** Puerto TCP donde el servidor recibe al cajero (Pico 2W). */
    public static final int PUERTO_CAJERO = 5050;

    /** Puerto UDP para que los clientes encuentren al servidor en la red. */
    public static final int PUERTO_DESCUBRIMIENTO = 5001;

    /** Puerto de PruebaConexion. Distinto de todos los anteriores para no chocar. */
    public static final int PUERTO_PRUEBA_CONEXION = 5060;

    /** Cuánto espera un cliente buscando al servidor antes de rendirse. */
    public static final int TIEMPO_DESCUBRIMIENTO_MS = 5000;

    // =========================================================
    // ARCHIVOS
    // =========================================================

    /** Carpeta donde se guarda el historial de transacciones de cada partida. */
    public static final String CARPETA_TRANSACCIONES = "transacciones";

    // =========================================================
    // ANIMACIONES DEL CLIENTE
    // =========================================================

    /** Cuánto giran los dados antes de mostrar el número final. */
    public static final int DURACION_DADOS_MS = 1200;

    /** Cada cuánto cambia la cara de los dados mientras giran. */
    public static final int INTERVALO_DADOS_MS = 80;

    /** Lo que tarda una ficha en saltar de una casilla a la siguiente. */
    public static final int PASO_FICHA_MS = 230;

    /** Más pasos que esto no se animan: es un salto (por ejemplo, "Vaya a la cárcel"). */
    public static final int MAX_PASOS_ANIMADOS = 12;
}
