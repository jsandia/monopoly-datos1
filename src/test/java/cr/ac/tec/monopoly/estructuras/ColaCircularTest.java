package cr.ac.tec.monopoly.estructuras;

import estructuras.ColaCircular;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColaCircularTest {

    @Test
    void empiezaVacia() {
        ColaCircular<String> cola = new ColaCircular<>(3);
        assertTrue(cola.estaVacia());
        assertEquals(0, cola.tamaño());
        assertNull(cola.verFrente());
        assertNull(cola.desencolar());
    }

    @Test
    void saleEnElMismoOrdenEnQueEntra() {
        ColaCircular<String> cola = new ColaCircular<>(3);
        cola.encolar("A");
        cola.encolar("B");
        cola.encolar("C");
        assertEquals("A", cola.desencolar());
        assertEquals("B", cola.desencolar());
        assertEquals("C", cola.desencolar());
        assertTrue(cola.estaVacia());
    }

    @Test
    void noDejaEncolarSiEstaLlena() {
        ColaCircular<Integer> cola = new ColaCircular<>(2);
        assertTrue(cola.encolar(1));
        assertTrue(cola.encolar(2));
        assertTrue(cola.estaLlena());
        assertFalse(cola.encolar(3));
        assertEquals(2, cola.tamaño());
    }

    @Test
    void reutilizaLosEspaciosAlDarLaVuelta() {
        ColaCircular<String> cola = new ColaCircular<>(3);
        cola.encolar("A");
        cola.encolar("B");
        cola.encolar("C");
        cola.desencolar();              // sale A, se libera el índice 0
        assertTrue(cola.encolar("D"));  // fin da la vuelta al índice 0
        assertEquals("[B, C, D]", cola.toString());
        assertEquals("B", cola.verFrente());
    }

    @Test
    void sacarYVolverAMeterRotaComoUnMazo() {
        ColaCircular<Integer> mazo = new ColaCircular<>(3);
        mazo.encolar(1);
        mazo.encolar(2);
        mazo.encolar(3);
        for (int vuelta = 0; vuelta < 7; vuelta++) {
            mazo.encolar(mazo.desencolar()); // la carta usada pasa al final
        }
        // 7 rotaciones en un mazo de 3 = 1 rotación: el frente es la carta 2
        assertEquals("[2, 3, 1]", mazo.toString());
        assertEquals(3, mazo.tamaño());
    }

    @Test
    void capacidadInvalidaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new ColaCircular<String>(0));
    }
}
