import config.Configuracion;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * Permite que los clientes encuentren al servidor en la red local sin
 * escribir su IP, usando UDP broadcast:
 *
 * 1. El servidor escucha en Configuracion.PUERTO_DESCUBRIMIENTO.
 * 2. El cliente grita SOLICITUD a la dirección de broadcast de cada red.
 * 3. El servidor contesta PREFIJO_RESPUESTA + el puerto TCP del juego.
 * 4. El cliente se queda con la IP de quien contestó y ese puerto.
 *
 * Los dos textos viajan por la red: si se cambian, hay que cambiarlos en
 * todas las computadoras a la vez.
 */
public class Descubrimiento {

    private static final String SOLICITUD = "DISCOVER_GAME_REQUEST";
    private static final String PREFIJO_RESPUESTA = "GAME_HERE:";

    /** Dónde está el servidor encontrado. */
    public record InfoServidor(InetAddress direccion, int puerto) {}

    /**
     * El servidor anuncia su existencia en la red: contesta cada solicitud
     * con el puerto TCP del juego. Corre en un hilo aparte para no bloquear.
     */
    public static void anunciarServidor(int puertoJuego) {
        Thread hilo = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(Configuracion.PUERTO_DESCUBRIMIENTO)) {
                socket.setBroadcast(true);
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    socket.receive(paquete);
                    if (SOLICITUD.equals(texto(paquete))) {
                        byte[] respuesta = (PREFIJO_RESPUESTA + puertoJuego).getBytes(StandardCharsets.UTF_8);
                        socket.send(new DatagramPacket(respuesta, respuesta.length,
                                paquete.getAddress(), paquete.getPort()));
                    }
                }
            } catch (IOException e) {
                System.out.println("Descubrimiento detenido: " + e.getMessage());
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Busca automáticamente un servidor en la red.
     *
     * @param tiempoMaximoMs cuánto esperar antes de rendirse
     * @throws IOException si no contesta ningún servidor a tiempo
     */
    public static InfoServidor buscarServidor(int tiempoMaximoMs) throws IOException {
        byte[] solicitud = SOLICITUD.getBytes(StandardCharsets.UTF_8);
        long inicio = System.currentTimeMillis();

        while (System.currentTimeMillis() - inicio < tiempoMaximoMs) {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.setSoTimeout(Math.min(1000, tiempoMaximoMs));

                // Se manda la solicitud a la dirección de broadcast de cada red activa...
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface interfazRed = interfaces.nextElement();
                    try {
                        if (!interfazRed.isUp() || interfazRed.isLoopback()) continue;
                    } catch (SocketException e) {
                        continue;
                    }
                    for (InterfaceAddress direccion : interfazRed.getInterfaceAddresses()) {
                        if (direccion.getBroadcast() != null) {
                            enviar(socket, solicitud, direccion.getBroadcast());
                        }
                    }
                }
                // ...y a esta misma computadora, por si el servidor corre aquí
                enviar(socket, solicitud, InetAddress.getByName("127.0.0.1"));

                // Se esperan respuestas hasta que se acabe el tiempo de este intento
                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket respuesta = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(respuesta);
                    } catch (SocketTimeoutException e) {
                        break;
                    }
                    String texto = texto(respuesta);
                    if (texto.startsWith(PREFIJO_RESPUESTA)) {
                        int puerto = Integer.parseInt(texto.substring(PREFIJO_RESPUESTA.length()));
                        return new InfoServidor(respuesta.getAddress(), puerto);
                    }
                }
            }
        }
        throw new IOException("No se encontró ningún servidor.");
    }

    // Si un envío falla (por ejemplo, una red sin permiso), se sigue con las demás
    private static void enviar(DatagramSocket socket, byte[] datos, InetAddress destino) {
        try {
            socket.send(new DatagramPacket(datos, datos.length, destino, Configuracion.PUERTO_DESCUBRIMIENTO));
        } catch (IOException ignorada) {
        }
    }

    private static String texto(DatagramPacket paquete) {
        return new String(paquete.getData(), paquete.getOffset(), paquete.getLength(), StandardCharsets.UTF_8);
    }
}
