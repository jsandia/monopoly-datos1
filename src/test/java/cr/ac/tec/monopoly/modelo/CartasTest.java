package cr.ac.tec.monopoly.modelo;

import estructuras.ColaCircular;
import modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cartas de Suerte y Arca Comunal aplicadas al caer en sus casillas.
 * Los mazos se crean sin barajar y cada prueba pone al frente la carta que necesita.
 */
class CartasTest {

    // Casillas del tablero
    private static final int SUERTE_7 = 7, ARCA_17 = 17, SUERTE_36 = 36;

    private Juego juego;
    private Jugador ana, jose, luis;

    @BeforeEach
    void crearPartida() {
        juego = new Juego(100, 3, "PRUEBA_CARTAS", false);
        ana = new Jugador("J1", "Ana", 1500);
        jose = new Jugador("J2", "José", 1500);
        luis = new Jugador("J3", "Luis", 1500);
        juego.agregarJugador(ana); // Ana tiene el turno
        juego.agregarJugador(jose);
        juego.agregarJugador(luis);
    }

    @AfterEach
    void borrarArchivo() throws Exception {
        Files.deleteIfExists(juego.getArchivoTransacciones());
    }

    // Rota el mazo hasta que la carta con ese id quede al frente
    private static void ponerAlFrente(ColaCircular<CartaEvento> mazo, int id) {
        while (mazo.verFrente().getId() != id) {
            mazo.encolar(mazo.desencolar());
        }
    }

    // Deja a Ana 3 casillas antes de "destino" y tira [1][2] para caer ahí
    private void caerEn(int destino) {
        ana.setPosicionActual(destino - 3);
        juego.jugarTiro(1, 2);
    }

    @Test
    void laCartaUsadaPasaAlFinalDelMazo() {
        ColaCircular<CartaEvento> mazo = juego.getMazoArcaComunal();
        ponerAlFrente(mazo, 7); // "Cobre ₡100"
        caerEn(ARCA_17);

        assertEquals(1600, ana.getSaldo());
        assertEquals(8, mazo.verFrente().getId());
        assertEquals(10, mazo.tamaño());
    }

    @Test
    void avanzarASalidaCobraElPremio() {
        ponerAlFrente(juego.getMazoArcaComunal(), 1);
        caerEn(ARCA_17);

        assertEquals(0, ana.getPosicionActual());
        assertEquals(1500 + Juego.PREMIO_SALIDA, ana.getSaldo());
    }

    @Test
    void irAUnaCasillaDandoLaVueltaCobraSalida() {
        ponerAlFrente(juego.getMazoSuerte(), 12); // Monteverde (24)
        caerEn(SUERTE_36);

        assertEquals(24, ana.getPosicionActual());
        assertEquals(1500 + Juego.PREMIO_SALIDA, ana.getSaldo());
    }

    @Test
    void irAUnaCasillaSinDarLaVueltaNoCobra() {
        ponerAlFrente(juego.getMazoSuerte(), 12); // Monteverde (24)
        caerEn(SUERTE_7);

        assertEquals(24, ana.getPosicionActual());
        assertEquals(1500, ana.getSaldo());
    }

    @Test
    void transporteMasCercanoAvanzaHastaElSiguiente() {
        ponerAlFrente(juego.getMazoSuerte(), 14);
        caerEn(SUERTE_7);
        assertEquals(15, ana.getPosicionActual()); // Puerto Caldera
    }

    @Test
    void transporteMasCercanoPuedeDarLaVuelta() {
        ponerAlFrente(juego.getMazoSuerte(), 14);
        caerEn(SUERTE_36);

        assertEquals(5, ana.getPosicionActual()); // Aeropuerto Juan Santamaría
        assertEquals(1500 + Juego.PREMIO_SALIDA, ana.getSaldo());
    }

    @Test
    void retrocederUsaAnteriorYResuelveLaNuevaCasilla() {
        ponerAlFrente(juego.getMazoSuerte(), 17);     // retroceda 3: de 36 a 33
        ponerAlFrente(juego.getMazoArcaComunal(), 7); // en 33 (Arca Comunal) cobra ₡100
        caerEn(SUERTE_36);

        assertEquals(33, ana.getPosicionActual());
        assertEquals(1600, ana.getSaldo());
    }

    @Test
    void irALaCarcelPorCarta() {
        ponerAlFrente(juego.getMazoArcaComunal(), 6);
        caerEn(ARCA_17);

        assertTrue(ana.isEnCarcel());
        assertEquals(Juego.POSICION_CARCEL, ana.getPosicionActual());
        assertEquals(1500, ana.getSaldo());
    }

    @Test
    void laCartaDeSalirDeLaCarcelSeLaQuedaElJugador() {
        ponerAlFrente(juego.getMazoSuerte(), 16);
        caerEn(SUERTE_7);

        assertEquals(1, ana.getCantidadCartasSalirCarcel());
        assertEquals(9, juego.getMazoSuerte().tamaño()); // no volvió al mazo
    }

    @Test
    void cobrarleACadaJugador() {
        ponerAlFrente(juego.getMazoArcaComunal(), 8); // ₡10 de cada jugador
        caerEn(ARCA_17);

        assertEquals(1520, ana.getSaldo());
        assertEquals(1490, jose.getSaldo());
        assertEquals(1490, luis.getSaldo());
    }

    @Test
    void pagarleACadaJugador() {
        ponerAlFrente(juego.getMazoSuerte(), 20); // ₡50 a cada jugador
        caerEn(SUERTE_7);

        assertEquals(1400, ana.getSaldo());
        assertEquals(1550, jose.getSaldo());
        assertEquals(1550, luis.getSaldo());
    }

    @Test
    void reparacionesCobraPorCadaCasa() {
        Propiedad liberia = (Propiedad) juego.getTablero().obtenerCasilla(16);
        liberia.agregarCasa();
        liberia.agregarCasa();
        ana.agregarPropiedad(liberia);

        ponerAlFrente(juego.getMazoArcaComunal(), 10); // ₡40 por casa
        caerEn(ARCA_17);

        assertEquals(1500 - 2 * 40, ana.getSaldo());
    }
}
