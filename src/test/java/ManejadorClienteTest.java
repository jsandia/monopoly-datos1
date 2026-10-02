import protocolo.Mensaje;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

/** Un cliente de verdad, por TCP local, hablando con un ManejadorCliente. */
class ManejadorClienteTest {
    private ServerSocket servidorSocket;
    private Socket conexionCliente;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Servidor servidor;

    @BeforeEach void conectar() throws Exception {
        servidor = new Servidor();
        // Puerto 0: el sistema elige uno libre
        servidorSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        conexionCliente = new Socket(InetAddress.getLoopbackAddress(), servidorSocket.getLocalPort());
        Socket aceptado = servidorSocket.accept();
        new Thread(new ManejadorCliente(aceptado, 0, servidor)).start();

        out = new ObjectOutputStream(conexionCliente.getOutputStream());
        out.flush();
        in = new ObjectInputStream(conexionCliente.getInputStream());
        conexionCliente.setSoTimeout(5000); // si algo se traba, la prueba falla en vez de colgarse
    }

    @AfterEach void cerrar() throws Exception {
        conexionCliente.close();
        servidorSocket.close();
    }

    private Object enviarYRecibir(Mensaje mensaje) throws Exception {
        out.writeObject(mensaje);
        out.flush();
        return in.readObject();
    }

    private ObjectOutputStream salidaRegistrada() throws Exception {
        Field campo = Servidor.class.getDeclaredField("salidas");
        campo.setAccessible(true);
        return ((ObjectOutputStream[]) campo.get(servidor))[0];
    }

    @Test void saludaConElNumeroDeComputadoraYQuedaRegistrada() throws Exception {
        assertEquals(new Mensaje.Bienvenida(0), in.readObject());
        assertNotNull(salidaRegistrada());
    }

    @Test void pasaCadaMensajeAlMetodoQueLoResuelve() throws Exception {
        in.readObject(); // Bienvenida

        Mensaje.Respuesta registro = (Mensaje.Respuesta) enviarYRecibir(new Mensaje.Registrarse("Ana"));
        assertEquals("REGISTER_OK", registro.tipo());

        Mensaje.Respuesta repetido = (Mensaje.Respuesta) enviarYRecibir(new Mensaje.Registrarse("ana"));
        assertEquals("USER_EXISTS", repetido.tipo());
    }

    @Test void ignoraMensajesQueSoloPuedeMandarElServidor() throws Exception {
        in.readObject(); // Bienvenida
        out.writeObject(new Mensaje.Aviso("un cliente no debería mandar esto"));
        out.flush();
        // Sigue atendiendo normal: el siguiente mensaje tiene respuesta
        Mensaje.Respuesta registro = (Mensaje.Respuesta) enviarYRecibir(new Mensaje.Registrarse("Ana"));
        assertEquals("REGISTER_OK", registro.tipo());
    }

    @Test void alDesconectarseDejaDeRecibirMensajes() throws Exception {
        in.readObject(); // Bienvenida
        assertNotNull(salidaRegistrada());

        conexionCliente.close();

        // El hilo del manejador tarda un momento en notar el cierre
        long limite = System.currentTimeMillis() + 5000;
        while (salidaRegistrada() != null && System.currentTimeMillis() < limite) {
            Thread.sleep(20);
        }
        assertNull(salidaRegistrada());
    }
}
