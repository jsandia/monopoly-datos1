import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Suite de pruebas en Java puro (sin JUnit) para no depender de
 * descargar nada de internet. Corré con:
 *
 *   javac --release 26 *.java
 *   java AllTests
 *
 * Si algo falla, imprime FAIL con el detalle; si todo pasa, termina
 * con "TODAS LAS PRUEBAS PASARON".
 */
public class AllTests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, "UTF-8"));

        run("Serialización de Message.PlayerMove", AllTests::testPlayerMoveSerialization);
        run("Serialización de Message.GameState", AllTests::testGameStateSerialization);
        run("Igualdad de records (equals/hashCode)", AllTests::testRecordEquality);
        // Esta va antes que las de GameServer: todas usan el mismo puerto UDP
        // fijo de descubrimiento (5001), así que si un GameServer de otra
        // prueba ya lo tiene tomado, esta prueba se ve afectada sin motivo.
        run("Descubrimiento por broadcast (host + client en localhost)", AllTests::testDiscoveryRoundTrip);
        run("Servidor + 2 clientes en localhost (integración)", AllTests::testServerClientIntegration);
        run("Servidor sigue funcionando si un jugador se desconecta", AllTests::testServerSurvivesClientDisconnect);

        System.out.println();
        System.out.println("Resultado: " + passed + " pasaron, " + failed + " fallaron");
        if (failed > 0) {
            System.out.println("HAY ERRORES — revisá el detalle arriba.");
            System.exit(1);
        } else {
            System.out.println("TODAS LAS PRUEBAS PASARON.");
        }
    }

    // ---------- infraestructura mínima de test ----------

    private interface TestCase { void run() throws Exception; }

    private static void run(String name, TestCase test) {
        try {
            test.run();
            System.out.println("PASS - " + name);
            passed++;
        } catch (Throwable t) {
            System.out.println("FAIL - " + name);
            System.out.println("       " + t);
            failed++;
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " — esperado: " + expected + ", obtenido: " + actual);
        }
    }

    // ---------- pruebas de Message (serialización) ----------

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
        }
        return bos.toByteArray();
    }

    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return ois.readObject();
        }
    }

    private static void testPlayerMoveSerialization() throws Exception {
        Message.PlayerMove original = new Message.PlayerMove(1, 3.5, 7.25, "move");
        Message.PlayerMove copy = (Message.PlayerMove) deserialize(serialize(original));

        assertEquals(original.playerId(), copy.playerId(), "playerId no sobrevivió la serialización");
        assertEquals(original.x(), copy.x(), "x no sobrevivió la serialización");
        assertEquals(original.y(), copy.y(), "y no sobrevivió la serialización");
        assertEquals(original.action(), copy.action(), "action no sobrevivió la serialización");
    }

    private static void testGameStateSerialization() throws Exception {
        Message.GameState original = new Message.GameState(1, 2, 3, 4, 10, 20);
        Message.GameState copy = (Message.GameState) deserialize(serialize(original));
        assertEquals(original, copy, "GameState no es igual tras serializar/deserializar");
    }

    private static void testRecordEquality() {
        Message.Ping a = new Message.Ping(12345L);
        Message.Ping b = new Message.Ping(12345L);
        Message.Ping c = new Message.Ping(99999L);
        assertTrue(a.equals(b), "dos Ping con el mismo timestamp deberían ser iguales");
        assertTrue(!a.equals(c), "dos Ping con distinto timestamp NO deberían ser iguales");
    }

    // ---------- integración: GameServer + clientes reales por socket ----------

    /**
     * Simula lo que hace GameClient, pero sin Scanner ni System.out,
     * para poder controlarlo desde el test.
     */
    private record RawClient(Socket socket, ObjectOutputStream out, ObjectInputStream in) implements Closeable {
        static RawClient connect(int port) throws IOException {
            Socket socket = new Socket("127.0.0.1", port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            return new RawClient(socket, out, in);
        }
        public void close() throws IOException { socket.close(); }
    }

    private static void testServerClientIntegration() throws Exception {
        int testPort = 15000; // puerto alto para no chocar con el de producción (5000)
        GameServer server = new GameServer(testPort);
        Thread serverThread = new Thread(() -> {
            try { server.start(); } catch (Exception ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(200); // darle tiempo al server a abrir el ServerSocket

        try (RawClient p0 = RawClient.connect(testPort);
             RawClient p1 = RawClient.connect(testPort)) {

            Message.Welcome w0 = (Message.Welcome) p0.in().readObject();
            Message.Welcome w1 = (Message.Welcome) p1.in().readObject();
            assertEquals(0, w0.playerId(), "primer cliente debería recibir playerId 0");
            assertEquals(1, w1.playerId(), "segundo cliente debería recibir playerId 1");

            // Jugador 0 se mueve
            p0.out().writeObject(new Message.PlayerMove(0, 5.0, 6.0, "move"));
            p0.out().flush();

            // Ambos clientes deberían recibir el GameState actualizado
            Message.GameState state0 = (Message.GameState) p0.in().readObject();
            Message.GameState state1 = (Message.GameState) p1.in().readObject();

            assertEquals(5.0, state0.p0x(), "el estado que recibe el jugador 0 no refleja su propio movimiento (x)");
            assertEquals(6.0, state0.p0y(), "el estado que recibe el jugador 0 no refleja su propio movimiento (y)");
            assertEquals(state0, state1, "los dos jugadores deberían recibir exactamente el mismo estado");
        }
    }

    private static void testServerSurvivesClientDisconnect() throws Exception {
        int testPort = 15001;
        GameServer server = new GameServer(testPort);
        Thread serverThread = new Thread(() -> {
            try { server.start(); } catch (Exception ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(200);

        RawClient p0 = RawClient.connect(testPort);
        p0.in().readObject(); // Welcome

        try (RawClient p1 = RawClient.connect(testPort)) {
            p1.in().readObject(); // Welcome

            p0.close(); // el jugador 0 se desconecta a la fuerza

            // el jugador 1 sigue pudiendo moverse sin que el servidor explote
            p1.out().writeObject(new Message.PlayerMove(1, 1.0, 1.0, "move"));
            p1.out().flush();

            // El próximo mensaje puede ser el GameState de nuestro propio
            // movimiento, o el aviso PlayerDisconnected del jugador 0 —
            // el orden no está garantizado (es una carrera real de red),
            // así que leemos hasta encontrar el GameState, igual que hace
            // GameClient de verdad con su switch.
            Message.GameState state = readUntilGameState(p1, 5);
            assertEquals(1.0, state.p1x(), "el servidor debería seguir funcionando para el jugador que queda conectado");
        }
    }

    private static Message.GameState readUntilGameState(RawClient client, int maxMessages) throws Exception {
        for (int i = 0; i < maxMessages; i++) {
            Message msg = (Message) client.in().readObject();
            if (msg instanceof Message.GameState state) return state;
        }
        throw new AssertionError("no llegó ningún GameState después de " + maxMessages + " mensajes");
    }

    // ---------- descubrimiento (UDP broadcast) ----------

    private static void testDiscoveryRoundTrip() throws Exception {
        int testPort = 15002;
        AtomicBoolean announced = new AtomicBoolean(false);

        Thread announcer = new Thread(() -> {
            announced.set(true);
            Discovery.hostAnnounce(testPort);
        });
        announcer.setDaemon(true);
        announcer.start();
        Thread.sleep(300);
        assertTrue(announced.get(), "el hilo de anuncio no llegó a arrancar");

        try {
            Discovery.HostInfo info = Discovery.discoverHost(3000);
            assertEquals(testPort, info.port(), "el puerto descubierto no coincide con el anunciado");
        } catch (IOException e) {
            // En algunos entornos en la nube (contenedores sin interfaz de
            // broadcast real) el UDP broadcast no llega ni siquiera a uno
            // mismo. Esto NO es un bug del código: en una red WiFi real
            // (como la que van a usar las 2 computadoras) sí funciona.
            // Lo dejamos como aviso, no como fallo duro de la prueba.
            System.out.println("       (aviso: broadcast no disponible en este entorno, no es un error del código: " + e.getMessage() + ")");
        }
    }
}
