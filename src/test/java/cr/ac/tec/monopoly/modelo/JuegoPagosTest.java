package cr.ac.tec.monopoly.modelo;

import modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class JuegoPagosTest {
    private Juego juego;
    private Jugador ana;
    private Jugador jose;

    @BeforeEach
    void preparar() {
        juego = new Juego(20, 2, "PRUEBA_PAGOS");
        ana = new Jugador("J1", "Ana", 1500);
        jose = new Jugador("J2", "José", 1500);
        juego.agregarJugador(ana);
        juego.agregarJugador(jose);
    }

    @AfterEach
    void borrarArchivo() throws Exception {
        Files.deleteIfExists(juego.getArchivoTransacciones());
    }

    private int alquileresRegistrados() {
        return juego.getRegistroTransacciones().getHistorial()
                .buscarPorTipo(TipoTransaccion.PAGO_ALQUILER).length;
    }

    @Test
    void caerEnPropiedadDeOtroDejaElAlquilerPendienteSinMoverDinero() {
        Propiedad aeropuerto = (Propiedad) juego.getTablero().obtenerCasilla(5);
        assertTrue(juego.getBanco().comprarPropiedad(jose, aeropuerto));
        ana.setPosicionActual(2);

        String resultado = juego.jugarTiro(1, 2);

        assertTrue(resultado.contains("debe pagar ₡25 de alquiler a José"));
        assertTrue(juego.hayAlquilerPendiente());
        assertSame(aeropuerto, juego.getAlquilerPendiente());
        assertEquals(1500, ana.getSaldo());
        assertEquals(1300, jose.getSaldo());
        assertEquals(0, alquileresRegistrados());
    }

    @Test
    void pagarElAlquilerPendienteTransfiereAlPropietario() {
        Propiedad aeropuerto = (Propiedad) juego.getTablero().obtenerCasilla(5);
        assertTrue(juego.getBanco().comprarPropiedad(jose, aeropuerto));
        ana.setPosicionActual(2);
        juego.jugarTiro(1, 2);

        String resultado = juego.pagarAlquilerPendiente();

        assertFalse(juego.hayAlquilerPendiente());
        assertEquals(1475, ana.getSaldo());
        assertEquals(1325, jose.getSaldo());
        assertTrue(resultado.contains("paga ₡25 de alquiler a José"));
        Transaccion[] alquileres = juego.getRegistroTransacciones().getHistorial()
                .buscarPorTipo(TipoTransaccion.PAGO_ALQUILER);
        assertEquals(1, alquileres.length);
        assertEquals("J1", alquileres[0].idOrigen());
        assertEquals("J2", alquileres[0].idDestino());
        assertEquals(25, alquileres[0].monto());
    }

    @Test
    void conUnAlquilerPendienteNoSePuedeTerminarElTurno() {
        assertTrue(juego.getBanco().comprarPropiedad(jose, (Propiedad) juego.getTablero().obtenerCasilla(5)));
        ana.setPosicionActual(2);
        juego.jugarTiro(1, 2);

        assertThrows(IllegalStateException.class, () -> juego.terminarTurnoActual());
        assertSame(ana, juego.getJugadorActual());

        juego.pagarAlquilerPendiente();
        juego.terminarTurnoActual();
        assertSame(jose, juego.getJugadorActual());
    }

    @Test
    void pagarSinAlquilerPendienteEsUnError() {
        assertThrows(IllegalStateException.class, () -> juego.pagarAlquilerPendiente());
    }

    @Test
    void siAlPagarYaNoLeAlcanzaQuedaEnBancarrota() {
        assertTrue(juego.getBanco().comprarPropiedad(jose, (Propiedad) juego.getTablero().obtenerCasilla(5)));
        ana.setPosicionActual(2);
        juego.jugarTiro(1, 2);
        ana.setSaldo(10); // por ejemplo, gastó antes de pagar

        String resultado = juego.pagarAlquilerPendiente();

        assertTrue(resultado.contains("BANCARROTA"));
        assertFalse(ana.isActivo());
        assertEquals(1310, jose.getSaldo());
    }

    @Test
    void caerEnPropiedadPropiaNoCobraAlquiler() {
        Propiedad aeropuerto = (Propiedad) juego.getTablero().obtenerCasilla(5);
        assertTrue(juego.getBanco().comprarPropiedad(ana, aeropuerto));
        ana.setPosicionActual(2);

        juego.jugarTiro(1, 2);

        assertEquals(1300, ana.getSaldo());
        assertFalse(juego.hayAlquilerPendiente());
        assertEquals(0, alquileresRegistrados());
    }

    @Test
    void ambasCasillasDeImpuestoCobranAlJugadorQueCaeEnEllas() {
        ana.setPosicionActual(1);
        juego.jugarTiro(1, 2); // Casilla 4: impuesto de renta de ₡200

        juego.terminarTurnoActual();
        jose.setPosicionActual(35);
        juego.jugarTiro(1, 2); // Casilla 38: impuesto de lujo de ₡100

        assertEquals(1300, ana.getSaldo());
        assertEquals(1400, jose.getSaldo());
        assertEquals(1_000_300, juego.getBanco().getSaldo());
        Transaccion[] impuestos = juego.getRegistroTransacciones().getHistorial()
                .buscarPorTipo(TipoTransaccion.PAGO_AL_BANCO);
        assertEquals(2, impuestos.length);
        assertEquals(200, impuestos[0].monto());
        assertEquals(100, impuestos[1].monto());
        assertEquals("BANCO", impuestos[0].idDestino());
        assertEquals("BANCO", impuestos[1].idDestino());
    }

    @Test
    void cartaQueMueveAPropiedadAjenaTambienDejaElAlquilerPendiente() {
        Propiedad puertoCaldera = (Propiedad) juego.getTablero().obtenerCasilla(15);
        assertTrue(juego.getBanco().comprarPropiedad(jose, puertoCaldera));
        ana.setPosicionActual(4);
        while (juego.getMazoSuerte().verFrente().getId() != 14) {
            juego.getMazoSuerte().encolar(juego.getMazoSuerte().desencolar());
        }

        juego.jugarTiro(1, 2); // Suerte la mueve al transporte de la casilla 15.

        assertEquals(15, ana.getPosicionActual());
        assertSame(puertoCaldera, juego.getAlquilerPendiente());
        assertEquals(1500, ana.getSaldo());

        juego.pagarAlquilerPendiente();
        assertEquals(1475, ana.getSaldo());
        assertEquals(1325, jose.getSaldo());
        assertEquals(1, alquileresRegistrados());
    }
}
