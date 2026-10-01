import config.Configuracion;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * Permite que los clientes encuentren al servidor en la red local sin
 * escribir su IP, usando UDP broadcast:
 *
 * 1. El servidor escucha en Configuracion.PUERTO_DESCUBRIMIENTO.
 * 2. El cliente grita SOLICITUD a la direccion de broadcast de cada red.
 * 3. El servidor contesta PREFIJO_RESPUESTA + el puerto TCP del juego.
 * 4. El cliente se queda con la IP de quien contesto y ese puerto.
 *
 * Los dos textos viajan por la red: si se cambian, hay que cambiarlos en
 * todas las computadoras a la vez.
 */
public class Descubrimiento {

    private static final String SOLICITUD =
            "DISCOVER_GAME_REQUEST";

    private static final String PREFIJO_RESPUESTA =
            "GAME_HERE:";


    /**
     * Donde esta el servidor encontrado.
     */
    public record InfoServidor(
            InetAddress direccion,
            int puerto
    ) {}


    /**
     * El servidor anuncia su existencia en la red: contesta cada solicitud
     * con el puerto TCP del juego. Corre en un hilo aparte para no bloquear.
     */
    public static void anunciarServidor(int puertoJuego) {

        Thread hilo = new Thread(() -> {

            try (DatagramSocket socket =
                         new DatagramSocket(Configuracion.PUERTO_DESCUBRIMIENTO)) {

                socket.setBroadcast(true);

                byte[] buffer =
                        new byte[1024];

                while (true) {

                    DatagramPacket paquete =
                            new DatagramPacket(
                                    buffer,
                                    buffer.length
                            );

                    socket.receive(paquete);

                    String texto =
                            new String(
                                    paquete.getData(),
                                    paquete.getOffset(),
                                    paquete.getLength(),
                                    StandardCharsets.UTF_8
                            );

                    if (SOLICITUD.equals(texto)) {

                        byte[] datosRespuesta =
                                (PREFIJO_RESPUESTA + puertoJuego).getBytes(
                                        StandardCharsets.UTF_8
                                );

                        DatagramPacket paqueteRespuesta =
                                new DatagramPacket(
                                        datosRespuesta,
                                        datosRespuesta.length,
                                        paquete.getAddress(),
                                        paquete.getPort()
                                );

                        socket.send(paqueteRespuesta);
                    }
                }

            } catch (IOException e) {

                System.out.println(
                        "Descubrimiento detenido: "
                                + e.getMessage()
                );
            }

        });

        hilo.setDaemon(true);
        hilo.start();
    }


    /**
     * Busca automaticamente un servidor en la red.
     *
     * @param tiempoMaximoMs cuanto esperar antes de rendirse
     * @throws IOException si no contesta ningun servidor a tiempo
     */
    public static InfoServidor buscarServidor(
            int tiempoMaximoMs
    ) throws IOException {

        long inicio =
                System.currentTimeMillis();

        byte[] datosSolicitud =
                SOLICITUD.getBytes(
                        StandardCharsets.UTF_8
                );


        while (System.currentTimeMillis() - inicio
                < tiempoMaximoMs) {

            try (DatagramSocket socket =
                         new DatagramSocket()) {

                socket.setBroadcast(true);
                socket.setSoTimeout(
                        (int) Math.min(
                                1000,
                                tiempoMaximoMs
                        )
                );


                // Se manda la solicitud a la direccion de broadcast de cada red activa
                Enumeration<NetworkInterface>
                        interfaces =
                        NetworkInterface.getNetworkInterfaces();


                while (interfaces.hasMoreElements()) {

                    NetworkInterface interfazRed =
                            interfaces.nextElement();

                    try {

                        if (!interfazRed.isUp()
                                || interfazRed.isLoopback()) {
                            continue;
                        }

                    } catch (SocketException e) {
                        continue;
                    }


                    for (InterfaceAddress direccionInterfaz :
                            interfazRed.getInterfaceAddresses()) {

                        InetAddress broadcast =
                                direccionInterfaz.getBroadcast();

                        if (broadcast == null) {
                            continue;
                        }


                        DatagramPacket paquete =
                                new DatagramPacket(
                                        datosSolicitud,
                                        datosSolicitud.length,
                                        broadcast,
                                        Configuracion.PUERTO_DESCUBRIMIENTO
                                );

                        try {

                            socket.send(paquete);

                        } catch (IOException ignorada) {
                        }
                    }
                }


                // También intentamos localhost (servidor en esta misma computadora).
                try {

                    DatagramPacket paqueteLocal =
                            new DatagramPacket(
                                    datosSolicitud,
                                    datosSolicitud.length,
                                    InetAddress.getByName(
                                            "127.0.0.1"
                                    ),
                                    Configuracion.PUERTO_DESCUBRIMIENTO
                            );

                    socket.send(paqueteLocal);

                } catch (IOException ignorada) {
                }


                while (true) {

                    try {

                        byte[] buffer =
                                new byte[1024];

                        DatagramPacket respuesta =
                                new DatagramPacket(
                                        buffer,
                                        buffer.length
                                );

                        socket.receive(respuesta);

                        String texto =
                                new String(
                                        respuesta.getData(),
                                        respuesta.getOffset(),
                                        respuesta.getLength(),
                                        StandardCharsets.UTF_8
                                );


                        if (texto.startsWith(
                                PREFIJO_RESPUESTA)) {

                            int puerto =
                                    Integer.parseInt(
                                            texto.substring(
                                                    PREFIJO_RESPUESTA.length()
                                            )
                                    );

                            return new InfoServidor(
                                    respuesta.getAddress(),
                                    puerto
                            );
                        }

                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }
            }
        }


        throw new IOException(
                "No se encontró ningún servidor."
        );
    }
}
