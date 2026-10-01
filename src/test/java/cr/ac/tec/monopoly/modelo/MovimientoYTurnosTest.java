package cr.ac.tec.monopoly.modelo;

import config.Configuracion;
import modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/** Movimiento caminando por los nodos del tablero y turnos sobre ColaCircular. */
class MovimientoYTurnosTest {
    private Juego juego;
    private Jugador ana;
    private Jugador beto;
    private Jugador caro;
    private Jugador dani;

    @BeforeEach
    void preparar() {
        juego = new Juego(20, 4, "PRUEBA_MOVIMIENTO", false);
        ana = new Jugador("J1", "Ana", 1500);
        beto = new Jugador("J2", "Beto", 1500);
        caro = new Jugador("J3", "Caro", 1500);
        dani = new Jugador("J4", "Dani", 1500);
        juego.agregarJugador(ana);
        juego.agregarJugador(beto);
        juego.agregarJugador(caro);
        juego.agregarJugador(dani);
    }

    @AfterEach
    void borrarArchivo() throws Exception {
        Files.deleteIfExists(juego.getArchivoTransacciones());
    }

    @Test
    void losTurnosRotanEnElOrdenDeEntrada() {
        Jugador[] esperado = {ana, beto, caro, dani, ana, beto};
        for (Jugador jugador : esperado) {
            assertSame(jugador, juego.getJugadorActual());
            juego.terminarTurnoActual();
        }
    }

    @Test
    void noSeAceptanMasJugadoresQueLaCapacidad() {
        assertThrows(IllegalStateException.class,
                () -> juego.agregarJugador(new Jugador("J5", "Eva", 1500)));
    }

    @Test
    void avanzarSinPasarPorSalidaNoCobra() {
        juego.jugarTiro(2, 3); // de Salida (0) al Aeropuerto Juan Santamaría (5)
        assertEquals(5, ana.getPosicionActual());
        assertEquals(1500, ana.getSaldo());
    }

    @Test
    void pasarPorSalidaCobraElPremio() {
        ana.setPosicionActual(35);
        juego.jugarTiro(3, 3); // 35 + 6: pasa por Salida y cae en Sixaola (1)
        assertEquals(1, ana.getPosicionActual());
        assertEquals(1500 + Configuracion.PREMIO_SALIDA, ana.getSaldo());
    }

    @Test
    void caerExactamenteEnSalidaTambienCobra() {
        ana.setPosicionActual(34);
        juego.jugarTiro(3, 3); // 34 + 6 = Salida
        assertEquals(0, ana.getPosicionActual());
        assertEquals(1500 + Configuracion.PREMIO_SALIDA, ana.getSaldo());
    }

    @Test
    void unaCartaQueRecorreATodosNoCambiaElTurno() {
        // "Es su cumpleaños": recorre la cola de turnos para cobrarle a cada uno
        while (juego.getMazoArcaComunal().verFrente().getId() != 8) {
            juego.getMazoArcaComunal().encolar(juego.getMazoArcaComunal().desencolar());
        }
        juego.jugarTiro(1, 1); // Salida → Arca Comunal (2)
        assertEquals(1500 + 30, ana.getSaldo());
        assertEquals(1500 - 10, beto.getSaldo());

        // La cola quedó igual: sigue Ana, y después el orden normal
        assertSame(ana, juego.getJugadorActual());
        juego.terminarTurnoActual();
        assertSame(beto, juego.getJugadorActual());
        juego.terminarTurnoActual();
        assertSame(caro, juego.getJugadorActual());
    }

    @Test
    void elHistorialSeRecorreEnLosDosSentidos() {
        ana.setPosicionActual(35);
        juego.jugarTiro(3, 3); // genera un PREMIO_INICIO después de los 4 saldos iniciales
        Transaccion[] antiguas = juego.getRegistroTransacciones().getHistorial().desdeLaMasAntigua();
        Transaccion[] recientes = juego.getRegistroTransacciones().getHistorial().desdeLaMasReciente();
        assertEquals(5, antiguas.length);
        assertEquals(TipoTransaccion.SALDO_INICIAL, antiguas[0].tipo());
        assertEquals(TipoTransaccion.PREMIO_INICIO, recientes[0].tipo());
        for (int i = 0; i < antiguas.length; i++) {
            assertSame(antiguas[i], recientes[antiguas.length - 1 - i]);
        }
    }
}
