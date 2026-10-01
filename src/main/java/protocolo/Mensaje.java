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
        Mensaje.PlayerMove,
        Mensaje.Ping,
        Mensaje.Pong,
        Mensaje.Registrarse,
        Mensaje.IniciarSesion,
        Mensaje.CrearPartida,
        Mensaje.UnirsePartida,
        Mensaje.Respuesta,
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
    // CONEXIÓN
    // =========================================================

    record Bienvenida(int computadora) implements Mensaje {}

    record PlayerMove(
            int computadora,
            double x,
            double y,
            String action
    ) implements Mensaje {}

    record Ping(long enviadoMs) implements Mensaje {}

    record Pong(long enviadoMs) implements Mensaje {}


    // =========================================================
    // REGISTRO E INICIO DE SESIÓN
    // =========================================================

    record Registrarse(
            String nombre
    ) implements Mensaje {}

    record IniciarSesion(
            String nombre
    ) implements Mensaje {}


    // =========================================================
    // PARTIDAS
    // =========================================================

    record CrearPartida(
            String jugador1,
            String jugador2
    ) implements Mensaje {}

    record UnirsePartida(
            String codigo,
            String jugador1,
            String jugador2
    ) implements Mensaje {}


    // =========================================================
    // RESPUESTA DEL SERVIDOR
    // =========================================================

    record Respuesta(
            String tipo,
            String mensaje
    ) implements Mensaje {}


    // =========================================================
    // PARTIDA: CLIENTE -> SERVIDOR (solo intenciones)
    // =========================================================
    // No llevan datos: el servidor sabe de qué computadora vienen,
    // revisa que le toque a un jugador de esa computadora y decide.

    /** Pide que el SERVIDOR tire dados al azar (para probar sin el cajero). */
    record TirarDadosPrueba() implements Mensaje {}

    record ComprarPropiedad() implements Mensaje {}

    record ComprarCasa(int idPropiedad) implements Mensaje {}

    record ComprarHotel(int idPropiedad) implements Mensaje {}

    /** Datos oficiales de una propiedad; propietario es el índice del jugador o -1. */
    record EstadoPropiedad(int id, int propietario, int casas, boolean hotel,
            boolean hipotecada, int renta, int precioCasa, String motivoNoComprarCasa,
            int precioHotel, String motivoNoComprarHotel) implements Serializable {}

    record TerminarTurno() implements Mensaje {}

    /** Estando preso y antes de tirar: pagar la multa para salir. */
    record PagarMultaCarcel() implements Mensaje {}

    /** Estando preso y antes de tirar: usar una carta "Salga de la cárcel". */
    record UsarCartaCarcel() implements Mensaje {}

    /** Filtros opcionales y orden; el servidor conserva y consulta el historial oficial. */
    record ConsultarTransacciones(String jugador, String tipo, boolean recientesPrimero) implements Mensaje {}

    record ExportarTransacciones() implements Mensaje {}

    /** Texto preparado por el servidor, para mostrar o guardar una copia en el cliente. */
    record HistorialTransacciones(String texto, String nombreArchivo, boolean exportacion) implements Mensaje {}


    // =========================================================
    // PARTIDA: SERVIDOR -> TODOS LOS CLIENTES
    // =========================================================

    /** Se tiraron los dados (del cajero o de prueba). indiceJugador va de 0 a 3. */
    record Dados(
            int indiceJugador,
            int dado1,
            int dado2
    ) implements Mensaje {}

    /** Una línea para el "Registro de Partida" (o un error solo para quien lo causó). */
    record Aviso(
            String texto
    ) implements Mensaje {}

    /**
     * Foto completa de la partida. Los arreglos tienen una posición por
     * jugador (0 a 3). computadoras[i] dice desde qué computadora se juega
     * el jugador i, para que cada cliente sepa si el turno es suyo.
     * enCarcel[i] dice si el jugador i está preso y cartasCarcel[i] cuántas
     * cartas "Salga de la cárcel" tiene guardadas. activos[i] es false si
     * quedó en bancarrota, y patrimonios[i] es su saldo + valor de propiedades.
     * Al terminar, ganador es el índice del ganador (-1 mientras se juega) y
     * empate dice si ganó por desempate.
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
            boolean empate
    ) implements Mensaje {}
}
