package cr.ac.tec.monopoly.modelo;

import modelo.Jugador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JugadorTest {

    @Test
    void debitarRestaSoloSiLeAlcanza() {
        Jugador ana = new Jugador("J1", "Ana", 100);
        assertTrue(ana.debitar(100));
        assertEquals(0, ana.getSaldo());
        assertFalse(ana.debitar(1));
        assertEquals(0, ana.getSaldo());
    }

    @Test
    void debitarUnMontoNegativoOCeroNoCambiaElSaldo() {
        Jugador ana = new Jugador("J1", "Ana", 100);
        assertFalse(ana.debitar(-50)); // antes sumaba 50
        assertFalse(ana.debitar(0));
        assertEquals(100, ana.getSaldo());
    }

    @Test
    void acreditarIgnoraMontosQueNoSonPositivos() {
        Jugador ana = new Jugador("J1", "Ana", 100);
        ana.acreditar(-30);
        ana.acreditar(0);
        assertEquals(100, ana.getSaldo());
        ana.acreditar(30);
        assertEquals(130, ana.getSaldo());
    }

    @Test
    void laTarjetaSeValidaSinImportarMayusculasNiEspacios() {
        Jugador ana = new Jugador("J1", "Ana", 100);
        assertFalse(ana.tieneTarjetaRegistrada());
        ana.registrarTarjeta("  a3f29b11 ");
        assertTrue(ana.tieneTarjetaRegistrada());
        assertEquals("A3F29B11", ana.getUidTarjeta());
        assertTrue(ana.validarTarjeta("A3F29B11"));
        assertTrue(ana.validarTarjeta("a3f29b11"));
        assertFalse(ana.validarTarjeta("OTRA"));
        assertFalse(ana.validarTarjeta(null));
    }
}
