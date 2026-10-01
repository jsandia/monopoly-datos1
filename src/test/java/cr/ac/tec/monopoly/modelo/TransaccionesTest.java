package cr.ac.tec.monopoly.modelo;

import modelo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TransaccionesTest {
    @TempDir Path carpeta;
    private RegistroTransacciones registro;
    private Banco banco;
    private Jugador ana;
    private Jugador jose;

    @BeforeEach
    void preparar() {
        registro = new RegistroTransacciones("PRUEBA", carpeta);
        banco = new Banco();
        banco.setRegistroTransacciones(registro);
        ana = new Jugador("J1", "Ana", 10000);
        jose = new Jugador("J2", "José", 10000);
    }

    @Test
    void compraConTarjetaContieneTodosLosCamposYElTurno() throws Exception {
        registro.setTurno(7);
        ana.registrarTarjeta("ABC123");
        Propiedad propiedad = new Propiedad(1, "San José", "Grupo", 1000, 100);
        assertFalse(banco.comprarPropiedadConTarjeta(ana, propiedad, "INCORRECTA"));
        assertEquals(0, registro.getHistorial().getCantidad());
        assertTrue(banco.comprarPropiedadConTarjeta(ana, propiedad, "ABC123"));
        Transaccion t = registro.getHistorial().desdeLaMasAntigua()[0];
        assertEquals(1, t.numero());
        assertNotNull(t.fechaHora());
        assertEquals(7, t.turno());
        assertEquals(TipoTransaccion.COMPRA_PROPIEDAD, t.tipo());
        assertEquals("J1", t.idOrigen());
        assertEquals("BANCO", t.idDestino());
        assertEquals(1000, t.monto());
        assertEquals("San José", t.descripcion());
        assertEquals(10000, t.saldoOrigenAnterior());
        assertEquals(9000, t.saldoOrigenPosterior());
        assertEquals(1000000, t.saldoDestinoAnterior());
        assertEquals(1001000, t.saldoDestinoPosterior());
        assertTrue(Files.readString(registro.getArchivo()).contains(t.formatear()));
    }

    @Test
    void alquilerEsUnaSolaTransaccionQueSeEncuentraPorAmbosJugadores() {
        Propiedad propiedad = new Propiedad(1, "San José", "Grupo", 1000, 100);
        assertTrue(banco.comprarPropiedad(ana, propiedad));
        int saldoBanco = banco.getSaldo();
        assertTrue(banco.cobrarAlquiler(jose, propiedad));
        Transaccion[] alquileres = registro.getHistorial().buscarPorTipo(TipoTransaccion.PAGO_ALQUILER);
        assertEquals(1, alquileres.length);
        Transaccion t = alquileres[0];
        assertEquals("J2", t.idOrigen());
        assertEquals("J1", t.idDestino());
        assertEquals(100, t.monto());
        assertEquals(10000, t.saldoOrigenAnterior());
        assertEquals(9900, t.saldoOrigenPosterior());
        assertEquals(9000, t.saldoDestinoAnterior());
        assertEquals(9100, t.saldoDestinoPosterior());
        assertEquals(saldoBanco, banco.getSaldo());
        assertEquals(2, registro.getHistorial().buscarPorJugador("J1").length);
        assertEquals(1, registro.getHistorial().buscarPorJugador("J2").length);
    }

    @Test
    void seGeneranLosSieteTiposMinimosConOrigenDestinoCorrectos() {
        Propiedad propiedad = new Propiedad(1, "Propiedad", "Grupo", 1000, 100);
        assertTrue(banco.comprarPropiedad(ana, propiedad));
        assertTrue(banco.cobrarAlquiler(jose, propiedad));
        assertTrue(banco.cobrarJugador(ana, 10));
        assertTrue(banco.pagarEntreJugadores(ana, jose, 50, "Pago acordado"));
        assertTrue(banco.pagarGananciaEvento(ana, 200, "Premio de carta"));
        assertTrue(banco.cobrarPerdidaEvento(ana, 100, "Multa de carta"));
        assertTrue(banco.pagarSalida(ana, 200));
        for (TipoTransaccion tipo : new TipoTransaccion[]{TipoTransaccion.COMPRA_PROPIEDAD,
                TipoTransaccion.PAGO_ALQUILER, TipoTransaccion.PAGO_AL_BANCO,
                TipoTransaccion.PAGO_ENTRE_JUGADORES, TipoTransaccion.GANANCIA_EVENTO,
                TipoTransaccion.PERDIDA_EVENTO, TipoTransaccion.PREMIO_INICIO}) {
            assertEquals(1, registro.getHistorial().buscarPorTipo(tipo).length, tipo.name());
        }
        Transaccion ganancia = registro.getHistorial().buscarPorTipo(TipoTransaccion.GANANCIA_EVENTO)[0];
        assertEquals("BANCO", ganancia.idOrigen());
        assertEquals("J1", ganancia.idDestino());
        Transaccion perdida = registro.getHistorial().buscarPorTipo(TipoTransaccion.PERDIDA_EVENTO)[0];
        assertEquals("J1", perdida.idOrigen());
        assertEquals("BANCO", perdida.idDestino());
    }

    @Test
    void historialVacioUnicoYConVariosSeRecorreEnAmbosSentidos() {
        assertEquals(0, registro.getHistorial().desdeLaMasAntigua().length);
        assertEquals(0, registro.getHistorial().desdeLaMasReciente().length);
        assertTrue(banco.cobrarJugador(ana, 10));
        assertEquals(1, registro.getHistorial().desdeLaMasReciente()[0].numero());
        assertTrue(banco.pagarEntreJugadores(ana, jose, 20, "Pago"));
        assertTrue(banco.pagarSalida(jose, 200));
        Transaccion[] antiguas = registro.getHistorial().desdeLaMasAntigua();
        Transaccion[] recientes = registro.getHistorial().desdeLaMasReciente();
        for (int i = 0; i < antiguas.length; i++) {
            assertEquals(i + 1, antiguas[i].numero());
            assertEquals(antiguas[i], recientes[recientes.length - 1 - i]);
        }
        assertEquals(1, registro.getHistorial().consultar("J1", TipoTransaccion.PAGO_ENTRE_JUGADORES, true).length);
        assertEquals(0, registro.getHistorial().buscarPorJugador("DESCONOCIDO").length);
        antiguas[0] = null;
        assertNotNull(registro.getHistorial().desdeLaMasAntigua()[0]);
    }

    @Test
    void pagosFallidosNoCambianSaldosNiGeneranTransacciones() throws Exception {
        String antes = Files.readString(registro.getArchivo());
        assertFalse(banco.cobrarJugador(ana, 100000));
        assertFalse(banco.pagarEntreJugadores(ana, jose, -1, "Pago"));
        assertFalse(banco.pagarEntreJugadores(ana, ana, 1, "Pago"));
        assertFalse(banco.pagarEntreJugadores(ana, jose, 20000, "Pago"));
        assertFalse(banco.pagarGananciaEvento(ana, 2000000, "Premio"));
        assertFalse(banco.cobrarPerdidaEvento(ana, 20000, "Multa"));
        assertEquals(10000, ana.getSaldo());
        assertEquals(10000, jose.getSaldo());
        assertEquals(0, registro.getHistorial().getCantidad());
        assertEquals(antes, Files.readString(registro.getArchivo()));
    }

    @Test
    void exportarIncluyeTodosLosCamposSinModificarElHistorial() throws Exception {
        assertTrue(banco.pagarGananciaEvento(jose, 100, "Carta: premio de José"));
        assertTrue(banco.cobrarJugador(ana, 10));
        String reporte = registro.exportar();
        assertTrue(reporte.contains(RegistroTransacciones.COLUMNAS));
        assertTrue(reporte.contains("Carta: premio de José"));
        assertTrue(reporte.indexOf("GANANCIA_EVENTO") < reporte.indexOf("PAGO_AL_BANCO"));
        Path copia = carpeta.resolve("exportado.txt");
        registro.exportar(copia);
        assertEquals(reporte, Files.readString(copia));
        assertTrue(reporte.endsWith(registro.getHistorial().imprimirTodas()));
        assertEquals(2, registro.getHistorial().getCantidad());
    }

    @Test
    void otrasOperacionesDelBancoTambienQuedanRegistradas() {
        Propiedad propiedad = new Propiedad(1, "Propiedad", "Grupo", 1000, 100);
        assertTrue(banco.comprarPropiedad(ana, propiedad));
        ana.mover(40, 40);
        assertTrue(banco.comprarCasa(ana, propiedad));
        assertTrue(banco.venderCasa(ana, propiedad));
        assertTrue(banco.hipotecarPropiedad(ana, propiedad));
        assertTrue(banco.quitarHipoteca(ana, propiedad));
        for (int i = 0; i < 4; i++) assertTrue(banco.comprarCasa(ana, propiedad));
        assertTrue(banco.comprarHotel(ana, propiedad));
        assertTrue(banco.venderHotel(ana, propiedad));
        assertTrue(banco.cobrarImpuesto(ana, 10));
        assertTrue(banco.pagarJugador(ana, 10));
        assertEquals(13, registro.getHistorial().getCantidad());
        assertEquals(1, registro.getHistorial().buscarPorTipo(TipoTransaccion.COMPRA_HOTEL).length);
        assertEquals(1, registro.getHistorial().buscarPorTipo(TipoTransaccion.CANCELACION_HIPOTECA).length);
    }

    @Test
    void cadaPartidaTieneArchivoYNumeracionIndependientes() {
        RegistroTransacciones otro = new RegistroTransacciones("PRUEBA", carpeta);
        assertNotEquals(registro.getArchivo(), otro.getArchivo());
        assertTrue(banco.cobrarJugador(ana, 10));
        assertEquals(0, otro.getHistorial().getCantidad());
        Banco otroBanco = new Banco();
        otroBanco.setRegistroTransacciones(otro);
        assertTrue(otroBanco.cobrarJugador(jose, 10));
        assertEquals(1, otro.getHistorial().desdeLaMasAntigua()[0].numero());
    }

    @Test
    void juegoActualizaElTurnoDeLasTransacciones() throws Exception {
        Juego juego = new Juego(2, 2, "PRUEBA_TURNOS");
        try {
            juego.agregarJugador(ana);
            juego.agregarJugador(jose);
            assertTrue(juego.getBanco().cobrarJugador(ana, 10));
            juego.terminarTurnoActual();
            assertTrue(juego.getBanco().cobrarJugador(jose, 20));
            Transaccion[] historial = juego.getRegistroTransacciones().getHistorial().desdeLaMasAntigua();
            assertEquals(TipoTransaccion.SALDO_INICIAL, historial[0].tipo());
            assertEquals(1, historial[2].turno());
            assertEquals(2, historial[3].turno());
            juego.terminarTurnoActual();
            assertTrue(juego.esFinDeJuego());
            assertTrue(juego.getRegistroTransacciones().exportar().contains("PAGO_AL_BANCO"));
        } finally {
            Files.deleteIfExists(juego.getArchivoTransacciones());
        }
    }

    @Test
    void errorDeDiscoConservaElPagoYPermiteReexportar() throws Exception {
        Files.delete(registro.getArchivo());
        assertTrue(banco.cobrarJugador(ana, 10));
        assertEquals(9990, ana.getSaldo());
        assertEquals(1, registro.getHistorial().getCantidad());
        assertNotNull(registro.getErrorEscritura());
        registro.exportar(carpeta.resolve("recuperado.txt"));
        assertTrue(Files.readString(carpeta.resolve("recuperado.txt")).contains("PAGO_AL_BANCO"));
    }
}
