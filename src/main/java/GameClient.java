import java.io.*;
import java.net.*;

public class GameClient {

    private Socket socket;

    private ObjectOutputStream out;

    private ObjectInputStream in;

    private int playerId;


    // =========================================================
    // CONECTAR
    // =========================================================

    public void conectar()
            throws IOException, ClassNotFoundException {

        Discovery.HostInfo host =
                Discovery.discoverHost(5000);

        if (host == null) {

            throw new IOException(
                    "No se encontró el servidor."
            );
        }


        socket =
                new Socket(
                        host.address(),
                        host.port()
                );


        out =
                new ObjectOutputStream(
                        socket.getOutputStream()
                );

        out.flush();


        in =
                new ObjectInputStream(
                        socket.getInputStream()
                );


        Object objeto =
                in.readObject();


        if (objeto instanceof Message.Welcome welcome) {

            playerId =
                    welcome.playerId();

            System.out.println(
                    "Conectado como computadora "
                            + playerId
            );
        }
    }


    // =========================================================
    // REGISTRAR
    // =========================================================

    public Message.Response registrar(
            String nombre
    )
            throws IOException, ClassNotFoundException {

        enviar(
                new Message.Register(nombre)
        );

        return recibirRespuesta();
    }


    // =========================================================
    // LOGIN
    // =========================================================

    public Message.Response login(
            String nombre
    )
            throws IOException, ClassNotFoundException {

        enviar(
                new Message.Login(nombre)
        );

        return recibirRespuesta();
    }


    // =========================================================
    // CREAR PARTIDA
    // =========================================================

    public Message.Response crearPartida(
            String jugador1,
            String jugador2
    )
            throws IOException, ClassNotFoundException {

        enviar(
                new Message.CreateGame(
                        jugador1,
                        jugador2
                )
        );

        return recibirRespuesta();
    }


    // =========================================================
    // ENTRAR A PARTIDA
    // =========================================================

    public Message.Response entrarPartida(
            String codigo,
            String jugador1,
            String jugador2
    )
            throws IOException, ClassNotFoundException {

        enviar(
                new Message.JoinGame(
                        codigo,
                        jugador1,
                        jugador2
                )
        );

        return recibirRespuesta();
    }


    // =========================================================
    // ENVIAR
    // =========================================================

    private void enviar(
            Message mensaje
    ) throws IOException {

        out.writeObject(mensaje);

        out.flush();
    }


    // =========================================================
    // RECIBIR RESPUESTA
    // =========================================================

    private Message.Response recibirRespuesta()
            throws IOException, ClassNotFoundException {

        Object objeto =
                in.readObject();

        if (objeto instanceof Message.Response respuesta) {

            return respuesta;
        }

        throw new IOException(
                "Respuesta inesperada del servidor."
        );
    }


    // =========================================================
    // GET PLAYER ID
    // =========================================================

    public int getPlayerId() {

        return playerId;
    }


    // =========================================================
    // CERRAR
    // =========================================================

    public void close() {

        try {

            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {

            System.out.println(
                    "Error cerrando conexión."
            );
        }
    }
}