import modelo.Juego;
import modelo.Jugador;
import modelo.Propiedad;
import protocolo.Mensaje;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

/** El servidor pasa el turno solo cuando el jugador en turno queda en bancarrota. */
class ServidorBancarrotaTest {
    private Servidor servidor;
    private Juego juego;
    private Jugador[] jugadores;

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

    @BeforeEach void preparar() throws Exception {
        servidor = new Servidor();
        juego = new Juego(20, 4, "TEST_BANCARROTA_SERVIDOR", false);
        jugadores = (Jugador[]) campo("jugadores");
        int[] computadoras = (int[]) campo("computadoraDe");
        for (int i = 0; i < 4; i++) {
            jugadores[i] = new Jugador("J" + i, "Jugador " + i, 1500);
            computadoras[i] = i / 2;
            juego.agregarJugador(jugadores[i]);
        }
        asignar("juego", juego);
        asignar("tablero", juego.getTablero());
    }
    @AfterEach void limpiar() throws Exception {
        Files.deleteIfExists(juego.getArchivoTransacciones());
    }

    @Test void siQuiebraEnSuTiroElServidorPasaElTurnoSolo() throws Exception {
        Propiedad aeropuerto = (Propiedad) juego.getTablero().obtenerCasilla(5);
        assertTrue(juego.getBanco().comprarPropiedad(jugadores[1], aeropuerto));
        jugadores[0].setSaldo(10);
        jugadores[0].setPosicionActual(2);

        assertNull(invocar("aplicarDados", new Class<?>[]{int.class, int.class}, 1, 2));

        assertSame(jugadores[1], juego.getJugadorActual());
        assertEquals(false, campo("yaTiro"));
        Mensaje.EstadoJuego estado = (Mensaje.EstadoJuego) invocar("crearEstado", new Class<?>[0]);
        assertFalse(estado.activos()[0]);
        assertEquals(1, estado.enTurno());
        assertEquals(-1, estado.ganador());
    }

    @Test void elEstadoFinalTraeAlGanador() throws Exception {
        for (int i = 1; i < 4; i++) jugadores[i].setSaldo(0);
        jugadores[0].setSaldo(5000);
        // Cumpleaños: los tres sin saldo quiebran y queda solo el jugador 0
        while (juego.getMazoArcaComunal().verFrente().getId() != 8) {
            juego.getMazoArcaComunal().encolar(juego.getMazoArcaComunal().desencolar());
        }
        invocar("aplicarDados", new Class<?>[]{int.class, int.class}, 1, 1);

        Mensaje.EstadoJuego estado = (Mensaje.EstadoJuego) invocar("crearEstado", new Class<?>[0]);
        assertTrue(estado.terminado());
        assertEquals(0, estado.ganador());
        assertFalse(estado.puedeComprar());
        assertEquals(5000, estado.patrimonios()[0]);
    }
}
