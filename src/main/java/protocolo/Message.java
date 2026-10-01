package protocolo;

import java.io.Serializable;

public sealed interface Message extends Serializable
        permits Message.Welcome,
        Message.PlayerMove,
        Message.GameState,
        Message.Ping,
        Message.Pong,
        Message.PlayerDisconnected,
        Message.Register,
        Message.Login,
        Message.CreateGame,
        Message.JoinGame,
        Message.Response,
        Message.TirarDadosPrueba,
        Message.ComprarPropiedad,
        Message.ComprarCasa,
        Message.TerminarTurno,
        Message.PagarMultaCarcel,
        Message.UsarCartaCarcel,
        Message.Dados,
        Message.Aviso,
        Message.EstadoJuego,
        Message.ConsultarTransacciones,
        Message.ExportarTransacciones,
        Message.HistorialTransacciones {

    // =========================================================
    // MENSAJES ORIGINALES
    // =========================================================

    record Welcome(int playerId) implements Message {}

    record PlayerMove(
            int playerId,
            double x,
            double y,
            String action
    ) implements Message {}

    record GameState(
            double p0x,
            double p0y,
            double p1x,
            double p1y,
            int score0,
            int score1
    ) implements Message {}

    record Ping(long timestamp) implements Message {}

    record Pong(long timestamp) implements Message {}

    record PlayerDisconnected(int playerId) implements Message {}


    // =========================================================
    // REGISTRO Y LOGIN
    // =========================================================

    record Register(
            String nombre
    ) implements Message {}

    record Login(
            String nombre
    ) implements Message {}


    // =========================================================
    // PARTIDAS
    // =========================================================

    record CreateGame(
            String jugador1,
            String jugador2
    ) implements Message {}

    record JoinGame(
            String codigo,
            String jugador1,
            String jugador2
    ) implements Message {}


    // =========================================================
    // RESPUESTA DEL SERVIDOR
    // =========================================================

    record Response(
            String tipo,
            String mensaje
    ) implements Message {}


    // =========================================================
    // PARTIDA: CLIENTE -> SERVIDOR (solo intenciones)
    // =========================================================
    // No llevan datos: el servidor sabe de qué computadora vienen,
    // revisa que le toque a un jugador de esa computadora y decide.

    /** Pide que el SERVIDOR tire dados al azar (para probar sin el cajero). */
    record TirarDadosPrueba() implements Message {}

    record ComprarPropiedad() implements Message {}

    record ComprarCasa(int idPropiedad) implements Message {}

    /** Datos oficiales de una propiedad; propietario es el índice del jugador o -1. */
    record EstadoPropiedad(int id, int propietario, int casas, boolean hotel,
            boolean hipotecada, int renta, int precioCasa, String motivoNoComprarCasa) implements Serializable {}

    record TerminarTurno() implements Message {}

    /** Estando preso y antes de tirar: pagar la multa para salir. */
    record PagarMultaCarcel() implements Message {}

    /** Estando preso y antes de tirar: usar una carta "Salga de la cárcel". */
    record UsarCartaCarcel() implements Message {}

    /** Filtros opcionales y orden; el servidor conserva y consulta el historial oficial. */
    record ConsultarTransacciones(String jugador, String tipo, boolean recientesPrimero) implements Message {}

    record ExportarTransacciones() implements Message {}

    /** Texto preparado por el servidor, para mostrar o guardar una copia en el cliente. */
    record HistorialTransacciones(String texto, String nombreArchivo, boolean exportacion) implements Message {}


    // =========================================================
    // PARTIDA: SERVIDOR -> TODOS LOS CLIENTES
    // =========================================================

    /** Se tiraron los dados (del cajero o de prueba). indiceJugador va de 0 a 3. */
    record Dados(
            int indiceJugador,
            int dado1,
            int dado2
    ) implements Message {}

    /** Una línea para el "Registro de Partida" (o un error solo para quien lo causó). */
    record Aviso(
            String texto
    ) implements Message {}

    /**
     * Foto completa de la partida. Los arreglos tienen una posición por
     * jugador (0 a 3). computadoras[i] dice desde qué computadora se juega
     * el jugador i, para que cada cliente sepa si el turno es suyo.
     * enCarcel[i] dice si el jugador i está preso y cartasCarcel[i] cuántas
     * cartas "Salga de la cárcel" tiene guardadas.
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
            int casasDisponibles
    ) implements Message {}
}
