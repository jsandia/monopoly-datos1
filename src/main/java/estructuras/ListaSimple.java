package estructuras;

/**
 * Lista simplemente enlazada genérica.
 *
 * <p>En el juego guarda las propiedades de cada jugador: solo hace falta
 * agregar al comprar, recorrer (para calcular patrimonio o contar casas) y,
 * a lo sumo, quitar una propiedad.</p>
 *
 * <h2>Cómo funciona</h2>
 * <p>Cada {@link NodoSimple} apunta solo al siguiente. El último apunta a
 * {@code null}: ahí se sabe que la lista terminó.</p>
 *
 * <pre>
 *   primero                              ultimo
 *     │                                    │
 *     ▼                                    ▼
 *   [ A | ●]──►[ B | ●]──►[ C | ●]──►[ D | null]
 * </pre>
 *
 * <p>Además de {@code primero} se guarda {@code ultimo}. Sin él, agregar al
 * final obligaría a caminar toda la lista para encontrar el último nodo
 * (O(n)); con él es O(1). El costo es mantenerlo al día cuando se elimina
 * el último nodo.</p>
 *
 * <p>No acepta {@code null} como dato. Así {@code null} nunca es ambiguo:
 * significa "no hay" o "no se encontró".</p>
 *
 * @param <T> tipo de los datos (por ejemplo, {@code Propiedad})
 */
public class ListaSimple<T> {

    /** Primer nodo, o {@code null} si la lista está vacía. */
    private NodoSimple<T> primero;

    /** Último nodo, o {@code null} si la lista está vacía. */
    private NodoSimple<T> ultimo;

    /** Cuántos nodos hay. Se lleva aparte para no tener que contarlos. */
    private int tamaño;

    /** Crea una lista vacía. */
    public ListaSimple() {
        this.primero = null;
        this.ultimo = null;
        this.tamaño = 0;
    }

    /** @return cuántos elementos hay */
    public int tamaño() {
        return tamaño;
    }

    /** @return {@code true} si no hay elementos */
    public boolean estaVacia() {
        return tamaño == 0;
    }

    /**
     * Devuelve el primer nodo, para recorrer la lista desde afuera:
     * <pre>
     * NodoSimple&lt;T&gt; actual = lista.getPrimero();
     * while (actual != null) {
     *     ... actual.getDato() ...
     *     actual = actual.getSiguiente();
     * }
     * </pre>
     *
     * @return el primer nodo, o {@code null} si la lista está vacía
     */
    public NodoSimple<T> getPrimero() {
        return primero;
    }

    /**
     * Agrega un dato al final. Es O(1) porque se tiene {@code ultimo}.
     *
     * @param dato el dato a agregar
     * @throws IllegalArgumentException si el dato es {@code null}
     */
    public void agregarAlFinal(T dato) {
        validarNoNulo(dato);
        NodoSimple<T> nuevo = new NodoSimple<>(dato);

        if (estaVacia()) {
            // El único nodo es a la vez el primero y el último
            primero = nuevo;
        } else {
            ultimo.setSiguiente(nuevo);
        }
        ultimo = nuevo;
        tamaño++;
    }

    /**
     * Agrega un dato al inicio. El nuevo nodo apunta al que era el primero.
     *
     * @param dato el dato a agregar
     * @throws IllegalArgumentException si el dato es {@code null}
     */
    public void agregarAlInicio(T dato) {
        validarNoNulo(dato);
        NodoSimple<T> nuevo = new NodoSimple<>(dato);

        nuevo.setSiguiente(primero); // si la lista estaba vacía, esto es null
        primero = nuevo;
        if (ultimo == null) {
            // La lista estaba vacía: el nuevo también es el último
            ultimo = nuevo;
        }
        tamaño++;
    }

    /**
     * Devuelve el dato en una posición. No hay acceso directo como en un
     * arreglo: hay que caminar {@code indice} saltos desde el primero, O(n).
     *
     * @param indice posición, de 0 a {@code tamaño() - 1}
     * @return el dato en esa posición
     * @throws IndexOutOfBoundsException si el índice está fuera de rango
     */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamaño) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango (tamaño " + tamaño + ")");
        }
        NodoSimple<T> actual = primero;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    /**
     * Busca la posición de la primera aparición de un dato (compara con
     * {@code equals}).
     *
     * @param dato el dato a buscar
     * @return su posición, o -1 si no está (o si el dato es {@code null})
     */
    public int indiceDe(T dato) {
        if (dato == null) {
            return -1;
        }
        NodoSimple<T> actual = primero;
        int indice = 0;
        while (actual != null) {
            if (actual.getDato().equals(dato)) {
                return indice;
            }
            actual = actual.getSiguiente();
            indice++;
        }
        return -1;
    }

    /**
     * @param dato el dato a buscar
     * @return {@code true} si el dato está en la lista
     */
    public boolean contiene(T dato) {
        return indiceDe(dato) != -1;
    }

    /**
     * Elimina la primera aparición de un dato.
     *
     * <p>En una lista simple un nodo no sabe quién lo apunta. Por eso, para
     * borrar un nodo hay que recorrer llevando también el nodo
     * <b>anterior</b>, y hacer que ese anterior se salte al que se borra:</p>
     * <pre>
     *   antes:    [A]──►[B]──►[C]        borrar B
     *   después:  [A]─────────►[C]       anterior.siguiente = B.siguiente
     * </pre>
     *
     * <p>Casos borde:</p>
     * <ul>
     *   <li>Borrar el primero: no hay anterior; se mueve {@code primero}.</li>
     *   <li>Borrar el último: hay que retroceder {@code ultimo} al anterior.</li>
     *   <li>Borrar el único: {@code primero} y {@code ultimo} quedan en null.</li>
     * </ul>
     *
     * @param dato el dato a eliminar
     * @return {@code true} si se encontró y eliminó; {@code false} si no estaba
     */
    public boolean eliminar(T dato) {
        if (dato == null) {
            return false;
        }

        NodoSimple<T> anterior = null;
        NodoSimple<T> actual = primero;

        while (actual != null && !actual.getDato().equals(dato)) {
            anterior = actual;
            actual = actual.getSiguiente();
        }

        if (actual == null) {
            return false; // se llegó al final sin encontrarlo
        }

        if (anterior == null) {
            // Era el primero
            primero = actual.getSiguiente();
        } else {
            anterior.setSiguiente(actual.getSiguiente());
        }

        if (actual == ultimo) {
            // Era el último: el nuevo último es el anterior
            // (null si era el único, y entonces primero también quedó null)
            ultimo = anterior;
        }

        actual.setSiguiente(null); // suelta el nodo borrado
        tamaño--;
        return true;
    }

    /**
     * Deja la lista vacía. Basta con soltar {@code primero} y {@code ultimo}:
     * los nodos que nadie apunta los borra el recolector de basura de Java.
     */
    public void vaciar() {
        primero = null;
        ultimo = null;
        tamaño = 0;
    }

    /**
     * @return los datos del primero al último, por ejemplo {@code [A, B, C]}
     */
    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder("[");
        NodoSimple<T> actual = primero;
        while (actual != null) {
            texto.append(actual.getDato());
            if (actual.getSiguiente() != null) {
                texto.append(", ");
            }
            actual = actual.getSiguiente();
        }
        return texto.append("]").toString();
    }

    private static void validarNoNulo(Object dato) {
        if (dato == null) {
            throw new IllegalArgumentException("La lista no acepta null");
        }
    }
}
