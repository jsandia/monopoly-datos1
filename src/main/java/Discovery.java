import config.Configuracion;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class Discovery {

    private static final String REQUEST =
            "DISCOVER_GAME_REQUEST";

    private static final String RESPONSE_PREFIX =
            "GAME_HERE:";


    /**
     * Información del servidor encontrado.
     */
    public record HostInfo(
            InetAddress address,
            int port
    ) {}


    /**
     * El servidor anuncia su existencia en la red.
     */
    public static void hostAnnounce(int gamePort) {

        Thread thread = new Thread(() -> {

            try (DatagramSocket socket =
                         new DatagramSocket(Configuracion.PUERTO_DESCUBRIMIENTO)) {

                socket.setBroadcast(true);

                byte[] buffer =
                        new byte[1024];

                while (true) {

                    DatagramPacket packet =
                            new DatagramPacket(
                                    buffer,
                                    buffer.length
                            );

                    socket.receive(packet);

                    String message =
                            new String(
                                    packet.getData(),
                                    packet.getOffset(),
                                    packet.getLength(),
                                    StandardCharsets.UTF_8
                            );

                    if (REQUEST.equals(message)) {

                        String response =
                                RESPONSE_PREFIX + gamePort;

                        byte[] responseData =
                                response.getBytes(
                                        StandardCharsets.UTF_8
                                );

                        DatagramPacket responsePacket =
                                new DatagramPacket(
                                        responseData,
                                        responseData.length,
                                        packet.getAddress(),
                                        packet.getPort()
                                );

                        socket.send(responsePacket);
                    }
                }

            } catch (IOException e) {

                System.out.println(
                        "Discovery detenido: "
                                + e.getMessage()
                );
            }

        });

        thread.setDaemon(true);
        thread.start();
    }


    /**
     * Busca automáticamente un servidor en la red.
     */
    public static HostInfo discoverHost(
            int timeoutMs
    ) throws IOException {

        long start =
                System.currentTimeMillis();

        byte[] requestData =
                REQUEST.getBytes(
                        StandardCharsets.UTF_8
                );


        while (System.currentTimeMillis() - start
                < timeoutMs) {

            try (DatagramSocket socket =
                         new DatagramSocket()) {

                socket.setBroadcast(true);
                socket.setSoTimeout(
                        (int) Math.min(
                                1000,
                                timeoutMs
                        )
                );


                Enumeration<NetworkInterface>
                        interfaces =
                        NetworkInterface.getNetworkInterfaces();


                while (interfaces.hasMoreElements()) {

                    NetworkInterface networkInterface =
                            interfaces.nextElement();

                    try {

                        if (!networkInterface.isUp()
                                || networkInterface.isLoopback()) {
                            continue;
                        }

                    } catch (SocketException e) {
                        continue;
                    }


                    for (InterfaceAddress interfaceAddress :
                            networkInterface.getInterfaceAddresses()) {

                        InetAddress broadcast =
                                interfaceAddress.getBroadcast();

                        if (broadcast == null) {
                            continue;
                        }


                        DatagramPacket packet =
                                new DatagramPacket(
                                        requestData,
                                        requestData.length,
                                        broadcast,
                                        Configuracion.PUERTO_DESCUBRIMIENTO
                                );

                        try {

                            socket.send(packet);

                        } catch (IOException ignored) {
                        }
                    }
                }


                // También intentamos localhost.
                try {

                    DatagramPacket localhostPacket =
                            new DatagramPacket(
                                    requestData,
                                    requestData.length,
                                    InetAddress.getByName(
                                            "127.0.0.1"
                                    ),
                                    Configuracion.PUERTO_DESCUBRIMIENTO
                            );

                    socket.send(localhostPacket);

                } catch (IOException ignored) {
                }


                while (true) {

                    try {

                        byte[] buffer =
                                new byte[1024];

                        DatagramPacket response =
                                new DatagramPacket(
                                        buffer,
                                        buffer.length
                                );

                        socket.receive(response);

                        String message =
                                new String(
                                        response.getData(),
                                        response.getOffset(),
                                        response.getLength(),
                                        StandardCharsets.UTF_8
                                );


                        if (message.startsWith(
                                RESPONSE_PREFIX)) {

                            String portText =
                                    message.substring(
                                            RESPONSE_PREFIX.length()
                                    );

                            int port =
                                    Integer.parseInt(
                                            portText
                                    );

                            return new HostInfo(
                                    response.getAddress(),
                                    port
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