import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

/**
 * Descubrimiento automático del host en la red local, usando UDP broadcast.
 * Así el jugador que se conecta NO necesita escribir la IP a mano: solo
 * tiene que estar en la misma red WiFi que el host.
 *
 * Cómo funciona:
 *  - El HOST corre un hilo que escucha en el puerto de descubrimiento.
 *    Cuando le llega el mensaje "DISCOVER_GAME_REQUEST", responde con
 *    "GAME_HERE:<puertoTCP>" directo a quien preguntó.
 *  - El CLIENTE manda ese mensaje a la dirección de broadcast de la red
 *    (255.255.255.255) y espera respuesta. La IP del host la saca del
 *    paquete que le llega (packet.getAddress()), no hace falta que nadie
 *    la escriba.
 */
public class Discovery {

    private static final int DISCOVERY_PORT = 5001;
    private static final String REQUEST = "DISCOVER_GAME_REQUEST";
    private static final String RESPONSE_PREFIX = "GAME_HERE:";

    /**
     * Se corre en el HOST, en un hilo aparte, mientras dure la partida.
     * gamePort = el puerto TCP donde está GameServer (ej. 5000).
     */
    public static void hostAnnounce(int gamePort) {
        Thread t = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
                byte[] buffer = new byte[256];
                System.out.println("Anunciándose en la red para que otros jugadores lo encuentren...");
                while (true) {
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    socket.receive(request); // se bloquea hasta que alguien pregunte

                    String msg = new String(request.getData(), 0, request.getLength());
                    if (REQUEST.equals(msg)) {
                        byte[] responseData = (RESPONSE_PREFIX + gamePort).getBytes();
                        DatagramPacket response = new DatagramPacket(
                                responseData, responseData.length,
                                request.getAddress(), request.getPort());
                        socket.send(response);
                        System.out.println("Respondido a jugador que preguntó desde " + request.getAddress());
                    }
                }
            } catch (IOException e) {
                System.out.println("Se detuvo el anuncio de descubrimiento: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Se corre en el CLIENTE. Busca un host en la red local y devuelve
     * su dirección + puerto TCP. Tarda como máximo 'timeoutMs' antes de
     * lanzar una excepción si no encontró nada.
     */
    public static HostInfo discoverHost(int timeoutMs) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(timeoutMs);

            byte[] requestData = REQUEST.getBytes();

            // Manda la pregunta a todas las direcciones de broadcast disponibles
            // (en algunas redes 255.255.255.255 no basta y conviene mandar
            // también al broadcast específico de cada interfaz de red).
            for (InetAddress broadcastAddr : listBroadcastAddresses()) {
                DatagramPacket packet = new DatagramPacket(
                        requestData, requestData.length, broadcastAddr, DISCOVERY_PORT);
                socket.send(packet);
            }

            byte[] buffer = new byte[256];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response); // espera la primera respuesta que llegue

            String msg = new String(response.getData(), 0, response.getLength());
            if (!msg.startsWith(RESPONSE_PREFIX)) {
                throw new IOException("Respuesta inesperada del host: " + msg);
            }
            int gamePort = Integer.parseInt(msg.substring(RESPONSE_PREFIX.length()));
            return new HostInfo(response.getAddress(), gamePort);
        }
    }

    /** Recorre las interfaces de red locales para armar direcciones de broadcast. */
    private static java.util.List<InetAddress> listBroadcastAddresses() throws IOException {
        java.util.List<InetAddress> result = new java.util.ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (iface.isLoopback() || !iface.isUp()) continue;
            for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                InetAddress broadcast = addr.getBroadcast();
                if (broadcast != null) result.add(broadcast);
            }
        }
        if (result.isEmpty()) {
            result.add(InetAddress.getByName("255.255.255.255"));
        }
        return result;
    }

    public record HostInfo(InetAddress address, int port) {}
}