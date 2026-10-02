import config.Configuracion;
import protocolo.Mensaje;
import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Cliente {

    private Socket socket;

    private ObjectOutputStream out;

    private ObjectInputStream in;

    private int computadora;

    // Las respuestas a registrarse/iniciar sesión/crear/entrar. El hilo lector las
    // mete aquí y recibirRespuesta() las saca (espera si todavía no llegan).
    private final BlockingQueue<Mensaje.Respuesta> respuestas =
            new LinkedBlockingQueue<>();

    // Quién recibe lo que el servidor manda por su cuenta (Dados, Aviso,
    // EstadoJuego). Es la ventana del tablero.
    private Consumer<Mensaje> oyente;

    // El último estado recibido, por si llega antes de que la ventana
    // esté lista para escucharlo.
    private Mensaje.EstadoJuego ultimoEstado;


    // =========================================================
    // CONECTAR
    // =========================================================

    public void conectar()
            throws IOException, ClassNotFoundException {

        Descubrimiento.InfoServidor servidor =
                Descubrimiento.buscarServidor(Configuracion.TIEMPO_DESCUBRIMIENTO_MS);

        if (servidor == null) {

            throw new IOException(
                    "No se encontró el servidor."
            );
        }


        socket =
                new Socket(
                        servidor.direccion(),
                        servidor.puerto()
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


        if (objeto instanceof Mensaje.Bienvenida bienvenida) {

            computadora =
                    bienvenida.computadora();

            System.out.println(
                    "Conectado como computadora "
                            + computadora
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

                    if (objeto instanceof Mensaje.Respuesta respuesta) {

                        respuestas.put(respuesta);

                    } else if (objeto instanceof Mensaje mensaje) {

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
            Mensaje mensaje
    ) {

        if (mensaje instanceof Mensaje.EstadoJuego estado) {
            ultimoEstado = estado;
        }

        if (oyente != null) {
            oyente.accept(mensaje);
        }
    }

    // Registra al oyente y, si ya había llegado un estado, se lo pasa de una vez
    public synchronized void setOyente(
            Consumer<Mensaje> oyente
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
            Mensaje accion
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

    public Mensaje.Respuesta registrar(
            String nombre
    )
            throws IOException, ClassNotFoundException {

        enviar(
                new Mensaje.Registrarse(nombre)
        );

        return recibirRespuesta();
    }


    // =========================================================
    // CREAR PARTIDA
    // =========================================================

    public Mensaje.Respuesta crearPartida(
            String jugador1,
            String jugador2
    )
            throws IOException, ClassNotFoundException {

        enviar(
                new Mensaje.CrearPartida(
                        jugador1,
                        jugador2
                )
        );

        return recibirRespuesta();
    }


    // =========================================================
    // ENTRAR A PARTIDA
    // =========================================================

    public Mensaje.Respuesta entrarPartida(
            String codigo,
            String jugador1,
            String jugador2
    )
            throws IOException, ClassNotFoundException {

        enviar(
                new Mensaje.UnirsePartida(
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
            Mensaje mensaje
    ) throws IOException {

        out.writeObject(mensaje);

        out.flush();
    }


    // =========================================================
    // RECIBIR RESPUESTA
    // =========================================================

    private Mensaje.Respuesta recibirRespuesta()
            throws IOException, ClassNotFoundException {

        try {

            // Ya no leemos del socket aquí: el hilo lector lo hace
            Mensaje.Respuesta respuesta =
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

    public int getNumeroComputadora() {

        return computadora;
    }


    // =========================================================
    // CERRAR
    // =========================================================

    public void cerrar() {

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