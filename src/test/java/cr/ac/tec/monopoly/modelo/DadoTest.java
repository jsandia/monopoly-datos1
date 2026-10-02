package cr.ac.tec.monopoly.modelo;

import modelo.Dado;
import modelo.Juego;
import modelo.Jugador;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DadoTest {

    @Test
    void unDadoNuevoTodaviaNoTieneCara() {
        assertEquals(0, new Dado().getValor());
    }

    @Test
    void lanzarSiempreDaDeUnoASeis() {
        Dado dado = new Dado(new Random(42));
        boolean[] salio = new boolean[Dado.CARAS + 1];
        for (int i = 0; i < 1000; i++) {
            int cara = dado.lanzar();
            assertTrue(cara >= 1 && cara <= 6, "salió " + cara);
            assertEquals(cara, dado.getValor());
            salio[cara] = true;
        }
        // En 1000 tiradas tienen que haber salido las seis caras
        for (int cara = 1; cara <= 6; cara++) {
            assertTrue(salio[cara], "nunca salió " + cara);
        }
    }

    @Test
    void conLaMismaSemillaSaleLaMismaSecuencia() {
        Dado a = new Dado(new Random(7));
        Dado b = new Dado(new Random(7));
        for (int i = 0; i < 20; i++) {
            assertEquals(a.lanzar(), b.lanzar());
        }
    }

    @Test
    void registrarValorGuardaLaCaraDelDadoFisico() {
        Dado dado = new Dado();
        dado.registrarValor(4);
        assertEquals(4, dado.getValor());
    }

    @Test
    void registrarUnValorImposibleLanzaExcepcionYNoCambiaLaCara() {
        Dado dado = new Dado();
        dado.registrarValor(3);
        assertThrows(IllegalArgumentException.class, () -> dado.registrarValor(0));
        assertThrows(IllegalArgumentException.class, () -> dado.registrarValor(7));
        assertEquals(3, dado.getValor());
    }

    @Test
    void esValidoSoloDeUnoASeis() {
        for (int cara = 1; cara <= 6; cara++) {
            assertTrue(Dado.esValido(cara));
        }
        assertFalse(Dado.esValido(0));
        assertFalse(Dado.esValido(7));
        assertFalse(Dado.esValido(-1));
    }

    @Test
    void elTiroQuedaGuardadoEnLosDadosDelJuego() throws Exception {
        Juego juego = new Juego(20, 2, "PRUEBA_DADOS", false);
        try {
            juego.agregarJugador(new Jugador("J1", "Ana", 1500));
            juego.agregarJugador(new Jugador("J2", "Beto", 1500));

            juego.jugarTiro(2, 3); // como si viniera del cajero
            assertEquals(2, juego.getDado1().getValor());
            assertEquals(3, juego.getDado2().getValor());

            int[] caras = juego.lanzarDados(); // botón de dados de prueba
            assertEquals(caras[0], juego.getDado1().getValor());
            assertEquals(caras[1], juego.getDado2().getValor());
            assertTrue(Dado.esValido(caras[0]) && Dado.esValido(caras[1]));
        } finally {
            Files.deleteIfExists(juego.getArchivoTransacciones());
        }
    }

    @Test
    void jugarTiroConUnaCaraImposibleLanzaExcepcion() throws Exception {
        Juego juego = new Juego(20, 2, "PRUEBA_DADOS_MAL", false);
        try {
            Jugador ana = new Jugador("J1", "Ana", 1500);
            juego.agregarJugador(ana);
            juego.agregarJugador(new Jugador("J2", "Beto", 1500));

            assertThrows(IllegalArgumentException.class, () -> juego.jugarTiro(0, 9));
            assertEquals(0, ana.getPosicionActual()); // no se movió
        } finally {
            Files.deleteIfExists(juego.getArchivoTransacciones());
        }
    }
}
