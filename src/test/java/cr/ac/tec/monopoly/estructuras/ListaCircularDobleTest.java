package cr.ac.tec.monopoly.estructuras;

import estructuras.ListaCircularDoble;
import estructuras.Nodo;
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
        assertFalse(lista.eliminar(null));
        assertEquals(3, lista.tamaño());
    }

    @Test
    void eliminarEnVaciaDevuelveFalse() {
        assertFalse(new ListaCircularDoble<String>().eliminar("A"));
    }

    @Test
    void noAceptaNull() {
        ListaCircularDoble<String> lista = new ListaCircularDoble<>();
        assertThrows(IllegalArgumentException.class, () -> lista.agregarAlFinal(null));
        assertThrows(IllegalArgumentException.class, () -> lista.agregarAlInicio(null));
        assertThrows(IllegalArgumentException.class, () -> lista.insertarEn(0, null));
        assertTrue(lista.estaVacia());
    }

    @Test
    void toStringInversoUsaLosPunterosAnterior() {
        ListaCircularDoble<String> lista = listaABC();
        assertEquals("[A, B, C]", lista.toString());
        assertEquals("[C, B, A]", lista.toStringInverso());
        assertEquals("[]", new ListaCircularDoble<String>().toStringInverso());
    }

    @Test
    void insertarEnElMedio() {
        ListaCircularDoble<String> lista = listaABC();
        lista.insertarEn(1, "X");
        assertEquals("[A, X, B, C]", lista.toString());
        assertEquals("[C, B, X, A]", lista.toStringInverso());
        assertEquals(4, lista.tamaño());
    }

    @Test
    void insertarEnLosExtremos() {
        ListaCircularDoble<String> lista = listaABC();
        lista.insertarEn(0, "Inicio");
        lista.insertarEn(lista.tamaño(), "Fin");
        assertEquals("[Inicio, A, B, C, Fin]", lista.toString());
        assertEquals("Inicio", lista.getCabeza().getDato());
        assertEquals("Fin", lista.getCabeza().getAnterior().getDato());
    }

    @Test
    void insertarEnVacia() {
        ListaCircularDoble<String> lista = new ListaCircularDoble<>();
        lista.insertarEn(0, "A");
        assertEquals("[A]", lista.toString());
        assertSame(lista.getCabeza(), lista.getCabeza().getSiguiente());
    }

    @Test
    void insertarFueraDeRangoLanzaExcepcion() {
        ListaCircularDoble<String> lista = listaABC();
        assertThrows(IndexOutOfBoundsException.class, () -> lista.insertarEn(4, "X"));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.insertarEn(-1, "X"));
        assertEquals(3, lista.tamaño());
    }

    @Test
    void eliminarEnDevuelveElDato() {
        ListaCircularDoble<String> lista = listaABC();
        assertEquals("C", lista.eliminarEn(2));
        assertEquals("A", lista.eliminarEn(0));
        assertEquals("[B]", lista.toString());
        assertSame(lista.getCabeza(), lista.getCabeza().getAnterior());
        assertThrows(IndexOutOfBoundsException.class, () -> lista.eliminarEn(1));
    }

    @Test
    void elNodoBorradoQuedaSuelto() {
        ListaCircularDoble<String> lista = listaABC();
        Nodo<String> b = lista.obtenerNodo(1);
        lista.eliminar("B");
        assertNull(b.getSiguiente());
        assertNull(b.getAnterior());
    }

    @Test
    void indiceDeYContiene() {
        ListaCircularDoble<String> lista = listaABC();
        assertEquals(2, lista.indiceDe("C"));
        assertEquals(-1, lista.indiceDe("X"));
        assertEquals(-1, lista.indiceDe(null));
        assertTrue(lista.contiene("B"));
        assertFalse(lista.contiene("X"));
    }

    @Test
    void obtenerNodoDesdeAmbosLados() {
        ListaCircularDoble<String> lista = listaABC();
        lista.agregarAlFinal("D");
        lista.agregarAlFinal("E");
        // 0, 1 y 2 avanzan desde la cabeza; 3 y 4 retroceden desde ella
        for (int i = 0; i < lista.tamaño(); i++) {
            assertEquals(String.valueOf((char) ('A' + i)), lista.obtenerNodo(i).getDato());
        }
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtenerNodo(5));
    }

    @Test
    void avanzarDaLaVuelta() {
        ListaCircularDoble<String> lista = listaABC();
        Nodo<String> c = lista.obtenerNodo(2);
        assertEquals("A", lista.avanzar(c, 1).getDato());
        assertEquals("B", lista.avanzar(c, 2).getDato());
        assertSame(c, lista.avanzar(c, 3));
        assertSame(c, lista.avanzar(c, 0));
    }

    @Test
    void retrocederDaLaVuelta() {
        ListaCircularDoble<String> lista = listaABC();
        Nodo<String> a = lista.getCabeza();
        assertEquals("C", lista.retroceder(a, 1).getDato());
        assertEquals("B", lista.retroceder(a, 2).getDato());
        assertSame(a, lista.retroceder(a, 3));
    }

    @Test
    void moverseConDatosInvalidosLanzaExcepcion() {
        ListaCircularDoble<String> lista = listaABC();
        assertThrows(IllegalArgumentException.class, () -> lista.avanzar(null, 1));
        assertThrows(IllegalArgumentException.class, () -> lista.avanzar(lista.getCabeza(), -1));
        assertThrows(IllegalArgumentException.class, () -> lista.retroceder(lista.getCabeza(), -1));
    }

    @Test
    void vaciarYReusar() {
        ListaCircularDoble<String> lista = listaABC();
        lista.vaciar();
        assertTrue(lista.estaVacia());
        assertNull(lista.getCabeza());
        lista.agregarAlFinal("X");
        assertEquals("[X]", lista.toString());
    }
}
