package cr.ac.tec.monopoly.estructuras;

import estructuras.ListaDoble;
import estructuras.Nodo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListaDobleTest {

    private ListaDoble<String> listaABCD() {
        ListaDoble<String> lista = new ListaDoble<>();
        lista.agregarAlFinal("A");
        lista.agregarAlFinal("B");
        lista.agregarAlFinal("C");
        lista.agregarAlFinal("D");
        return lista;
    }

    /** Revisa que los punteros hacia atrás sean el espejo de los de adelante. */
    private void assertEnlacesCoherentes(ListaDoble<String> lista) {
        assertEquals(lista.toString(), "[" + invertir(lista.toStringInverso()) + "]");
        if (!lista.estaVacia()) {
            assertNull(lista.getPrimero().getAnterior());
            assertNull(lista.getUltimo().getSiguiente());
        }
    }

    private String invertir(String textoLista) {
        String contenido = textoLista.replace("[", "").replace("]", "");
        if (contenido.isEmpty()) return "";
        String[] partes = contenido.split(", ");
        StringBuilder invertido = new StringBuilder();
        for (int i = partes.length - 1; i >= 0; i--) {
            invertido.append(partes[i]);
            if (i > 0) invertido.append(", ");
        }
        return invertido.toString();
    }

    @Test
    void listaNuevaEstaVacia() {
        ListaDoble<String> lista = new ListaDoble<>();
        assertTrue(lista.estaVacia());
        assertNull(lista.getPrimero());
        assertNull(lista.getUltimo());
        assertEquals("[]", lista.toString());
        assertEquals("[]", lista.toStringInverso());
    }

    @Test
    void unSoloNodoEsPrimeroYUltimo() {
        ListaDoble<String> lista = new ListaDoble<>();
        lista.agregarAlFinal("A");
        assertSame(lista.getPrimero(), lista.getUltimo());
        assertNull(lista.getPrimero().getAnterior());
        assertNull(lista.getPrimero().getSiguiente());
    }

    @Test
    void recorreEnLosDosSentidos() {
        ListaDoble<String> lista = listaABCD();
        assertEquals("[A, B, C, D]", lista.toString());
        assertEquals("[D, C, B, A]", lista.toStringInverso());
        assertEnlacesCoherentes(lista);
    }

    @Test
    void agregarAlInicio() {
        ListaDoble<String> lista = listaABCD();
        lista.agregarAlInicio("Z");
        assertEquals("[Z, A, B, C, D]", lista.toString());
        assertEnlacesCoherentes(lista);
    }

    @Test
    void agregarAlInicioEnVacia() {
        ListaDoble<String> lista = new ListaDoble<>();
        lista.agregarAlInicio("A");
        lista.agregarAlFinal("B");
        assertEquals("[A, B]", lista.toString());
        assertEnlacesCoherentes(lista);
    }

    @Test
    void obtenerDesdeAmbosExtremos() {
        ListaDoble<String> lista = listaABCD();
        // 0 y 1 se buscan desde primero; 2 y 3 desde ultimo
        assertEquals("A", lista.obtener(0));
        assertEquals("B", lista.obtener(1));
        assertEquals("C", lista.obtener(2));
        assertEquals("D", lista.obtener(3));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(4));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(-1));
    }

    @Test
    void indiceDeYContiene() {
        ListaDoble<String> lista = listaABCD();
        assertEquals(2, lista.indiceDe("C"));
        assertEquals(-1, lista.indiceDe("X"));
        assertEquals(-1, lista.indiceDe(null));
        assertTrue(lista.contiene("A"));
        assertFalse(lista.contiene("X"));
    }

    @Test
    void eliminarDelMedioUneALosVecinos() {
        ListaDoble<String> lista = listaABCD();
        assertTrue(lista.eliminar("B"));
        assertEquals("[A, C, D]", lista.toString());
        Nodo<String> a = lista.getPrimero();
        assertEquals("C", a.getSiguiente().getDato());
        assertSame(a, a.getSiguiente().getAnterior());
        assertEnlacesCoherentes(lista);
    }

    @Test
    void eliminarExtremos() {
        ListaDoble<String> lista = listaABCD();
        assertTrue(lista.eliminar("A"));
        assertTrue(lista.eliminar("D"));
        assertEquals("[B, C]", lista.toString());
        assertEnlacesCoherentes(lista);
    }

    @Test
    void eliminarElUnicoDejaLaListaVacia() {
        ListaDoble<String> lista = new ListaDoble<>();
        lista.agregarAlFinal("A");
        assertTrue(lista.eliminar("A"));
        assertTrue(lista.estaVacia());
        assertNull(lista.getPrimero());
        assertNull(lista.getUltimo());
    }

    @Test
    void eliminarInexistenteONullDevuelveFalse() {
        ListaDoble<String> lista = listaABCD();
        assertFalse(lista.eliminar("X"));
        assertFalse(lista.eliminar(null));
        assertEquals(4, lista.tamaño());
    }

    @Test
    void eliminarPrimeroYUltimo() {
        ListaDoble<String> lista = listaABCD();
        assertEquals("A", lista.eliminarPrimero());
        assertEquals("D", lista.eliminarUltimo());
        assertEquals("[B, C]", lista.toString());
        assertEnlacesCoherentes(lista);
        assertEquals("B", lista.eliminarPrimero());
        assertEquals("C", lista.eliminarUltimo());
        assertTrue(lista.estaVacia());
        assertNull(lista.eliminarPrimero());
        assertNull(lista.eliminarUltimo());
    }

    @Test
    void elNodoBorradoQuedaSuelto() {
        ListaDoble<String> lista = listaABCD();
        Nodo<String> b = lista.getPrimero().getSiguiente();
        lista.eliminar("B");
        assertNull(b.getAnterior());
        assertNull(b.getSiguiente());
    }

    @Test
    void noAceptaNull() {
        ListaDoble<String> lista = new ListaDoble<>();
        assertThrows(IllegalArgumentException.class, () -> lista.agregarAlFinal(null));
        assertThrows(IllegalArgumentException.class, () -> lista.agregarAlInicio(null));
        assertTrue(lista.estaVacia());
    }

    @Test
    void vaciarYReusar() {
        ListaDoble<String> lista = listaABCD();
        lista.vaciar();
        assertTrue(lista.estaVacia());
        assertNull(lista.getPrimero());
        assertNull(lista.getUltimo());
        lista.agregarAlFinal("X");
        assertEquals("[X]", lista.toString());
        assertEnlacesCoherentes(lista);
    }
}
