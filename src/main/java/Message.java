import java.io.Serializable;

/**
 * Protocolo de mensajes del juego.
 * Cada tipo de mensaje es un "record": inmutable, liviano y fácil de
 * extender. Al ser un "sealed interface", el compilador te obliga a
 * manejar todos los casos posibles en el switch (evita bugs por olvido).
 *
 * Usa ObjectOutputStream/ObjectInputStream para enviarse por el socket,
 * así que todo tiene que ser Serializable.
 */
public sealed interface Message extends Serializable {

    // El servidor le asigna un id de jugador (0 o 1) al conectarse
    record Welcome(int playerId) implements Message {}

    // Un jugador envía su nueva posición / acción
    record PlayerMove(int playerId, double x, double y, String action) implements Message {}

    // El servidor retransmite el estado completo a ambos clientes
    record GameState(double p0x, double p0y, double p1x, double p1y, int score0, int score1) implements Message {}

    // Latido para detectar desconexiones (opcional, útil por wifi inestable)
    record Ping(long timestamp) implements Message {}

    // Respuesta a un Ping, para medir el tiempo de ida y vuelta (RTT)
    record Pong(long timestamp) implements Message {}

    // Un jugador se desconectó
    record PlayerDisconnected(int playerId) implements Message {}
}
