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
        ana.registrarRecorrido(40);
    }

    @Test void construyeEnUnaSolaPropiedadTrasUnaVueltaSinExigirGrupoNiUniformidad() {
        ana.setPosicionActual(34);
        assertTrue(banco.comprarPropiedad(ana, propiedad(1)));
        int saldo = ana.getSaldo();
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        ana.registrarRecorrido(39); // Ya pasó por Salida, pero falta una casilla de recorrido.
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        assertEquals(saldo, ana.getSaldo());
        assertEquals(32, banco.getCasasDisponibles());
        ana.registrarRecorrido(1);
        assertTrue(banco.comprarCasa(ana, propiedad(1)));
        for (int i = 0; i < 3; i++) assertTrue(banco.comprarCasa(ana, propiedad(1)));
        assertEquals(4, propiedad(1).getCantidadCasas());
        assertEquals(0, propiedad(3).getCantidadCasas());
        assertNull(propiedad(3).getPropietario());
        assertEquals(28, banco.getCasasDisponibles());
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
    }

    @Test void cadaCompraTieneSuPropiaVueltaYNoCuentaRecorridoAnterior() {
        ana.registrarRecorrido(80);
        assertTrue(banco.comprarPropiedad(ana, propiedad(1)));
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        ana.registrarRecorrido(20);
        assertTrue(banco.comprarPropiedad(ana, propiedad(3)));
        ana.registrarRecorrido(20);
        assertTrue(banco.comprarCasa(ana, propiedad(1)));
        assertFalse(banco.comprarCasa(ana, propiedad(3)));
        ana.registrarRecorrido(20);
        assertTrue(banco.comprarCasa(ana, propiedad(3)));
    }

    @Test void retrocederDescuentaRecorridoYCarcelNoCompletaVuelta() {
        assertTrue(banco.comprarPropiedad(ana, propiedad(1)));
        ana.registrarRecorrido(38);
        ana.registrarRecorrido(-3);
        ana.enviarACarcel(10);
        ana.setPosicionActual(39);
        ana.registrarRecorrido(1);
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        ana.registrarRecorrido(4);
        assertTrue(banco.comprarCasa(ana, propiedad(1)));
        ana.registrarRecorrido(-3);
        assertTrue(banco.comprarCasa(ana, propiedad(1))); // Una vuelta cumplida sigue habilitada.
    }

    @Test void hipotecaDeOtraPropiedadNoImpideConstruirYCambioDeDuenoReiniciaEspera() {
        comprarGrupoCafe();
        assertTrue(propiedad(3).hipotecar());
        assertTrue(banco.comprarCasa(ana, propiedad(1)));
        propiedad(1).setPropietario(jose);
        jose.agregarPropiedad(propiedad(1));
        ana.registrarRecorrido(40);
        assertFalse(banco.comprarCasa(jose, propiedad(1)));
        jose.registrarRecorrido(40);
        assertTrue(banco.comprarCasa(jose, propiedad(1)));
    }

    @Test void cartaDeRetrocesoDescuentaRecorridoEnElMotorDelJuego() throws Exception {
        Juego juego = new Juego(20, 2, "TEST_RECORRIDO", false);
        try {
            juego.agregarJugador(ana);
            juego.agregarJugador(jose);
            Propiedad sixaola = (Propiedad) juego.getTablero().obtenerCasilla(1);
            assertTrue(juego.getBanco().comprarPropiedad(ana, sixaola));
            ana.registrarRecorrido(30);
            ana.setPosicionActual(4);
            while (juego.getMazoSuerte().verFrente().getId() != 17) {
                juego.getMazoSuerte().encolar(juego.getMazoSuerte().desencolar());
            }
            juego.jugarTiro(1, 2); // Avanza a Suerte y retrocede tres: recorrido neto sigue en 30.
            ana.registrarRecorrido(9);
            assertFalse(juego.getBanco().comprarCasa(ana, sixaola));
            ana.registrarRecorrido(1);
            assertTrue(juego.getBanco().comprarCasa(ana, sixaola));
        } finally { java.nio.file.Files.deleteIfExists(juego.getArchivoTransacciones()); }
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
        assertTrue(propiedad(1).hipotecar());
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        assertEquals(0, propiedad(1).calcularAlquiler());
        assertTrue(propiedad(1).quitarHipoteca());
        ana.setSaldo(49);
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        assertEquals(49, ana.getSaldo());
        ana.setSaldo(1000);
        assertFalse(new Banco(1000, 0, 12).comprarCasa(ana, propiedad(1)));
        assertEquals(0, propiedad(1).getCantidadCasas());
        assertEquals(32, banco.getCasasDisponibles());
    }

    @Test void hotelReemplazaCuatroCasasDevuelveExistenciasYCobraRentaDeHotel() {
        RegistroTransacciones registro = new RegistroTransacciones("HOTEL", carpeta);
        banco.setRegistroTransacciones(registro);
        assertTrue(banco.comprarPropiedad(ana, propiedad(1)));
        ana.registrarRecorrido(40);
        for (int i = 0; i < 4; i++) assertTrue(banco.comprarCasa(ana, propiedad(1)));
        int saldo = ana.getSaldo();
        int saldoBanco = banco.getSaldo();
        assertTrue(banco.comprarHotel(ana, propiedad(1)));
        assertEquals(saldo - 50, ana.getSaldo());
        assertEquals(saldoBanco + 50, banco.getSaldo());
        assertTrue(propiedad(1).tieneHotel());
        assertEquals(0, propiedad(1).getCantidadCasas());
        assertEquals(32, banco.getCasasDisponibles());
        assertEquals(11, banco.getHotelesDisponibles());
        assertEquals(250, propiedad(1).calcularAlquiler());
        assertFalse(banco.comprarCasa(ana, propiedad(1)));
        assertFalse(banco.comprarHotel(ana, propiedad(1)));
        int antesJose = jose.getSaldo();
        assertTrue(banco.cobrarAlquiler(jose, propiedad(1)));
        assertEquals(antesJose - 250, jose.getSaldo());
        assertEquals(saldo - 50 + 250, ana.getSaldo());
        assertEquals(1, registro.getHistorial().buscarPorTipo(TipoTransaccion.COMPRA_HOTEL).length);
        assertEquals(50, registro.getHistorial().buscarPorTipo(TipoTransaccion.COMPRA_HOTEL)[0].monto());
    }

    @Test void hotelRechazaMenosDeCuatroCasasOtroDuenoSaldoInsuficienteYSinExistencias() {
        assertTrue(banco.comprarPropiedad(ana, propiedad(1)));
        ana.registrarRecorrido(40);
        for (int i = 0; i < 3; i++) assertTrue(banco.comprarCasa(ana, propiedad(1)));
        assertFalse(banco.comprarHotel(ana, propiedad(1)));
        assertTrue(banco.comprarCasa(ana, propiedad(1)));
        assertFalse(banco.comprarHotel(jose, propiedad(1)));
        ana.setSaldo(49);
        assertFalse(banco.comprarHotel(ana, propiedad(1)));
        assertEquals(49, ana.getSaldo());
        ana.setSaldo(500);
        assertFalse(new Banco(1000, 32, 0).comprarHotel(ana, propiedad(1)));
        assertEquals(500, ana.getSaldo());
        assertEquals(4, propiedad(1).getCantidadCasas());
        assertFalse(propiedad(1).tieneHotel());
        assertEquals(28, banco.getCasasDisponibles());
        assertEquals(12, banco.getHotelesDisponibles());
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
