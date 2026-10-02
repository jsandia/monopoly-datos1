import config.Configuracion;
import protocolo.Mensaje;
import protocolo.Mensaje.Respuesta;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * La conexión de una computadora con el servidor.
 *
 * Un hilo lector recibe todo lo que manda el servidor:
 * - las Respuesta (a registrarse, crear o unirse a una partida) se guardan
 *   para quien las está esperando;
 * - lo demás (Dados, Aviso, EstadoJuego...) se le pasa al oyente, que es
 *   la ventana que esté abierta.
 */
public class Cliente {

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private int computadora; // 0 o 1, lo asigna el servidor en la Bienvenida

    // El hilo lector mete aquí las respuestas y recibirRespuesta() las saca
    // (esperando si todavía no llegan)
    private final BlockingQueue<Respuesta> respuestas = new LinkedBlockingQueue<>();

    // Quién recibe lo que el servidor manda por su cuenta
    private Consumer<Mensaje> oyente;

    // El último estado recibido, por si llega antes de que la ventana esté lista
    private Mensaje.EstadoJuego ultimoEstado;

    /**
     * Busca al servidor en la red, se conecta y empieza a escuchar.
     *
     * @throws IOException si no encuentra al servidor o no se puede conectar
     */
    public void conectar() throws IOException, ClassNotFoundException {
        Descubrimiento.InfoServidor servidor = Descubrimiento.buscarServidor(Configuracion.TIEMPO_DESCUBRIMIENTO_MS);
        Socket socket = new Socket(servidor.direccion(), servidor.puerto());

        // La salida se crea antes que la entrada: el otro lado espera un encabezado
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        if (in.readObject() instanceof Mensaje.Bienvenida bienvenida) {
            computadora = bienvenida.computadora();
            System.out.println("Conectado como computadora " + computadora);
        }
        iniciarHiloLector();
    }

    // readObject() se queda esperando hasta que llegue algo. Por eso va en su
    // propio hilo: así la interfaz no se congela y el servidor puede mandar
    // dados en cualquier momento.
    private void iniciarHiloLector() {
        Thread lector = new Thread(() -> {
            try {
                while (true) {
                    Object objeto = in.readObject();
                    if (objeto instanceof Respuesta respuesta) {
                        respuestas.put(respuesta);
                    } else if (objeto instanceof Mensaje mensaje) {
                        entregar(mensaje);
                    }
                }
            } catch (Exception e) {
                System.out.println("Se perdió la conexión con el servidor.");
            }
        });
        lector.setDaemon(true); // no impide que el programa se cierre
        lector.start();
    }

    private synchronized void entregar(Mensaje mensaje) {
        if (mensaje instanceof Mensaje.EstadoJuego estado) {
            ultimoEstado = estado;
        }
        if (oyente != null) {
            oyente.accept(mensaje);
        }
    }

    /** Cambia quién recibe los mensajes y, si ya había llegado un estado, se lo pasa de una vez. */
    public synchronized void setOyente(Consumer<Mensaje> oyente) {
        this.oyente = oyente;
        if (ultimoEstado != null) {
            oyente.accept(ultimoEstado);
        }
    }

    public int getNumeroComputadora() {
        return computadora;
    }

    // =========================================================
    // MENSAJES AL SERVIDOR
    // =========================================================

    /** Manda una intención (tirar, comprar, terminar turno...). Lo que pase llega después. */
    public void enviarAccion(Mensaje accion) {
        try {
            enviar(accion);
        } catch (IOException e) {
            System.out.println("No se pudo enviar la acción: " + e.getMessage());
        }
    }

    public Respuesta registrar(String nombre) throws IOException {
        enviar(new Mensaje.Registrarse(nombre));
        return recibirRespuesta();
    }

    public Respuesta crearPartida(String jugador1, String jugador2) throws IOException {
        enviar(new Mensaje.CrearPartida(jugador1, jugador2));
        return recibirRespuesta();
    }

    public Respuesta entrarPartida(String codigo, String jugador1, String jugador2) throws IOException {
        enviar(new Mensaje.UnirsePartida(codigo, jugador1, jugador2));
        return recibirRespuesta();
    }

    // synchronized: la interfaz puede mandar mensajes desde varios lugares
    private synchronized void enviar(Mensaje mensaje) throws IOException {
        out.writeObject(mensaje);
        out.flush();
    }

    // Espera hasta 10 segundos la respuesta que trae el hilo lector
    private Respuesta recibirRespuesta() throws IOException {
        try {
            Respuesta respuesta = respuestas.poll(10, TimeUnit.SECONDS);
            if (respuesta != null) {
                return respuesta;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new IOException("El servidor no respondió.");
    }
}
