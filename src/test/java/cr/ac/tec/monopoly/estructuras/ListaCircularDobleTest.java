package cr.ac.tec.monopoly.estructuras;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListaCircularDobleTest {

    private ListaCircularDoble<String> listaABC() {
        ListaCircularDoble<String> lista = new ListaCircularDoble<>();
        lista.agregarAlFinal("A");
        lista.agregarAlFinal("B");
        lista.agregarAlFinal("C");
        return lista;
    }

    @Test
    void listaNuevaEstaVacia() {
        ListaCircularDoble<String> lista = new ListaCircularDoble<>();
        assertTrue(lista.estaVacia());
        assertEquals(0, lista.tamaño());
        assertNull(lista.getCabeza());
    }

    @Test
    void agregarAlFinalMantieneElOrden() {
        ListaCircularDoble<String> lista = listaABC();
        assertEquals(3, lista.tamaño());
        assertEquals("A", lista.obtener(0));
        assertEquals("B", lista.obtener(1));
        assertEquals("C", lista.obtener(2));
    }

    @Test
    void unSoloNodoSeApuntaASiMismo() {
        ListaCircularDoble<String> lista = new ListaCircularDoble<>();
        lista.agregarAlFinal("A");
        Nodo<String> cabeza = lista.getCabeza();
        assertSame(cabeza, cabeza.getSiguiente());
        assertSame(cabeza, cabeza.getAnterior());
    }

    @Test
    void esCircularEnAmbasDirecciones() {
        ListaCircularDoble<String> lista = listaABC();
        Nodo<String> cabeza = lista.getCabeza();
        // Tres saltos hacia adelante vuelven a la cabeza
        assertSame(cabeza, cabeza.getSiguiente().getSiguiente().getSiguiente());
        // Un salto hacia atrás desde la cabeza llega al último
        assertEquals("C", cabeza.getAnterior().getDato());
    }

    @Test
    void agregarAlInicioCambiaLaCabeza() {
        ListaCircularDoble<String> lista = listaABC();
        lista.agregarAlInicio("Z");
        assertEquals(4, lista.tamaño());
        assertEquals("Z", lista.obtener(0));
        assertEquals("A", lista.obtener(1));
        assertEquals("C", lista.obtener(3));
        // Sigue siendo circular: el último apunta al nuevo inicio
        assertEquals("Z", lista.getCabeza().getAnterior().getSiguiente().getDato());
    }

    @Test
    void obtenerFueraDeRangoLanzaExcepcion() {
        ListaCircularDoble<String> lista = listaABC();
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(3));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(-1));
    }

    @Test
    void eliminarDelMedioReenlazaVecinos() {
        ListaCircularDoble<String> lista = listaABC();
        assertTrue(lista.eliminar("B"));
        assertEquals(2, lista.tamaño());
        assertEquals("A", lista.obtener(0));
        assertEquals("C", lista.obtener(1));
        Nodo<String> cabeza = lista.getCabeza();
        assertEquals("C", cabeza.getSiguiente().getDato());
        assertEquals("A", cabeza.getSiguiente().getAnterior().getDato());
    }

    @Test
    void eliminarLaCabezaMueveLaCabeza() {
        ListaCircularDoble<String> lista = listaABC();
        assertTrue(lista.eliminar("A"));
        assertEquals("B", lista.getCabeza().getDato());
        assertEquals("C", lista.getCabeza().getAnterior().getDato());
    }

    @Test
    void eliminarElUnicoDejaLaListaVacia() {
        ListaCircularDoble<String> lista = new ListaCircularDoble<>();
        lista.agregarAlFinal("A");
        assertTrue(lista.eliminar("A"));
        assertTrue(lista.estaVacia());
        assertNull(lista.getCabeza());
    }

    @Test
    void eliminarInexistenteDevuelveFalse() {
        ListaCircularDoble<String> lista = listaABC();
        assertFalse(lista.eliminar("X"));
        assertEquals(3, lista.tamaño());
    }
}
