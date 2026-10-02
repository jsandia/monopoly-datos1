import modelo.Juego;
import modelo.Jugador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

/** Lo que el servidor le contesta al cajero cuando llegan dados. */
class ServidorDadosTest {
    private Servidor servidor;
    private Juego juego;

    private void asignar(String nombre, Object valor) throws Exception {
        Field campo = Servidor.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(servidor, valor);
    }
    private Object campo(String nombre) throws Exception {
        Field campo = Servidor.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        return campo.get(servidor);
    }

    @BeforeEach void preparar() throws Exception {
        servidor = new Servidor();
        juego = new Juego(20, 4, "TEST_DADOS_SERVIDOR", false);
        Jugador[] jugadores = (Jugador[]) campo("jugadores");
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

    @Test void sinPartidaContestaNoHayPartida() {
        assertEquals("NO_HAY_PARTIDA", new Servidor().recibirDadosDelCajero(3, 4));
    }

    @Test void unDadoFueraDeRangoSeRechazaSinMoverANadie() {
        assertEquals("DADO_INVALIDO", servidor.recibirDadosDelCajero(0, 4));
        assertEquals("DADO_INVALIDO", servidor.recibirDadosDelCajero(3, 7));
        assertEquals(0, juego.getJugadorActual().getPosicionActual());
    }

    @Test void unTiroValidoSeAplicaYQuedaEnLosDados() {
        assertNull(servidor.recibirDadosDelCajero(2, 3));
        assertEquals(5, juego.getJugadorActual().getPosicionActual());
        assertEquals(2, juego.getDado1().getValor());
        assertEquals(3, juego.getDado2().getValor());
    }

    @Test void noSePuedeTirarDosVecesEnElMismoTurno() {
        assertNull(servidor.recibirDadosDelCajero(2, 3));
        assertEquals("YA_TIRO", servidor.recibirDadosDelCajero(1, 1));
        assertEquals(5, juego.getJugadorActual().getPosicionActual());
    }
}
