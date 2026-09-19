import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Cliente del juego. Se corre en las DOS computadoras (incluida la
 * que hace de servidor, en otra terminal/proceso).
 *
 * Por defecto NO hace falta escribir ninguna IP: se descubre solo al
 * host que esté anunciándose en la red local (ver Discovery.java).
 * Si por algún motivo el descubrimiento automático falla (por ejemplo,
 * un router que bloquea broadcast), todavía se puede forzar la IP a mano:
 *
 *   java GameClient 192.168.4.1 5000
 */
public class GameClient {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String serverIp;
        int port;

        if (args.length >= 2) {
            // Modo manual (fallback, por si el broadcast no funciona en esa red)
            serverIp = args[0];
            port = Integer.parseInt(args[1]);
        } else {
            System.out.println("Buscando partida en la red local...");
            try {
                Discovery.HostInfo host = Discovery.discoverHost(5000);
                serverIp = host.address().getHostAddress();
                port = host.port();
                System.out.println("Partida encontrada en " + serverIp + ":" + port);
            } catch (IOException e) {
                System.out.println("No se encontró ninguna partida en la red (¿el host está corriendo?)");
                System.out.println("También podés forzar la IP a mano: java GameClient <ip> <puerto>");
                return;
            }
        }

        try (Socket socket = new Socket(serverIp, port)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // 1. El servidor nos asigna nuestro id de jugador
            Message.Welcome welcome = (Message.Welcome) in.readObject();
            int myId = welcome.playerId();
            System.out.println("Conectado. Sos el jugador " + myId);

            // 2. Hilo que escucha el estado del juego que manda el servidor
            Thread listener = new Thread(() -> {
                try {
                    while (true) {
                        Message msg = (Message) in.readObject();
                        switch (msg) {
                            case Message.GameState state -> System.out.printf(
                                    "Estado -> J0(%.1f,%.1f) J1(%.1f,%.1f) puntajes %d-%d%n",
                                    state.p0x(), state.p0y(), state.p1x(), state.p1y(),
                                    state.score0(), state.score1());
                            case Message.PlayerDisconnected d -> System.out.println("Jugador " + d.playerId() + " se desconectó");
                            default -> {}
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Se perdió la conexión con el servidor");
                }
            });
            listener.setDaemon(true);
            listener.start();

            // 3. Bucle simple para probar: escribís "x y" y se envía como movimiento
            Scanner scanner = new Scanner(System.in);
            System.out.println("Escribí tu posición como 'x y' (ej: '3.5 2.0') y Enter:");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                out.writeObject(new Message.PlayerMove(myId, x, y, "move"));
                out.flush();
            }
        }
    }
}
