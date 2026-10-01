package cr.ac.tec.monopoly.estructuras;

import estructuras.ListaSimple;
import estructuras.NodoSimple;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListaSimpleTest {

    private ListaSimple<String> listaABC() {
        ListaSimple<String> lista = new ListaSimple<>();
        lista.agregarAlFinal("A");
        lista.agregarAlFinal("B");
        lista.agregarAlFinal("C");
        return lista;
    }

    @Test
    void listaNuevaEstaVacia() {
        ListaSimple<String> lista = new ListaSimple<>();
        assertTrue(lista.estaVacia());
        assertEquals(0, lista.tamaño());
        assertNull(lista.getPrimero());
        assertEquals("[]", lista.toString());
    }

    @Test
    void agregarAlFinalMantieneElOrden() {
        ListaSimple<String> lista = listaABC();
        assertEquals(3, lista.tamaño());
        assertEquals("[A, B, C]", lista.toString());
    }

    @Test
    void agregarAlInicioQuedaPrimero() {
        ListaSimple<String> lista = listaABC();
        lista.agregarAlInicio("Z");
        assertEquals("[Z, A, B, C]", lista.toString());
        assertEquals("Z", lista.getPrimero().getDato());
    }

    @Test
    void agregarAlInicioEnVaciaTambienEsElUltimo() {
        ListaSimple<String> lista = new ListaSimple<>();
        lista.agregarAlInicio("A");
        // Si ultimo no se actualizara, este agregarAlFinal fallaría
        lista.agregarAlFinal("B");
        assertEquals("[A, B]", lista.toString());
    }

    @Test
    void elUltimoNodoApuntaANull() {
        NodoSimple<String> actual = listaABC().getPrimero();
        int pasos = 0;
        while (actual.getSiguiente() != null) {
            actual = actual.getSiguiente();
            pasos++;
        }
        assertEquals(2, pasos);
        assertEquals("C", actual.getDato());
    }

    @Test
    void obtenerYFueraDeRango() {
        ListaSimple<String> lista = listaABC();
        assertEquals("A", lista.obtener(0));
        assertEquals("C", lista.obtener(2));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(3));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(-1));
    }

    @Test
    void indiceDeYContiene() {
        ListaSimple<String> lista = listaABC();
        assertEquals(1, lista.indiceDe("B"));
        assertEquals(-1, lista.indiceDe("X"));
        assertEquals(-1, lista.indiceDe(null));
        assertTrue(lista.contiene("C"));
        assertFalse(lista.contiene("X"));
    }

    @Test
    void eliminarDelMedio() {
        ListaSimple<String> lista = listaABC();
        assertTrue(lista.eliminar("B"));
        assertEquals("[A, C]", lista.toString());
        assertEquals(2, lista.tamaño());
    }

    @Test
    void eliminarElPrimeroMueveElPrimero() {
        ListaSimple<String> lista = listaABC();
        assertTrue(lista.eliminar("A"));
        assertEquals("B", lista.getPrimero().getDato());
        assertEquals("[B, C]", lista.toString());
    }

    @Test
    void eliminarElUltimoMueveElUltimo() {
        ListaSimple<String> lista = listaABC();
        assertTrue(lista.eliminar("C"));
        // Si ultimo siguiera apuntando a C, la D quedaría colgada de un nodo borrado
        lista.agregarAlFinal("D");
        assertEquals("[A, B, D]", lista.toString());
    }

    @Test
    void eliminarElUnicoDejaLaListaVacia() {
        ListaSimple<String> lista = new ListaSimple<>();
        lista.agregarAlFinal("A");
        assertTrue(lista.eliminar("A"));
        assertTrue(lista.estaVacia());
        assertNull(lista.getPrimero());
        lista.agregarAlFinal("B");
        assertEquals("[B]", lista.toString());
    }

    @Test
    void eliminarInexistenteONullDevuelveFalse() {
        ListaSimple<String> lista = listaABC();
        assertFalse(lista.eliminar("X"));
        assertFalse(lista.eliminar(null));
        assertEquals(3, lista.tamaño());
    }

    @Test
    void eliminarSoloLaPrimeraAparicion() {
        ListaSimple<String> lista = listaABC();
        lista.agregarAlFinal("A");
        assertTrue(lista.eliminar("A"));
        assertEquals("[B, C, A]", lista.toString());
    }

    @Test
    void noAceptaNull() {
        ListaSimple<String> lista = new ListaSimple<>();
        assertThrows(IllegalArgumentException.class, () -> lista.agregarAlFinal(null));
        assertThrows(IllegalArgumentException.class, () -> lista.agregarAlInicio(null));
        assertTrue(lista.estaVacia());
    }

    @Test
    void vaciarYReusar() {
        ListaSimple<String> lista = listaABC();
        lista.vaciar();
        assertTrue(lista.estaVacia());
        assertNull(lista.getPrimero());
        lista.agregarAlFinal("X");
        assertEquals("[X]", lista.toString());
    }
}
