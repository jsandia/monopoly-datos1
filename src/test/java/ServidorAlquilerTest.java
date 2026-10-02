import modelo.Juego;
import modelo.Jugador;
import modelo.Propiedad;
import protocolo.Mensaje;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

/** El alquiler se paga acercando la tarjeta del jugador en turno al cajero. */
class ServidorAlquilerTest {
    private Servidor servidor;
    private Juego juego;
    private Jugador[] jugadores;
    private ObjectOutputStream salida;

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
    private void tirar(int dado1, int dado2) throws Exception {
        assertNull(invocar("aplicarDados", new Class<?>[]{int.class, int.class}, dado1, dado2));
    }
    private void terminarTurno(int computadora) throws Exception {
        invocar("terminarTurno", new Class<?>[]{int.class, ObjectOutputStream.class}, computadora, salida);
    }
    private void pedirCasa(int computadora, int idPropiedad) throws Exception {
        invocar("comprarCasa", new Class<?>[]{int.class, ObjectOutputStream.class, int.class},
                computadora, salida, idPropiedad);
    }
    private Mensaje.EstadoJuego estado() throws Exception {
        return (Mensaje.EstadoJuego) invocar("crearEstado", new Class<?>[0]);
    }
    private Propiedad propiedad(int id) {
        return (Propiedad) juego.getTablero().obtenerCasilla(id);
    }

    @BeforeEach void preparar() throws Exception {
        servidor = new Servidor();
        juego = new Juego(20, 4, "TEST_ALQUILER_SERVIDOR", false);
        jugadores = (Jugador[]) campo("jugadores");
        int[] computadoras = (int[]) campo("computadoraDe");
        for (int i = 0; i < 4; i++) {
            jugadores[i] = new Jugador("J" + i, "Jugador " + i, 1500);
            if (i != 3) jugadores[i].registrarTarjeta("ABC" + i); // el jugador 3 no tiene tarjeta
            computadoras[i] = i / 2;
            juego.agregarJugador(jugadores[i]);
        }
        asignar("juego", juego);
        salida = new ObjectOutputStream(new ByteArrayOutputStream());

        // El aeropuerto de la casilla 5 es del jugador 1: alquiler ₡25
        assertTrue(juego.getBanco().comprarPropiedad(jugadores[1], propiedad(5)));
    }

    @AfterEach void limpiar() throws Exception {
        salida.close();
        Files.deleteIfExists(juego.getArchivoTransacciones());
    }

    @Test void elAlquilerSePagaSoloConLaTarjetaDelJugadorEnTurno() throws Exception {
        jugadores[0].setPosicionActual(2);
        tirar(1, 2); // cae en el aeropuerto del jugador 1

        assertEquals("₡25 de alquiler a Jugador 1 por Aeropuerto Juan Santamaría", estado().pagoPendiente());
        assertEquals(1500, jugadores[0].getSaldo());

        servidor.recibirTarjetaDelCajero("ABC1"); // la del dueño: no sirve
        assertTrue(juego.hayAlquilerPendiente());
        assertEquals(1500, jugadores[0].getSaldo());

        servidor.recibirTarjetaDelCajero("abc0"); // la suya
        assertFalse(juego.hayAlquilerPendiente());
        assertNull(estado().pagoPendiente());
        assertEquals(1475, jugadores[0].getSaldo());
        assertEquals(1325, jugadores[1].getSaldo());
    }

    @Test void conElAlquilerPendienteNoSePuedeTerminarElTurno() throws Exception {
        jugadores[0].setPosicionActual(2);
        tirar(1, 2);

        terminarTurno(0);
        assertSame(jugadores[0], juego.getJugadorActual());

        servidor.recibirTarjetaDelCajero("ABC0");
        terminarTurno(0);
        assertSame(jugadores[1], juego.getJugadorActual());
    }

    @Test void conElAlquilerPendienteNoSePuedeComprarUnaCasa() throws Exception {
        assertTrue(juego.getBanco().comprarPropiedad(jugadores[0], propiedad(1)));
        jugadores[0].registrarRecorrido(40);
        jugadores[0].setPosicionActual(2);
        tirar(1, 2);

        pedirCasa(0, 1);

        assertNull(campo("jugadorPendientePago"));
    }

    @Test void elAlquilerSeCobraAntesQueUnaCompraPedidaAntesDeTirar() throws Exception {
        assertTrue(juego.getBanco().comprarPropiedad(jugadores[0], propiedad(1)));
        jugadores[0].registrarRecorrido(40);
        pedirCasa(0, 1);              // queda esperando la tarjeta
        jugadores[0].setPosicionActual(2);
        tirar(1, 2);                  // y ahora además debe el alquiler
        int saldo = jugadores[0].getSaldo();

        servidor.recibirTarjetaDelCajero("ABC0"); // primero el alquiler
        assertFalse(juego.hayAlquilerPendiente());
        assertEquals(saldo - 25, jugadores[0].getSaldo());
        assertEquals(0, propiedad(1).getCantidadCasas());

        servidor.recibirTarjetaDelCajero("ABC0"); // después la casa
        assertEquals(1, propiedad(1).getCantidadCasas());
        assertEquals(saldo - 25 - 50, jugadores[0].getSaldo());
    }

    @Test void siElJugadorNoTieneTarjetaElAlquilerSeCobraSolo() throws Exception {
        for (int i = 0; i < 3; i++) juego.terminarTurnoActual(); // turno del jugador 3
        jugadores[3].setPosicionActual(2);

        tirar(1, 2);

        assertFalse(juego.hayAlquilerPendiente());
        assertEquals(1475, jugadores[3].getSaldo());
        assertNull(estado().pagoPendiente());
    }

    @Test void siNoLeAlcanzaLaBancarrotaEsInmediataSinPedirTarjeta() throws Exception {
        jugadores[0].setSaldo(10);
        jugadores[0].setPosicionActual(2);

        tirar(1, 2);

        assertFalse(juego.hayAlquilerPendiente());
        assertFalse(jugadores[0].isActivo());
        assertSame(jugadores[1], juego.getJugadorActual()); // el servidor pasó el turno solo
    }
}
