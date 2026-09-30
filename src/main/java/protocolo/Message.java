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
        Message.TerminarTurno,
        Message.Dados,
        Message.Aviso,
        Message.EstadoJuego,
        Message.SolicitarRegistroTarjeta,
        Message.TarjetaRegistrada {

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

    record TerminarTurno() implements Message {}

    /** Indica que uno de los dos jugadores de esta computadora espera registrar RFID. */
    record SolicitarRegistroTarjeta(int indiceLocal) implements Message {}


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

    /** Confirmación del servidor de la tarjeta asignada al jugador local 0 o 1. */
    record TarjetaRegistrada(
            int indiceLocal,
            String uid,
            boolean correcta,
            String mensaje
    ) implements Message {}

    /**
     * Foto completa de la partida. Los arreglos tienen una posición por
     * jugador (0 a 3). computadoras[i] dice desde qué computadora se juega
     * el jugador i, para que cada cliente sepa si el turno es suyo.
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
            boolean terminado
    ) implements Message {}
}