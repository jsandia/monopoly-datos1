import protocolo.Mensaje;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Atiende la conexión con una computadora (cliente Swing), en su propio hilo.
 *
 * Su único trabajo es de "cartero": lee cada Mensaje que llega por el
 * socket y le pide al Servidor que lo resuelva. No decide nada del juego;
 * todas las reglas y validaciones viven en Servidor y en Juego.
 *
 * Es el equivalente de ManejadorCajero, pero para las computadoras: el
 * cajero habla con texto por líneas y las computadoras con objetos Mensaje.
 */
public class ManejadorCliente implements Runnable {

    private final Socket socket;
    private final int computadora; // 0 = la primera que se conectó, 1 = la segunda
    private final Servidor servidor;

    public ManejadorCliente(Socket socket, int computadora, Servidor servidor) {
        this.socket = socket;
        this.computadora = computadora;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        // try-with-resources: al salir (desconexión o error) se cierra el socket
        try (socket) {
            // La salida se crea y se vacía ANTES que la entrada: el
            // ObjectInputStream del otro lado espera un encabezado al arrancar
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Le avisa qué número de computadora le tocó
            out.writeObject(new Mensaje.Bienvenida(computadora));
            out.flush();

            // Desde aquí el servidor le puede escribir cuando quiera (dados, avisos, estado)
            servidor.conectarComputadora(computadora, out);

            while (true) {
                // readObject() espera hasta que llegue un objeto completo
                if (in.readObject() instanceof Mensaje mensaje) {
                    procesar(mensaje, out);
                }
            }

        } catch (EOFException e) {
            // Así termina readObject() cuando la otra computadora cierra la conexión
            System.out.println("Computadora " + computadora + " se desconectó.");

        } catch (Exception e) {
            System.out.println("Error con la computadora " + computadora);
            e.printStackTrace();

        } finally {
            servidor.desconectarComputadora(computadora);
        }
    }

    /**
     * Le pasa cada intención al método del Servidor que la resuelve.
     *
     * Es un switch sobre el tipo del mensaje (pattern matching). "_" es una
     * variable sin nombre: se usa cuando el mensaje no trae datos que leer.
     */
    private void procesar(Mensaje mensaje, ObjectOutputStream out) throws IOException {
        switch (mensaje) {
            // Antes de jugar: usuarios y partidas
            case Mensaje.Registrarse m -> servidor.registrarUsuario(m.nombre(), out);
            case Mensaje.CrearPartida m ->
                    servidor.crearPartida(m.jugador1(), m.jugador2(), computadora, out);
            case Mensaje.UnirsePartida m ->
                    servidor.entrarPartida(m.codigo(), m.jugador1(), m.jugador2(), computadora, out);
            case Mensaje.RegistrarTarjeta m ->
                    servidor.solicitarRegistroTarjeta(computadora, m.jugadorLocal(), out);

            // Durante la partida: el cliente solo pide, el servidor decide
            case Mensaje.TirarDadosPrueba _ -> servidor.tirarDadosPrueba(computadora, out);
            case Mensaje.ComprarPropiedad _ -> servidor.comprarPropiedad(computadora, out);
            case Mensaje.ComprarCasa m -> servidor.comprarCasa(computadora, out, m.idPropiedad());
            case Mensaje.ComprarHotel m -> servidor.comprarHotel(computadora, out, m.idPropiedad());
            case Mensaje.TerminarTurno _ -> servidor.terminarTurno(computadora, out);
            case Mensaje.PagarMultaCarcel _ -> servidor.salirDeCarcel(computadora, out, true);
            case Mensaje.UsarCartaCarcel _ -> servidor.salirDeCarcel(computadora, out, false);
            case Mensaje.ConsultarTransacciones m ->
                    servidor.consultarTransacciones(computadora, out, m, false);
            case Mensaje.ExportarTransacciones _ ->
                    servidor.consultarTransacciones(computadora, out, null, true);

            // El resto (EstadoJuego, Aviso, Dados...) solo lo manda el servidor:
            // si un cliente lo envía, se ignora
            default -> { }
        }
    }
}
