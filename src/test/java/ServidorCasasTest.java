import modelo.Juego;
import modelo.Jugador;
import modelo.Propiedad;
import protocolo.Mensaje;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class ServidorCasasTest {
    private Servidor servidor;
    private Juego juego;
    private Jugador ana;
    private ObjectOutputStream salida;
    private ByteArrayOutputStream bytes;

    private Object invocar(String nombre, Class<?>[] tipos, Object... argumentos) throws Exception {
        Method metodo = Servidor.class.getDeclaredMethod(nombre, tipos);
        metodo.setAccessible(true);
        return metodo.invoke(servidor, argumentos);
    }
    private Object campo(String nombre) throws Exception {
        Field campo = Servidor.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        return campo.get(servidor);
    }
    private void asignar(String nombre, Object valor) throws Exception {
        Field campo = Servidor.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(servidor, valor);
    }
    private void pedirCasa(int computadora, int id) throws Exception {
        invocar("comprarCasa", new Class<?>[]{int.class, ObjectOutputStream.class, int.class}, computadora, salida, id);
    }
    private Propiedad propiedad(int id) { return (Propiedad) juego.getTablero().obtenerCasilla(id); }

    @BeforeEach void preparar() throws Exception {
        servidor = new Servidor();
        juego = new Juego(20, 4, "TEST_CASAS_SERVIDOR", false);
        Jugador[] jugadores = (Jugador[]) campo("jugadores");
        int[] computadoras = (int[]) campo("computadoraDe");
        for (int i = 0; i < 4; i++) {
            jugadores[i] = new Jugador("J" + i, "Jugador " + i, 1500);
            jugadores[i].registrarTarjeta("ABC" + i);
            computadoras[i] = i / 2;
            juego.agregarJugador(jugadores[i]);
        }
        ana = jugadores[0];
        asignar("juego", juego);
        asignar("tablero", juego.getTablero());
        bytes = new ByteArrayOutputStream();
        salida = new ObjectOutputStream(bytes);
        assertTrue(juego.getBanco().comprarPropiedad(ana, propiedad(1)));
        assertTrue(juego.getBanco().comprarPropiedad(ana, propiedad(3)));
    }
    @AfterEach void limpiar() throws Exception {
        salida.close();
        Files.deleteIfExists(juego.getArchivoTransacciones());
    }

    @Test void compraAntesDeTirarSoloAlLeerTarjetaCorrectaYSincronizaEstado() throws Exception {
        ana.registrarRecorrido(40);
        int antes = ana.getSaldo();
        pedirCasa(0, 1);
        assertEquals(antes, ana.getSaldo());
        assertEquals(0, propiedad(1).getCantidadCasas());
        servidor.recibirTarjetaDelCajero("ABC1");
        assertEquals(0, propiedad(1).getCantidadCasas());
        servidor.recibirTarjetaDelCajero("ABC0");
        assertEquals(antes - 50, ana.getSaldo());
        assertEquals(1, propiedad(1).getCantidadCasas());
        servidor.recibirTarjetaDelCajero("ABC0");
        assertEquals(antes - 50, ana.getSaldo());
        Mensaje.EstadoJuego estado = (Mensaje.EstadoJuego) invocar("crearEstado", new Class<?>[0]);
        assertEquals(1, estado.propiedades()[1].casas());
        assertEquals(10, estado.propiedades()[1].renta());
        assertEquals(0, estado.propiedades()[1].propietario());
        assertEquals(31, estado.casasDisponibles());
        ByteArrayOutputStream copia = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(copia)) { out.writeObject(estado); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(copia.toByteArray()))) {
            assertEquals(10, ((Mensaje.EstadoJuego) in.readObject()).propiedades()[1].renta());
        }
    }

    @Test void rechazaOtraComputadoraCasillaInvalidaYCompraPendienteDuplicada() throws Exception {
        ana.registrarRecorrido(40);
        pedirCasa(1, 1);
        assertNull(campo("jugadorPendientePago"));
        pedirCasa(0, -1);
        assertNull(campo("jugadorPendientePago"));
        pedirCasa(0, 1);
        pedirCasa(0, 3);
        assertSame(propiedad(1), campo("propiedadPendientePago"));
        servidor.recibirTarjetaDelCajero("ABC0");
        assertEquals(1, propiedad(1).getCantidadCasas());
        assertEquals(0, propiedad(3).getCantidadCasas());
    }

    @Test void permiteDespuesDeTirarYCancelaPagoAlCambiarTurno() throws Exception {
        ana.registrarRecorrido(40);
        asignar("yaTiro", true);
        pedirCasa(0, 1);
        servidor.recibirTarjetaDelCajero("ABC0");
        assertEquals(1, propiedad(1).getCantidadCasas());
        pedirCasa(0, 3);
        invocar("terminarTurno", new Class<?>[]{int.class, ObjectOutputStream.class}, 0, salida);
        servidor.recibirTarjetaDelCajero("ABC0");
        assertEquals(0, propiedad(3).getCantidadCasas());
        assertNull(campo("jugadorPendientePago"));
    }

    @Test void exigeVueltaDesdeCompraAunquePoseaGrupoCompleto() throws Exception {
        pedirCasa(0, 1);
        assertNull(campo("jugadorPendientePago"));
        ana.registrarRecorrido(39);
        pedirCasa(0, 1);
        assertNull(campo("jugadorPendientePago"));
        Mensaje.EstadoJuego estado = (Mensaje.EstadoJuego) invocar("crearEstado", new Class<?>[0]);
        assertTrue(estado.propiedades()[1].motivoNoComprarCasa().contains("1 casillas"));
        ana.registrarRecorrido(1);
        pedirCasa(0, 1);
        assertSame(ana, campo("jugadorPendientePago"));
        servidor.recibirTarjetaDelCajero("ABC0");
        assertEquals(1, propiedad(1).getCantidadCasas());
    }

    private void cuatroCasas() {
        ana.registrarRecorrido(40);
        for (int i = 0; i < 4; i++) assertTrue(juego.getBanco().comprarCasa(ana, propiedad(1)));
    }

    private void pedirHotel(int computadora, int id) throws Exception {
        invocar("comprarHotel", new Class<?>[]{int.class, ObjectOutputStream.class, int.class}, computadora, salida, id);
    }

    @Test void hotelSoloSePagaConTarjetaCorrectaYSeSincronizaNuevaRenta() throws Exception {
        cuatroCasas();
        int saldo = ana.getSaldo();
        pedirHotel(0, 1);
        assertEquals(saldo, ana.getSaldo());
        assertFalse(propiedad(1).tieneHotel());
        servidor.recibirTarjetaDelCajero("ABC1");
        assertFalse(propiedad(1).tieneHotel());
        servidor.recibirTarjetaDelCajero("ABC0");
        assertEquals(saldo - 50, ana.getSaldo());
        assertTrue(propiedad(1).tieneHotel());
        servidor.recibirTarjetaDelCajero("ABC0");
        assertEquals(saldo - 50, ana.getSaldo());
        Mensaje.EstadoJuego estado = (Mensaje.EstadoJuego) invocar("crearEstado", new Class<?>[0]);
        assertTrue(estado.propiedades()[1].hotel());
        assertEquals(0, estado.propiedades()[1].casas());
        assertEquals(250, estado.propiedades()[1].renta());
        assertEquals(50, estado.propiedades()[1].precioHotel());
        assertNotNull(estado.propiedades()[1].motivoNoComprarHotel());
        assertEquals(32, estado.casasDisponibles());
        assertEquals(11, estado.hotelesDisponibles());
        ByteArrayOutputStream copia = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(copia)) { out.writeObject(estado); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(copia.toByteArray()))) {
            Mensaje.EstadoJuego recibido = (Mensaje.EstadoJuego) in.readObject();
            assertTrue(recibido.propiedades()[1].hotel());
            assertEquals(250, recibido.propiedades()[1].renta());
        }
    }

    @Test void hotelValidaTurnoCasasYComprasPendientesYRevisaSaldoAlPagar() throws Exception {
        pedirHotel(0, 1);
        assertNull(campo("jugadorPendientePago"));
        cuatroCasas();
        pedirHotel(1, 1);
        assertNull(campo("jugadorPendientePago"));
        pedirHotel(0, -1);
        assertNull(campo("jugadorPendientePago"));
        pedirHotel(0, 1);
        pedirCasa(0, 3);
        assertSame(propiedad(1), campo("propiedadPendientePago"));
        ana.setSaldo(49);
        servidor.recibirTarjetaDelCajero("ABC0");
        assertFalse(propiedad(1).tieneHotel());
        assertEquals(4, propiedad(1).getCantidadCasas());
        assertEquals(49, ana.getSaldo());
        assertNull(campo("jugadorPendientePago"));
    }

    @Test void hotelPendienteSeCancelaCuandoSeTerminaElTurno() throws Exception {
        cuatroCasas();
        asignar("yaTiro", true);
        pedirHotel(0, 1);
        invocar("terminarTurno", new Class<?>[]{int.class, ObjectOutputStream.class}, 0, salida);
        servidor.recibirTarjetaDelCajero("ABC0");
        assertFalse(propiedad(1).tieneHotel());
        assertEquals(4, propiedad(1).getCantidadCasas());
        assertNull(campo("jugadorPendientePago"));
    }
}
