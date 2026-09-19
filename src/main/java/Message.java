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
        Message.Response {

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
}