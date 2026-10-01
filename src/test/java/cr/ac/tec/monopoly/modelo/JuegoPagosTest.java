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

    @Test
    void caerEnPropiedadDeOtroTransfiereAlquilerAlPropietario() {
        Propiedad aeropuerto = (Propiedad) juego.getTablero().obtenerCasilla(5);
        assertTrue(juego.getBanco().comprarPropiedad(jose, aeropuerto));
        ana.setPosicionActual(2);

        String resultado = juego.jugarTiro(1, 2);

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
    void caerEnPropiedadPropiaNoCobraAlquiler() {
        Propiedad aeropuerto = (Propiedad) juego.getTablero().obtenerCasilla(5);
        assertTrue(juego.getBanco().comprarPropiedad(ana, aeropuerto));
        ana.setPosicionActual(2);

        juego.jugarTiro(1, 2);

        assertEquals(1300, ana.getSaldo());
        assertEquals(0, juego.getRegistroTransacciones().getHistorial()
                .buscarPorTipo(TipoTransaccion.PAGO_ALQUILER).length);
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
    void pagosInasequiblesNoCambianSaldosNiGeneranTransacciones() {
        ana.setSaldo(10);
        Propiedad aeropuerto = (Propiedad) juego.getTablero().obtenerCasilla(5);
        assertTrue(juego.getBanco().comprarPropiedad(jose, aeropuerto));
        ana.setPosicionActual(2);

        String resultadoAlquiler = juego.jugarTiro(1, 2);

        assertEquals(10, ana.getSaldo());
        assertEquals(1300, jose.getSaldo());
        assertTrue(resultadoAlquiler.contains("no tiene suficiente saldo"));
        assertEquals(0, juego.getRegistroTransacciones().getHistorial()
                .buscarPorTipo(TipoTransaccion.PAGO_ALQUILER).length);

        juego.terminarTurnoActual();
        juego.terminarTurnoActual();
        ana.setSaldo(150);
        ana.setPosicionActual(1);
        juego.jugarTiro(1, 2);

        assertEquals(150, ana.getSaldo());
        assertEquals(0, juego.getRegistroTransacciones().getHistorial()
                .buscarPorTipo(TipoTransaccion.PAGO_AL_BANCO).length);
    }

    @Test
    void cartaQueMueveAPropiedadResuelveSuAlquiler() {
        Propiedad puertoCaldera = (Propiedad) juego.getTablero().obtenerCasilla(15);
        assertTrue(juego.getBanco().comprarPropiedad(jose, puertoCaldera));
        ana.setPosicionActual(4);
        while (juego.getMazoSuerte().verFrente().getId() != 14) {
            juego.getMazoSuerte().encolar(juego.getMazoSuerte().desencolar());
        }

        juego.jugarTiro(1, 2); // Suerte la mueve al transporte de la casilla 15.

        assertEquals(15, ana.getPosicionActual());
        assertEquals(1475, ana.getSaldo());
        assertEquals(1325, jose.getSaldo());
        assertEquals(1, juego.getRegistroTransacciones().getHistorial()
                .buscarPorTipo(TipoTransaccion.PAGO_ALQUILER).length);
    }
}
