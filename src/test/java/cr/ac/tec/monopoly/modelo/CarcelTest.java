package cr.ac.tec.monopoly.modelo;

import config.Configuracion;
import estructuras.ColaCircular;
import modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class CarcelTest {

    private Juego juego;
    private Jugador ana;

    @BeforeEach
    void crearPartida() {
        juego = new Juego(100, 2, "PRUEBA_CARCEL", false);
        ana = new Jugador("J1", "Ana", 1500);
        juego.agregarJugador(ana);
        juego.agregarJugador(new Jugador("J2", "José", 1500));
    }

    @AfterEach
    void borrarArchivo() throws Exception {
        Files.deleteIfExists(juego.getArchivoTransacciones());
    }

    @Test
    void caerEnVayaALaCarcelLoMandaPresoSinCobrarSalida() {
        ana.setPosicionActual(25);
        juego.jugarTiro(2, 3); // 25 + 5 = 30, "Vaya a la cárcel"

        assertTrue(ana.isEnCarcel());
        assertEquals(Configuracion.POSICION_CARCEL, ana.getPosicionActual());
        assertEquals(1500, ana.getSaldo()); // de 30 a 10 "hacia atrás", pero no cobra Salida
    }

    @Test
    void caerEnLaCarcelDeVisitaNoLoMandaPreso() {
        ana.setPosicionActual(6);
        juego.jugarTiro(1, 3); // 6 + 4 = 10
        assertFalse(ana.isEnCarcel());
    }

    @Test
    void conDoblesSaleYAvanzaLaSuma() {
        ana.enviarACarcel(Configuracion.POSICION_CARCEL);
        juego.jugarTiro(3, 3);

        assertFalse(ana.isEnCarcel());
        assertEquals(16, ana.getPosicionActual());
    }

    @Test
    void sinDoblesSeQuedaQuieto() {
        ana.enviarACarcel(Configuracion.POSICION_CARCEL);
        juego.jugarTiro(1, 2);

        assertTrue(ana.isEnCarcel());
        assertEquals(Configuracion.POSICION_CARCEL, ana.getPosicionActual());
        assertEquals(1, ana.getTurnosEnCarcel());
    }

    @Test
    void alTercerIntentoPagaLaMultaYAvanza() {
        ana.enviarACarcel(Configuracion.POSICION_CARCEL);
        juego.jugarTiro(1, 2);
        juego.jugarTiro(1, 2);
        assertTrue(ana.isEnCarcel());

        juego.jugarTiro(1, 2); // tercer intento: paga y sale

        assertFalse(ana.isEnCarcel());
        assertEquals(13, ana.getPosicionActual());
        assertEquals(1500 - Configuracion.MULTA_CARCEL, ana.getSaldo());
    }

    @Test
    void siNoLeAlcanzaParaLaMultaSigueEnLaCarcel() {
        ana.setSaldo(10);
        ana.enviarACarcel(Configuracion.POSICION_CARCEL);
        juego.jugarTiro(1, 2);
        juego.jugarTiro(1, 2);
        juego.jugarTiro(1, 2);

        assertTrue(ana.isEnCarcel());
        assertEquals(10, ana.getSaldo());
    }

    @Test
    void pagarLaMultaLoLibera() {
        ana.enviarACarcel(Configuracion.POSICION_CARCEL);

        assertTrue(juego.pagarMultaCarcel());
        assertFalse(ana.isEnCarcel());
        assertEquals(1500 - Configuracion.MULTA_CARCEL, ana.getSaldo());
    }

    @Test
    void noSePuedePagarMultaSinEstarPreso() {
        assertFalse(juego.pagarMultaCarcel());
        assertEquals(1500, ana.getSaldo());
    }

    @Test
    void usarLaCartaLoLiberaYLaDevuelveAlMazo() {
        ColaCircular<CartaEvento> mazo = juego.getMazoArcaComunal();
        // Simula que Ana ya sacó la carta "Salga de la cárcel" (id 5) y se la quedó
        while (mazo.verFrente().getAccion() != CartaEvento.Accion.SALIR_DE_CARCEL) {
            mazo.encolar(mazo.desencolar());
        }
        ana.guardarCartaSalirCarcel(mazo.desencolar());
        assertEquals(9, mazo.tamaño());

        ana.enviarACarcel(Configuracion.POSICION_CARCEL);
        assertTrue(juego.usarCartaSalirCarcel());

        assertFalse(ana.isEnCarcel());
        assertEquals(0, ana.getCantidadCartasSalirCarcel());
        assertEquals(10, mazo.tamaño());
        assertEquals(1500, ana.getSaldo());
    }

    @Test
    void sinCartaNoPuedeUsarla() {
        ana.enviarACarcel(Configuracion.POSICION_CARCEL);
        assertFalse(juego.usarCartaSalirCarcel());
        assertTrue(ana.isEnCarcel());
    }
}
