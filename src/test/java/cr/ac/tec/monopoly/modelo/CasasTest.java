package cr.ac.tec.monopoly.modelo;

import modelo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class CasasTest {
    @TempDir Path carpeta;
    private final Tablero tablero = new Tablero();
    private final Banco banco = new Banco();
    private final Jugador ana = new Jugador("A", "Ana", 10000);
    private final Jugador jose = new Jugador("J", "José", 10000);

    private Propiedad propiedad(int id) { return (Propiedad) tablero.obtenerCasilla(id); }
    private void comprarGrupoCafe() {
        assertTrue(banco.comprarPropiedad(ana, propiedad(1)));
        assertTrue(banco.comprarPropiedad(ana, propiedad(3)));
    }

    @Test void exigeGrupoCompletoYConstruccionUniforme() {
        assertTrue(banco.comprarPropiedad(ana, propiedad(1)));
        int saldo = ana.getSaldo();
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        assertEquals(saldo, ana.getSaldo());
        assertEquals(32, banco.getCasasDisponibles());
        assertTrue(banco.comprarPropiedad(ana, propiedad(3)));
        assertTrue(banco.comprarCasa(ana, propiedad(1)));
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        assertTrue(banco.comprarCasa(ana, propiedad(3)));
        assertTrue(banco.comprarCasa(ana, propiedad(1)));
        assertEquals(2, propiedad(1).getCantidadCasas());
        assertEquals(29, banco.getCasasDisponibles());
    }

    @Test void rentasClasicasCobroYRegistroHastaCuatroCasas() {
        RegistroTransacciones registro = new RegistroTransacciones("CASAS", carpeta);
        banco.setRegistroTransacciones(registro);
        comprarGrupoCafe();
        assertEquals(4, propiedad(1).calcularAlquiler());
        int[] rentas = {10, 30, 90, 160};
        for (int renta : rentas) {
            assertTrue(banco.comprarCasa(ana, propiedad(1)));
            assertTrue(banco.comprarCasa(ana, propiedad(3)));
            assertEquals(renta, propiedad(1).calcularAlquiler());
            int antesAna = ana.getSaldo(), antesJose = jose.getSaldo();
            assertTrue(banco.cobrarAlquiler(jose, propiedad(1)));
            assertEquals(antesAna + renta, ana.getSaldo());
            assertEquals(antesJose - renta, jose.getSaldo());
        }
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        assertEquals(8, registro.getHistorial().buscarPorTipo(TipoTransaccion.COMPRA_CASA).length);
        assertEquals(50, registro.getHistorial().buscarPorTipo(TipoTransaccion.COMPRA_CASA)[0].monto());
        assertEquals(4, registro.getHistorial().buscarPorTipo(TipoTransaccion.PAGO_ALQUILER).length);
    }

    @Test void rechazaAjenaHipotecaSaldoInsuficienteYSinExistencias() {
        comprarGrupoCafe();
        assertFalse(banco.comprarCasa(jose, propiedad(1)));
        assertTrue(propiedad(3).hipotecar());
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        assertEquals(0, propiedad(3).calcularAlquiler());
        assertTrue(propiedad(3).quitarHipoteca());
        ana.setSaldo(49);
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        assertEquals(49, ana.getSaldo());
        ana.setSaldo(1000);
        assertFalse(new Banco(1000, 0, 12).comprarCasa(ana, propiedad(1)));
        assertEquals(0, propiedad(1).getCantidadCasas());
        assertEquals(32, banco.getCasasDisponibles());
    }

    @Test void noConstruyeEnServiciosNiTransportesYPreciosSonPorGrupo() {
        for (int id : new int[]{5, 12}) {
            assertTrue(banco.comprarPropiedad(ana, propiedad(id)));
            assertFalse(banco.comprarCasa(ana, propiedad(id)));
            assertFalse(propiedad(id).agregarCasa());
        }
        assertEquals(50, propiedad(6).getPrecioCasa());
        assertEquals(100, propiedad(11).getPrecioCasa());
        assertEquals(150, propiedad(21).getPrecioCasa());
        assertEquals(200, propiedad(31).getPrecioCasa());
        for (int i = 0; i < 4; i++) assertTrue(propiedad(39).agregarCasa());
        assertEquals(1700, propiedad(39).calcularAlquiler());
    }
}
