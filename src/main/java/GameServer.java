import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Servidor del juego. Se corre en UNA sola de las dos computadoras
 * (la que va a hacer de "host"). La otra computadora se conecta a
 * esta con GameClient usando la IP de esta máquina.
 *
 * Cuando uses la Raspberry Pi Pico W como punto de acceso WiFi:
 * las dos PCs se conectan a la red que emite la Pico, y esta
 * computadora sigue siendo el host del juego (la Pico solo da la red,
 * no corre la lógica del juego, a menos que luego decidas portar
 * esto a MicroPython/C con sockets).
 */
public class GameServer {

    private static final int DEFAULT_PORT = 5000;
    private final int port;

    // Estado compartido del juego (protegido porque 2 hilos lo tocan a la vez)
    private volatile double p0x = 0, p0y = 0;
    private volatile double p1x = 0, p1y = 0;
    private volatile int score0 = 0, score1 = 0;

    private final ObjectOutputStream[] outputs = new ObjectOutputStream[2];
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    public GameServer() {
        this(DEFAULT_PORT);
    }

    public GameServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        new GameServer().start();
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor esperando jugadores en el puerto " + port);

            // Se anuncia en la red para que los clientes lo encuentren solos,
            // sin que nadie tenga que escribir la IP a mano.
            Discovery.hostAnnounce(port);

            for (int playerId = 0; playerId < 2; playerId++) {
                Socket socket = serverSocket.accept(); // espera a que se conecte un jugador
                System.out.println("Jugador " + playerId + " conectado desde " + socket.getInetAddress());
                handleNewPlayer(socket, playerId);
            }

            System.out.println("Los 2 jugadores están conectados. Partida en curso...");
            // el servidor sigue vivo mientras los hilos de cada jugador corren
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.HOURS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleNewPlayer(Socket socket, int playerId) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        outputs[playerId] = out;

        out.writeObject(new Message.Welcome(playerId));
        out.flush();

        pool.submit(() -> playerLoop(socket, in, playerId));
    }

    private void playerLoop(Socket socket, ObjectInputStream in, int playerId) {
        try {
            while (true) {
                Message msg = (Message) in.readObject();

                switch (msg) {
                    case Message.PlayerMove move -> {
                        if (playerId == 0) { p0x = move.x(); p0y = move.y(); }
                        else               { p1x = move.x(); p1y = move.y(); }
                        broadcastState();
                    }
                    case Message.Ping ping -> { /* podés responder con otro Ping si querés medir latencia */ }
                    default -> { /* Welcome, GameState, PlayerDisconnected no los manda el cliente */ }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Jugador " + playerId + " se desconectó");
            outputs[playerId] = null; // evita seguir intentando escribirle a un socket cerrado
            broadcastDisconnect(playerId);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private synchronized void broadcastState() {
        Message.GameState state = new Message.GameState(p0x, p0y, p1x, p1y, score0, score1);
        for (ObjectOutputStream out : outputs) {
            if (out == null) continue;
            try {
                out.reset(); // evita que ObjectOutputStream cachee objetos viejos
                out.writeObject(state);
                out.flush();
            } catch (IOException e) {
                // ese jugador ya no está disponible, se ignora el error acá
            }
        }
    }

    private synchronized void broadcastDisconnect(int playerId) {
        for (ObjectOutputStream out : outputs) {
            if (out == null) continue;
            try {
                out.writeObject(new Message.PlayerDisconnected(playerId));
                out.flush();
            } catch (IOException ignored) {}
        }
    }
}
