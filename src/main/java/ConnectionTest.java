import java.io.*;
import java.net.*;

/**
 * Prueba de conexión independiente del juego. Sirve para confirmar,
 * antes de meterte con la lógica del juego, que las 2 computadoras
 * se encuentran solas en la red y que la conexión funciona bien.
 *
 * Uso:
 *   En UNA computadora:  java ConnectionTest host
 *   En la OTRA:          java ConnectionTest client
 *
 * El cliente descubre al host solo (mismo mecanismo de Discovery.java
 * que usa el juego), se conecta, y hace 5 "pings" midiendo cuánto tarda
 * cada ida y vuelta. Si ves los 5 tiempos y el promedio final, la
 * conexión está funcionando correctamente.
 */
public class ConnectionTest {

    private static final int TEST_PORT = 5050; // puerto distinto al del juego (5000), para no chocar

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        if (args.length == 0) {
            System.out.println("Uso:");
            System.out.println("  java ConnectionTest host    <- corré esto en UNA computadora");
            System.out.println("  java ConnectionTest client  <- corré esto en la OTRA");
            return;
        }

        switch (args[0]) {
            case "host"   -> runHost();
            case "client" -> runClient();
            default       -> System.out.println("Argumento inválido. Usá 'host' o 'client'.");
        }
    }

    private static void runHost() throws IOException, ClassNotFoundException {
        try (ServerSocket serverSocket = new ServerSocket(TEST_PORT)) {
            System.out.println("Anunciándose en la red y esperando a la otra computadora...");
            Discovery.hostAnnounce(TEST_PORT);

            try (Socket socket = serverSocket.accept()) {
                System.out.println("¡Conexión recibida desde " + socket.getInetAddress() + "!");
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out.flush();

                System.out.println("Respondiendo pings de prueba...");
                while (true) {
                    Message msg = (Message) in.readObject();
                    if (msg instanceof Message.Ping ping) {
                        out.writeObject(new Message.Pong(ping.timestamp()));
                        out.flush();
                    }
                }
            } catch (EOFException e) {
                System.out.println("La otra computadora cerró la conexión. Prueba terminada correctamente.");
            }
        }
    }

    private static void runClient() throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("Buscando la otra computadora en la red...");

        Discovery.HostInfo hostInfo;
        try {
            hostInfo = Discovery.discoverHost(5000); // espera hasta 5 segundos
        } catch (IOException e) {
            System.out.println("No se encontró ninguna computadora anunciándose.");
            System.out.println("¿Corriste 'java ConnectionTest host' en la otra máquina? ¿Están en la misma red WiFi?");
            return;
        }

        System.out.println("Encontrada en " + hostInfo.address().getHostAddress() + ":" + hostInfo.port());

        try (Socket socket = new Socket(hostInfo.address(), hostInfo.port())) {
            System.out.println("Conexión TCP establecida correctamente.");
            System.out.println("Midiendo latencia con 5 pings...\n");

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out.flush();

            long totalRtt = 0;
            int count = 5;
            for (int i = 1; i <= count; i++) {
                long start = System.currentTimeMillis();
                out.writeObject(new Message.Ping(start));
                out.flush();

                Message.Pong pong = (Message.Pong) in.readObject();
                long rtt = System.currentTimeMillis() - pong.timestamp();
                totalRtt += rtt;
                System.out.println("Ping " + i + ": " + rtt + " ms");
                Thread.sleep(300);
            }

            System.out.println("\n--- Resultado ---");
            System.out.println("Conexión OK. Latencia promedio: " + (totalRtt / count) + " ms");
            System.out.println("(por WiFi local, algo entre 1-20 ms es normal; más de eso puede indicar interferencia)");
        }
    }
}