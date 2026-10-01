package estructuras;

/**
 * Lista doblemente enlazada genérica (no circular).
 *
 * <p>En el juego guarda el historial de transacciones. El enunciado pide
 * recorrerlo "desde la más antigua" y "desde la más reciente": la más
 * antigua es {@code primero} y se avanza con {@code getSiguiente()}; la más
 * reciente es {@code ultimo} y se retrocede con {@code getAnterior()}.</p>
 *
 * <h2>Cómo funciona</h2>
 * <p>Cada {@link Nodo} apunta al siguiente y al anterior. A diferencia de la
 * {@link ListaCircularDoble}, los extremos apuntan a {@code null}: el
 * {@code anterior} del primero y el {@code siguiente} del último.</p>
 *
 * <pre>
 *          primero                         ultimo
 *            │                               │
 *            ▼                               ▼
 *  null ◄──[ A ]◄──►[ B ]◄──►[ C ]◄──►[ D ]──► null
 * </pre>
 *
 * <p>Como cada nodo conoce a su anterior, eliminar un nodo ya encontrado es
 * O(1): no hace falta recorrer llevando "el de atrás", como en la
 * {@link ListaSimple}.</p>
 *
 * <p>No acepta {@code null} como dato.</p>
 *
 * @param <T> tipo de los datos (por ejemplo, {@code Transaccion})
 */
public class ListaDoble<T> {

    /** Primer nodo (el más antiguo), o {@code null} si está vacía. */
    private Nodo<T> primero;

    /** Último nodo (el más reciente), o {@code null} si está vacía. */
    private Nodo<T> ultimo;

    /** Cuántos nodos hay. */
    private int tamaño;

    /** Crea una lista vacía. */
    public ListaDoble() {
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
     * Primer nodo, para recorrer hacia adelante con {@code getSiguiente()}
     * hasta llegar a {@code null}.
     *
     * @return el primer nodo, o {@code null} si la lista está vacía
     */
    public Nodo<T> getPrimero() {
        return primero;
    }

    /**
     * Último nodo, para recorrer hacia atrás con {@code getAnterior()}
     * hasta llegar a {@code null}.
     *
     * @return el último nodo, o {@code null} si la lista está vacía
     */
    public Nodo<T> getUltimo() {
        return ultimo;
    }

    /**
     * Agrega un dato al final, O(1).
     *
     * <pre>
     *   ... ◄──►[ultimo]──► null        se engancha "nuevo":
     *   ... ◄──►[viejo ultimo]◄──►[nuevo]──► null
     * </pre>
     *
     * @param dato el dato a agregar
     * @throws IllegalArgumentException si el dato es {@code null}
     */
    public void agregarAlFinal(T dato) {
        validarNoNulo(dato);
        Nodo<T> nuevo = new Nodo<>(dato);

        if (estaVacia()) {
            primero = nuevo;
        } else {
            nuevo.setAnterior(ultimo);
            ultimo.setSiguiente(nuevo);
        }
        ultimo = nuevo;
        tamaño++;
    }

    /**
     * Agrega un dato al inicio, O(1). Es el espejo de
     * {@link #agregarAlFinal(Object)}.
     *
     * @param dato el dato a agregar
     * @throws IllegalArgumentException si el dato es {@code null}
     */
    public void agregarAlInicio(T dato) {
        validarNoNulo(dato);
        Nodo<T> nuevo = new Nodo<>(dato);

        if (estaVacia()) {
            ultimo = nuevo;
        } else {
            nuevo.setSiguiente(primero);
            primero.setAnterior(nuevo);
        }
        primero = nuevo;
        tamaño++;
    }

    /**
     * Devuelve el dato en una posición.
     *
     * <p>Como se puede caminar en los dos sentidos, se arranca desde el
     * extremo más cercano: si el índice está en la primera mitad se sale de
     * {@code primero}; si no, de {@code ultimo}. Sigue siendo O(n), pero
     * camina como máximo la mitad de la lista.</p>
     *
     * @param indice posición, de 0 a {@code tamaño() - 1}
     * @return el dato en esa posición
     * @throws IndexOutOfBoundsException si el índice está fuera de rango
     */
    public T obtener(int indice) {
        return nodoEn(indice).getDato();
    }

    /**
     * Busca la posición de la primera aparición de un dato (con {@code equals}).
     *
     * @param dato el dato a buscar
     * @return su posición, o -1 si no está (o si el dato es {@code null})
     */
    public int indiceDe(T dato) {
        if (dato == null) {
            return -1;
        }
        Nodo<T> actual = primero;
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
     * Elimina la primera aparición de un dato. Buscarlo es O(n); quitarlo,
     * una vez encontrado, es O(1).
     *
     * @param dato el dato a eliminar
     * @return {@code true} si se encontró y eliminó; {@code false} si no estaba
     */
    public boolean eliminar(T dato) {
        if (dato == null) {
            return false;
        }
        Nodo<T> actual = primero;
        while (actual != null) {
            if (actual.getDato().equals(dato)) {
                desenganchar(actual);
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    /**
     * Quita el primer elemento (el más antiguo), O(1).
     *
     * @return el dato que se quitó, o {@code null} si la lista estaba vacía
     */
    public T eliminarPrimero() {
        if (estaVacia()) {
            return null;
        }
        T dato = primero.getDato();
        desenganchar(primero);
        return dato;
    }

    /**
     * Quita el último elemento (el más reciente), O(1).
     *
     * @return el dato que se quitó, o {@code null} si la lista estaba vacía
     */
    public T eliminarUltimo() {
        if (estaVacia()) {
            return null;
        }
        T dato = ultimo.getDato();
        desenganchar(ultimo);
        return dato;
    }

    /**
     * Deja la lista vacía. Los nodos que nadie apunta los borra el
     * recolector de basura de Java.
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
        Nodo<T> actual = primero;
        while (actual != null) {
            texto.append(actual.getDato());
            if (actual.getSiguiente() != null) {
                texto.append(", ");
            }
            actual = actual.getSiguiente();
        }
        return texto.append("]").toString();
    }

    /**
     * Recorre del último al primero usando solo {@code getAnterior()}.
     * Si algún puntero {@code anterior} estuviera mal enlazado, este texto
     * no sería el inverso de {@link #toString()}.
     *
     * @return los datos del último al primero, por ejemplo {@code [C, B, A]}
     */
    public String toStringInverso() {
        StringBuilder texto = new StringBuilder("[");
        Nodo<T> actual = ultimo;
        while (actual != null) {
            texto.append(actual.getDato());
            if (actual.getAnterior() != null) {
                texto.append(", ");
            }
            actual = actual.getAnterior();
        }
        return texto.append("]").toString();
    }

    // =========================================================
    // AYUDANTES PRIVADOS
    // =========================================================

    /**
     * Camina hasta el nodo de una posición desde el extremo más cercano.
     */
    private Nodo<T> nodoEn(int indice) {
        if (indice < 0 || indice >= tamaño) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango (tamaño " + tamaño + ")");
        }
        Nodo<T> actual;
        if (indice < tamaño / 2) {
            actual = primero;
            for (int i = 0; i < indice; i++) {
                actual = actual.getSiguiente();
            }
        } else {
            actual = ultimo;
            for (int i = tamaño - 1; i > indice; i--) {
                actual = actual.getAnterior();
            }
        }
        return actual;
    }

    /**
     * Saca un nodo de la lista uniendo a sus dos vecinos entre sí.
     *
     * <pre>
     *   antes:    [A]◄──►[B]◄──►[C]          sacar B
     *   después:  [A]◄──────────►[C]
     *             A.siguiente = C    y    C.anterior = A
     * </pre>
     *
     * <p>Si el nodo no tiene anterior, era el primero: se mueve
     * {@code primero}. Si no tiene siguiente, era el último: se mueve
     * {@code ultimo}. Si era el único, los dos quedan en {@code null}.</p>
     */
    private void desenganchar(Nodo<T> nodo) {
        Nodo<T> anterior = nodo.getAnterior();
        Nodo<T> siguiente = nodo.getSiguiente();

        if (anterior == null) {
            primero = siguiente;
        } else {
            anterior.setSiguiente(siguiente);
        }

        if (siguiente == null) {
            ultimo = anterior;
        } else {
            siguiente.setAnterior(anterior);
        }

        // Suelta el nodo borrado para que no siga apuntando a la lista
        nodo.setAnterior(null);
        nodo.setSiguiente(null);
        tamaño--;
    }

    private static void validarNoNulo(Object dato) {
        if (dato == null) {
            throw new IllegalArgumentException("La lista no acepta null");
        }
    }
}
