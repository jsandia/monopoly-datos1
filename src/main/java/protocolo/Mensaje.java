package protocolo;

import java.io.Serializable;

/**
 * Protocolo entre las computadoras (clientes Swing) y el servidor.
 *
 * Cada mensaje es un record que viaja por el socket TCP con
 * ObjectOutputStream / ObjectInputStream. Por eso extiende Serializable.
 *
 * "sealed ... permits" significa que solo los records de la lista pueden
 * ser un Mensaje: nadie puede inventar uno nuevo desde otro archivo.
 *
 * Los clientes solo mandan intenciones (tirar, comprar, terminar turno);
 * el servidor valida, cambia el estado y responde a todos con Aviso y
 * EstadoJuego.
 *
 * Ojo: como viaja el nombre de cada clase, las dos computadoras tienen
 * que correr la misma versión del programa.
 */
public sealed interface Mensaje extends Serializable
        permits Mensaje.Bienvenida,
        Mensaje.Registrarse,
        Mensaje.CrearPartida,
        Mensaje.UnirsePartida,
        Mensaje.Respuesta,
        Mensaje.RegistrarTarjeta,
        Mensaje.ResultadoTarjeta,
        Mensaje.TirarDadosPrueba,
        Mensaje.ComprarPropiedad,
        Mensaje.ComprarCasa,
        Mensaje.ComprarHotel,
        Mensaje.TerminarTurno,
        Mensaje.PagarMultaCarcel,
        Mensaje.UsarCartaCarcel,
        Mensaje.Dados,
        Mensaje.Aviso,
        Mensaje.EstadoJuego,
        Mensaje.ConsultarTransacciones,
        Mensaje.ExportarTransacciones,
        Mensaje.HistorialTransacciones {

    // =========================================================
    // ANTES DE JUGAR: CONEXIÓN, USUARIOS Y PARTIDAS
    // =========================================================

    /** Servidor → cliente al conectarse: qué número de computadora le tocó (0 o 1). */
    record Bienvenida(int computadora) implements Mensaje {}

    record Registrarse(String nombre) implements Mensaje {}

    /** La computadora que crea la partida pone a los jugadores 1 y 2. */
    record CrearPartida(String jugador1, String jugador2) implements Mensaje {}

    /** La otra computadora se une con el código y pone a los jugadores 3 y 4. */
    record UnirsePartida(String codigo, String jugador1, String jugador2) implements Mensaje {}

    /**
     * Servidor → cliente: respuesta a Registrarse, CrearPartida o UnirsePartida.
     * tipo es una de las constantes de abajo; al crear la partida, mensaje es el código.
     */
    record Respuesta(String tipo, String mensaje) implements Mensaje {
        public static final String ERROR = "ERROR";
        public static final String REGISTRO_OK = "REGISTRO_OK";
        public static final String USUARIO_EXISTE = "USUARIO_EXISTE";
        public static final String USUARIO_NO_EXISTE = "USUARIO_NO_EXISTE";
        public static final String PARTIDA_CREADA = "PARTIDA_CREADA";
        public static final String PARTIDA_NO_EXISTE = "PARTIDA_NO_EXISTE";
        public static final String PARTIDA_LLENA = "PARTIDA_LLENA";
        public static final String PARTIDA_UNIDA = "PARTIDA_UNIDA";
    }

    /**
     * Cliente → servidor: "el próximo UID que lea el cajero es de mi
     * jugador local N" (0 = el primero de esta computadora, 1 = el segundo).
     */
    record RegistrarTarjeta(int jugadorLocal) implements Mensaje {}

    /**
     * Servidor → la computadora que pidió el registro.
     * registrada = true: la tarjeta quedó asociada al jugador.
     * registrada = false con uid: se leyó una tarjeta que no sirve
     *   (por ejemplo, ya es de otro jugador); el servidor sigue esperando otra.
     * registrada = false con uid null: la solicitud misma no se pudo
     *   atender (por ejemplo, no hay partida); no hay nada que esperar.
     */
    record ResultadoTarjeta(int jugadorLocal, boolean registrada, String uid, String mensaje)
            implements Mensaje {}

    // =========================================================
    // PARTIDA: CLIENTE → SERVIDOR (solo intenciones)
    // =========================================================
    // No llevan quién las manda: el servidor sabe de qué computadora vienen,
    // revisa que le toque a un jugador de esa computadora y decide.

    /** Pide que el servidor tire dados al azar (para probar sin el cajero). */
    record TirarDadosPrueba() implements Mensaje {}

    record ComprarPropiedad() implements Mensaje {}

    record ComprarCasa(int idPropiedad) implements Mensaje {}

    record ComprarHotel(int idPropiedad) implements Mensaje {}

    record TerminarTurno() implements Mensaje {}

    /** Estando preso y antes de tirar: pagar la multa para salir. */
    record PagarMultaCarcel() implements Mensaje {}

    /** Estando preso y antes de tirar: usar una carta "Salga de la cárcel". */
    record UsarCartaCarcel() implements Mensaje {}

    /** Filtros opcionales (null = todos) y orden del historial. */
    record ConsultarTransacciones(String jugador, String tipo, boolean recientesPrimero) implements Mensaje {}

    record ExportarTransacciones() implements Mensaje {}

    // =========================================================
    // PARTIDA: SERVIDOR → CLIENTES
    // =========================================================

    /** Se tiraron los dados (del cajero o de prueba). indiceJugador va de 0 a 3. */
    record Dados(int indiceJugador, int dado1, int dado2) implements Mensaje {}

    /** Una línea para el "Registro de Partida" (o un error solo para quien lo causó). */
    record Aviso(String texto) implements Mensaje {}

    /** Historial preparado por el servidor, para mostrarlo o guardarlo como TXT. */
    record HistorialTransacciones(String texto, String nombreArchivo, boolean exportacion) implements Mensaje {}

    /** Datos oficiales de una propiedad; propietario es el índice del jugador o -1. */
    record EstadoPropiedad(int id, int propietario, int casas, boolean hotel,
            int renta, int precioCasa, String motivoNoComprarCasa,
            int precioHotel, String motivoNoComprarHotel) implements Serializable {}

    /**
     * Foto completa de la partida. Los arreglos tienen una posición por
     * jugador (0 a 3). computadoras[i] dice desde qué computadora se juega
     * el jugador i, para que cada cliente sepa si el turno es suyo.
     * enCarcel[i] dice si el jugador i está preso y cartasCarcel[i] cuántas
     * cartas "Salga de la cárcel" tiene guardadas. activos[i] es false si
     * quedó en bancarrota, y patrimonios[i] es su saldo + valor de propiedades.
     * Al terminar, ganador es el índice del ganador (-1 mientras se juega) y
     * empate dice si ganó por desempate. pagoPendiente describe el alquiler
     * que el jugador en turno debe pagar con su tarjeta (null si no debe nada).
     */
    record EstadoJuego(
            int turnoGlobal,
            int maxTurnos,
            int enTurno,
            String[] nombres,
            int[] saldos,
            int[] posiciones,
            int[] computadoras,
            boolean yaTiro,
            boolean puedeComprar,
            boolean terminado,
            boolean[] enCarcel,
            int[] cartasCarcel,
            EstadoPropiedad[] propiedades,
            int casasDisponibles,
            int hotelesDisponibles,
            boolean[] activos,
            int[] patrimonios,
            int ganador,
            boolean empate,
            String pagoPendiente
    ) implements Mensaje {}
}
