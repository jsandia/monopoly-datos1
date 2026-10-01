import config.Configuracion;
import protocolo.Message;
import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GameClient {

    private Socket socket;

    private ObjectOutputStream out;

    private ObjectInputStream in;

    private int playerId;

    // Las respuestas a registrar/login/crear/entrar. El hilo lector las
    // mete aquí y recibirRespuesta() las saca (espera si todavía no llegan).
    private final BlockingQueue<Message.Response> respuestas =
            new LinkedBlockingQueue<>();

    // Quién recibe lo que el servidor manda por su cuenta (Dados, Aviso,
    // EstadoJuego). Es la ventana del tablero.
    private Consumer<Message> oyente;

    // El último estado recibido, por si llega antes de que la ventana
    // esté lista para escucharlo.
    private Message.EstadoJuego ultimoEstado;


    // =========================================================
    // CONECTAR
    // =========================================================

    public void conectar()
            throws IOException, ClassNotFoundException {

        Discovery.HostInfo host =
                Discovery.discoverHost(Configuracion.TIEMPO_DESCUBRIMIENTO_MS);

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

        iniciarHiloLector();
    }


    // =========================================================
    // HILO LECTOR
    // =========================================================

    // readObject() se queda bloqueado hasta que llegue algo. Por eso va
    // en su propio hilo: así la interfaz no se congela y el servidor
    // puede mandar dados en cualquier momento.
    private void iniciarHiloLector() {

        Thread lector = new Thread(() -> {

            try {

                while (true) {

                    Object objeto = in.readObject();

                    if (objeto instanceof Message.Response respuesta) {

                        respuestas.put(respuesta);

                    } else if (objeto instanceof Message mensaje) {

                        entregar(mensaje);
                    }
                }

            } catch (Exception e) {

                System.out.println(
                        "Se perdió la conexión con el servidor."
                );
            }
        });

        // daemon: no impide que el programa se cierre
        lector.setDaemon(true);

        lector.start();
    }

    private synchronized void entregar(
            Message mensaje
    ) {

        if (mensaje instanceof Message.EstadoJuego estado) {
            ultimoEstado = estado;
        }

        if (oyente != null) {
            oyente.accept(mensaje);
        }
    }

    // Registra al oyente y, si ya había llegado un estado, se lo pasa de una vez
    public synchronized void setOyente(
            Consumer<Message> oyente
    ) {

        this.oyente = oyente;

        if (ultimoEstado != null) {
            oyente.accept(ultimoEstado);
        }
    }


    // =========================================================
    // ACCIONES DE LA PARTIDA
    // =========================================================

    // Manda una intención (TirarDadosPrueba, ComprarPropiedad,
    // TerminarTurno). La respuesta llega después como Aviso/EstadoJuego.
    public void enviarAccion(
            Message accion
    ) {

        try {

            enviar(accion);

        } catch (IOException e) {

            System.out.println(
                    "No se pudo enviar la acción: "
                            + e.getMessage()
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

    // synchronized: la interfaz puede mandar acciones desde varios lugares
    private synchronized void enviar(
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

        try {

            // Ya no leemos del socket aquí: el hilo lector lo hace
            Message.Response respuesta =
                    respuestas.poll(10, TimeUnit.SECONDS);

            if (respuesta != null) {
                return respuesta;
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }

        throw new IOException(
                "El servidor no respondió."
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