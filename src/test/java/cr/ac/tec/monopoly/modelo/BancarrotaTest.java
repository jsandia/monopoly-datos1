package cr.ac.tec.monopoly.modelo;

import estructuras.ColaCircular;
import modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/** Bancarrota, fin de partida, ganador y la carta "pierda su próximo turno". */
class BancarrotaTest {
    private Juego juego;
    private Jugador ana;
    private Jugador beto;
    private Jugador caro;
    private Jugador dani;

    @BeforeEach
    void preparar() {
        juego = new Juego(20, 4, "PRUEBA_BANCARROTA", false);
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

    private Propiedad propiedad(Juego partida, int id) {
        return (Propiedad) partida.getTablero().obtenerCasilla(id);
    }

    private void ponerAlFrente(ColaCircular<CartaEvento> mazo, int idCarta) {
        while (mazo.verFrente().getId() != idCarta) {
            mazo.encolar(mazo.desencolar());
        }
    }

    private Transaccion[] delTipo(TipoTransaccion tipo) {
        return juego.getRegistroTransacciones().getHistorial().buscarPorTipo(tipo);
    }

    // =========================================================
    // ELIMINACIÓN
    // =========================================================

    @Test
    void alquilerImpagableEntregaTodoAlDuenoYLiberaSusPropiedades() {
        Propiedad sixaola = propiedad(juego, 1);
        assertTrue(juego.getBanco().comprarPropiedad(ana, sixaola));
        assertTrue(juego.getBanco().comprarPropiedad(beto, propiedad(juego, 5)));
        ana.setSaldo(10);
        ana.setPosicionActual(2);

        String resultado = juego.jugarTiro(1, 2); // cae en el aeropuerto de Beto: alquiler ₡25

        assertTrue(resultado.contains("BANCARROTA"));
        assertFalse(ana.isActivo());
        assertEquals(0, ana.getSaldo());
        assertEquals(1500 - 200 + 10, beto.getSaldo());
        assertNull(sixaola.getPropietario());
        assertEquals(0, ana.getCantidadPropiedades());

        Transaccion pago = delTipo(TipoTransaccion.PAGO_ALQUILER)[0];
        assertEquals(10, pago.monto());
        assertEquals("J1", pago.idOrigen());
        assertEquals("J2", pago.idDestino());
        assertTrue(pago.descripcion().contains("bancarrota"));
        assertFalse(juego.esFinDeJuego());
    }

    @Test
    void elEliminadoSaleDeLaColaAlTerminarSuTurno() {
        assertTrue(juego.getBanco().comprarPropiedad(beto, propiedad(juego, 5)));
        ana.setSaldo(10);
        ana.setPosicionActual(2);
        juego.jugarTiro(1, 2);

        // Sigue al frente hasta que termina su turno; después no vuelve a entrar
        assertSame(ana, juego.getJugadorActual());
        Jugador[] esperado = {beto, caro, dani, beto, caro};
        for (Jugador jugador : esperado) {
            juego.terminarTurnoActual();
            assertSame(jugador, juego.getJugadorActual());
        }
    }

    @Test
    void impuestoImpagableDevuelveCasasYHotelAlBanco() {
        Propiedad sixaola = propiedad(juego, 1);
        Banco banco = juego.getBanco();
        assertTrue(banco.comprarPropiedad(ana, sixaola));
        ana.registrarRecorrido(40);
        for (int i = 0; i < 4; i++) assertTrue(banco.comprarCasa(ana, sixaola));
        assertTrue(banco.comprarHotel(ana, sixaola));
        assertEquals(11, banco.getHotelesDisponibles());
        ana.setSaldo(50);
        ana.setPosicionActual(1);

        juego.jugarTiro(1, 2); // impuesto de renta de ₡200

        assertFalse(ana.isActivo());
        assertEquals(12, banco.getHotelesDisponibles());
        assertEquals(32, banco.getCasasDisponibles());
        assertFalse(sixaola.tieneHotel());
        assertNull(sixaola.getPropietario());
        Transaccion[] alBanco = delTipo(TipoTransaccion.PAGO_AL_BANCO);
        assertEquals(50, alBanco[alBanco.length - 1].monto());
    }

    @Test
    void cartaDePagarAlBancoImpagableElimina() {
        ponerAlFrente(juego.getMazoArcaComunal(), 3); // honorarios del médico: ₡50
        ana.setSaldo(30);
        juego.jugarTiro(1, 1); // Salida → Arca Comunal

        assertFalse(ana.isActivo());
        assertEquals(30, delTipo(TipoTransaccion.PERDIDA_EVENTO)[0].monto());
    }

    @Test
    void elCumpleanosPuedeEliminarAOtroSinDesordenarLosTurnos() {
        ponerAlFrente(juego.getMazoArcaComunal(), 8); // ₡10 de cada jugador
        beto.setSaldo(5);
        juego.jugarTiro(1, 1);

        assertFalse(beto.isActivo());
        assertEquals(1500 + 5 + 10 + 10, ana.getSaldo());

        // Beto salió de la cola de una vez; Ana sigue en su turno
        assertSame(ana, juego.getJugadorActual());
        juego.terminarTurnoActual();
        assertSame(caro, juego.getJugadorActual());
        juego.terminarTurnoActual();
        assertSame(dani, juego.getJugadorActual());
        juego.terminarTurnoActual();
        assertSame(ana, juego.getJugadorActual());
    }

    @Test
    void siNoLeAlcanzaParaPagarleACadaUnoLosUltimosNoReciben() {
        ponerAlFrente(juego.getMazoSuerte(), 20); // ₡50 a cada jugador
        ana.setSaldo(70);
        juego.jugarTiro(3, 4); // Salida → Suerte (7)

        assertFalse(ana.isActivo());
        assertEquals(1550, beto.getSaldo()); // le alcanzó
        assertEquals(1520, caro.getSaldo()); // recibe lo que quedaba
        assertEquals(1500, dani.getSaldo()); // ya no recibe
    }

    // =========================================================
    // FIN DE PARTIDA Y GANADOR
    // =========================================================

    @Test
    void cuandoQuedaUnSoloActivoTerminaYEseGana() throws Exception {
        Juego partida = new Juego(20, 2, "PRUEBA_ULTIMO", false);
        try {
            Jugador eva = new Jugador("J1", "Eva", 10);
            Jugador fede = new Jugador("J2", "Fede", 1500);
            partida.agregarJugador(eva);
            partida.agregarJugador(fede);
            assertTrue(partida.getBanco().comprarPropiedad(fede, propiedad(partida, 5)));
            eva.setPosicionActual(2);

            partida.jugarTiro(1, 2);

            assertTrue(partida.esFinDeJuego());
            assertSame(fede, partida.getGanador());
            assertFalse(partida.hayEmpate());
        } finally {
            Files.deleteIfExists(partida.getArchivoTransacciones());
        }
    }

    @Test
    void alLlegarAlLimiteDeTurnosGanaElDeMayorPatrimonio() throws Exception {
        Juego partida = new Juego(2, 2, "PRUEBA_LIMITE", false);
        try {
            Jugador eva = new Jugador("J1", "Eva", 1500);
            Jugador fede = new Jugador("J2", "Fede", 1400);
            partida.agregarJugador(eva);
            partida.agregarJugador(fede);
            // Eva: 1100 de saldo + 400 de Santa Ana = 1500; Fede: 1400
            assertTrue(partida.getBanco().comprarPropiedad(eva, propiedad(partida, 39)));

            partida.terminarTurnoActual(); // turno 2
            assertFalse(partida.esFinDeJuego());
            partida.terminarTurnoActual(); // turno 3 > 2: se acabó

            assertTrue(partida.esFinDeJuego());
            assertSame(eva, partida.getGanador());
            assertEquals(1500, eva.calcularPatrimonio());
            assertFalse(partida.hayEmpate());
        } finally {
            Files.deleteIfExists(partida.getArchivoTransacciones());
        }
    }

    @Test
    void empateEnPatrimonioSeMarca() throws Exception {
        Juego partida = new Juego(1, 2, "PRUEBA_EMPATE", false);
        try {
            partida.agregarJugador(new Jugador("J1", "Eva", 1500));
            partida.agregarJugador(new Jugador("J2", "Fede", 1500));
            partida.terminarTurnoActual();
            assertTrue(partida.esFinDeJuego());
            assertNotNull(partida.getGanador());
            assertTrue(partida.hayEmpate());
        } finally {
            Files.deleteIfExists(partida.getArchivoTransacciones());
        }
    }

    @Test
    void conLaPartidaTerminadaNoAvanzanLosTurnos() throws Exception {
        Juego partida = new Juego(1, 2, "PRUEBA_TERMINADA", false);
        try {
            partida.agregarJugador(new Jugador("J1", "Eva", 1500));
            partida.agregarJugador(new Jugador("J2", "Fede", 1500));
            partida.terminarTurnoActual();
            int turno = partida.getTurnoActualGlobal();
            partida.terminarTurnoActual();
            assertEquals(turno, partida.getTurnoActualGlobal());
        } finally {
            Files.deleteIfExists(partida.getArchivoTransacciones());
        }
    }

    // =========================================================
    // PERDER UN TURNO
    // =========================================================

    @Test
    void laCartaDePerderTurnoSaltaExactamenteUnTurno() {
        ponerAlFrente(juego.getMazoArcaComunal(), 4); // pierda su próximo turno
        juego.jugarTiro(1, 1);
        assertEquals(1, ana.getTurnosPorPerder());

        juego.terminarTurnoActual(); // Beto
        juego.terminarTurnoActual(); // Caro
        juego.terminarTurnoActual(); // Dani
        String avisos = juego.terminarTurnoActual(); // le tocaba a Ana: lo pierde

        assertTrue(avisos.contains("Ana pierde este turno"));
        assertSame(beto, juego.getJugadorActual());
        assertEquals(0, ana.getTurnosPorPerder());
        assertEquals(5, juego.getTurnoActualGlobal()); // el turno perdido no cuenta

        // A la vuelta siguiente Ana juega normal
        juego.terminarTurnoActual(); // Caro
        juego.terminarTurnoActual(); // Dani
        juego.terminarTurnoActual();
        assertSame(ana, juego.getJugadorActual());
    }
}
