package cr.ac.tec.monopoly.modelo;

import modelo.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartaEventoTest {

    private final CartaEvento[] arca = CartaEvento.crearMazoArcaComunal();
    private final CartaEvento[] suerte = CartaEvento.crearMazoSuerte();

    @Test
    void cadaMazoTiene10Cartas() {
        assertEquals(10, arca.length);
        assertEquals(10, suerte.length);
    }

    @Test
    void cadaCartaEstaEnSuMazo() {
        for (CartaEvento c : arca) {
            assertEquals(CasillaEvento.Tipo.ARCA_COMUNAL, c.getMazo());
        }
        for (CartaEvento c : suerte) {
            assertEquals(CasillaEvento.Tipo.SUERTE, c.getMazo());
        }
    }

    @Test
    void losIdsNoSeRepitenEntreMazos() {
        for (int i = 0; i < 10; i++) {
            assertEquals(i + 1, arca[i].getId());
            assertEquals(i + 11, suerte[i].getId());
        }
    }

    /** Si alguien reordena el Tablero, este test avisa que los ids de las cartas quedaron mal. */
    @Test
    void lasCartasDeMovimientoApuntanALaCasillaQueNombran() {
        Tablero tablero = new Tablero();
        for (CartaEvento[] mazo : new CartaEvento[][] {arca, suerte}) {
            for (CartaEvento c : mazo) {
                if (c.getAccion() == CartaEvento.Accion.IR_A_CASILLA) {
                    String destino = tablero.obtenerCasilla(c.getValor()).getNombre();
                    assertTrue(c.getTexto().contains(destino),
                            "La carta " + c.getId() + " dice \"" + c.getTexto()
                                    + "\" pero apunta a " + destino);
                }
                if (c.getAccion() == CartaEvento.Accion.IR_A_CARCEL) {
                    Casilla carcel = tablero.obtenerCasilla(c.getValor());
                    assertEquals(CasillaEspecial.Tipo.CARCEL, ((CasillaEspecial) carcel).getTipo());
                }
            }
        }
    }
}
