package estructuras;

/**
 * Lista circular doblemente enlazada genérica.
 *
 * <p>En el juego es el tablero: cada nodo es una casilla. Avanzar N casillas
 * son N saltos a {@code siguiente}, y después de la última se vuelve sola a
 * la primera (Salida) sin ningún {@code if}. Las cartas de "retroceda"
 * usan {@code anterior}.</p>
 *
 * <h2>Cómo funciona</h2>
 * <p>Cada {@link Nodo} apunta al siguiente y al anterior. El último apunta
 * como siguiente a la {@code cabeza}, y la cabeza apunta como anterior al
 * último. No hay {@code null} en ningún puntero mientras la lista tenga
 * elementos.</p>
 *
 * <pre>
 *        ┌──────────────────────────────────────┐
 *        ▼                                      │
 *   cabeza ─►[ A ]◄──►[ B ]◄──►[ C ]◄──►[ D ]◄──┘
 *              ▲                          │
 *              └──────────────────────────┘
 *   (D.siguiente = A   y   A.anterior = D)
 * </pre>
 *
 * <p>Por qué solo se guarda {@code cabeza} y no también un "último": el
 * último siempre es {@code cabeza.getAnterior()}, así que llegar a él ya
 * es O(1).</p>
 *
 * <p>Ojo al recorrer: como nunca hay {@code null}, un ciclo
 * {@code while (actual != null)} no terminaría jamás. Los recorridos se
 * controlan contando hasta {@code tamaño}.</p>
 *
 * <p>No acepta {@code null} como dato.</p>
 *
 * @param <T> tipo de los datos que guarda la lista (por ejemplo, {@code Casilla})
 */
public class ListaCircularDoble<T> {

    /** Primer nodo (posición 0), o {@code null} si la lista está vacía. */
    private Nodo<T> cabeza;

    /** Cuántos nodos hay. Es lo que permite saber cuándo parar de recorrer. */
    private int tamaño;

    /** Crea una lista vacía. */
    public ListaCircularDoble() {
        this.cabeza = null;
        this.tamaño = 0;
    }

    /** @return cantidad de elementos en la lista */
    public int tamaño() {
        return tamaño;
    }

    /** @return {@code true} si la lista no tiene elementos */
    public boolean estaVacia() {
        return tamaño == 0;
    }

    /**
     * @return el nodo cabeza (posición 0), o {@code null} si la lista está
     *         vacía. Sirve para recorrer con {@code getSiguiente()} y
     *         {@code getAnterior()} desde afuera.
     */
    public Nodo<T> getCabeza() {
        return cabeza;
    }

    // =========================================================
    // AGREGAR E INSERTAR
    // =========================================================

    /**
     * Agrega un dato al final de la lista. En una lista circular, "el final"
     * es el nodo anterior a la cabeza: el nuevo queda entre el último y la
     * cabeza. O(1).
     *
     * <pre>
     *   antes:    ...[ultimo]◄──►[cabeza]...
     *   después:  ...[ultimo]◄──►[nuevo]◄──►[cabeza]...
     * </pre>
     *
     * @param dato el dato a agregar
     * @throws IllegalArgumentException si el dato es {@code null}
     */
    public void agregarAlFinal(T dato) {
        validarNoNulo(dato);
        Nodo<T> nuevo = new Nodo<>(dato);

        if (estaVacia()) {
            // Único nodo: se apunta a sí mismo en ambas direcciones
            cabeza = nuevo;
            nuevo.setSiguiente(nuevo);
            nuevo.setAnterior(nuevo);
        } else {
            enlazarAntesDe(cabeza, nuevo);
        }

        tamaño++;
    }

    /**
     * Agrega un dato al inicio de la lista. O(1).
     *
     * <p>Truco de la lista circular: agregar al inicio es lo mismo que
     * agregar al final y después correr la cabeza un paso hacia atrás, porque
     * el nodo que se acaba de agregar es justamente el anterior a la cabeza.</p>
     *
     * @param dato el dato a agregar
     * @throws IllegalArgumentException si el dato es {@code null}
     */
    public void agregarAlInicio(T dato) {
        agregarAlFinal(dato);
        cabeza = cabeza.getAnterior();
    }

    /**
     * Inserta un dato en una posición. Los que estaban desde esa posición en
     * adelante se corren un lugar.
     *
     * <p>Se busca el nodo que hoy ocupa {@code indice} y el nuevo se engancha
     * justo antes de él. Casos borde:</p>
     * <ul>
     *   <li>{@code indice == 0}: es {@link #agregarAlInicio(Object)}, porque
     *       además hay que mover la cabeza.</li>
     *   <li>{@code indice == tamaño()}: es {@link #agregarAlFinal(Object)}.</li>
     * </ul>
     *
     * @param indice posición donde va a quedar el dato, de 0 a {@code tamaño()}
     * @param dato   el dato a insertar
     * @throws IndexOutOfBoundsException si el índice está fuera de rango
     * @throws IllegalArgumentException  si el dato es {@code null}
     */
    public void insertarEn(int indice, T dato) {
        if (indice < 0 || indice > tamaño) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango para insertar (tamaño " + tamaño + ")");
        }
        validarNoNulo(dato);

        if (indice == 0) {
            agregarAlInicio(dato);
        } else if (indice == tamaño) {
            agregarAlFinal(dato);
        } else {
            enlazarAntesDe(obtenerNodo(indice), new Nodo<>(dato));
            tamaño++;
        }
    }

    // =========================================================
    // CONSULTAR
    // =========================================================

    /**
     * Devuelve el nodo de una posición.
     *
     * <p>No hay acceso directo como en un arreglo: hay que caminar. Como la
     * lista es doble y circular, se elige el camino más corto: si el índice
     * está en la primera mitad se avanza desde la cabeza con
     * {@code siguiente}; si está en la segunda, se retrocede desde la cabeza
     * con {@code anterior} (el primer paso hacia atrás ya cae en el último).
     * O(n), pero como máximo la mitad de la lista.</p>
     *
     * @param indice posición, de 0 a {@code tamaño() - 1}
     * @return el nodo en esa posición
     * @throws IndexOutOfBoundsException si el índice está fuera de rango
     */
    public Nodo<T> obtenerNodo(int indice) {
        if (indice < 0 || indice >= tamaño) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango (tamaño " + tamaño + ")");
        }
        if (indice <= tamaño / 2) {
            return avanzar(cabeza, indice);
        }
        return retroceder(cabeza, tamaño - indice);
    }

    /**
     * Devuelve el dato de una posición. Ver {@link #obtenerNodo(int)}.
     *
     * @param indice posición, de 0 a {@code tamaño() - 1}
     * @return el dato en esa posición
     * @throws IndexOutOfBoundsException si el índice está fuera de rango
     */
    public T obtener(int indice) {
        return obtenerNodo(indice).getDato();
    }

    /**
     * Busca la posición de la primera aparición de un dato (con {@code equals}).
     * El ciclo se controla con un contador, porque aquí nunca se llega a null.
     *
     * @param dato el dato a buscar
     * @return su posición, o -1 si no está (o si el dato es {@code null})
     */
    public int indiceDe(T dato) {
        if (dato == null) {
            return -1;
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < tamaño; i++) {
            if (actual.getDato().equals(dato)) {
                return i;
            }
            actual = actual.getSiguiente();
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

    // =========================================================
    // MOVERSE POR LOS NODOS
    // =========================================================

    /**
     * Camina {@code pasos} saltos hacia adelante (con {@code siguiente})
     * desde un nodo. Si se pasa del último, sigue por la cabeza: eso es lo
     * que hace circular al tablero.
     *
     * @param desde nodo de partida (debe ser de esta lista)
     * @param pasos cuántos saltos dar; 0 devuelve el mismo nodo
     * @return el nodo donde se termina
     * @throws IllegalArgumentException si el nodo es {@code null} o los pasos son negativos
     */
    public Nodo<T> avanzar(Nodo<T> desde, int pasos) {
        validarMovimiento(desde, pasos);
        Nodo<T> actual = desde;
        for (int i = 0; i < pasos; i++) {
            actual = actual.getSiguiente();
        }
        return actual;
    }

    /**
     * Camina {@code pasos} saltos hacia atrás (con {@code anterior}) desde un
     * nodo. Retroceder desde la cabeza cae en el último.
     *
     * @param desde nodo de partida (debe ser de esta lista)
     * @param pasos cuántos saltos dar; 0 devuelve el mismo nodo
     * @return el nodo donde se termina
     * @throws IllegalArgumentException si el nodo es {@code null} o los pasos son negativos
     */
    public Nodo<T> retroceder(Nodo<T> desde, int pasos) {
        validarMovimiento(desde, pasos);
        Nodo<T> actual = desde;
        for (int i = 0; i < pasos; i++) {
            actual = actual.getAnterior();
        }
        return actual;
    }

    // =========================================================
    // ELIMINAR
    // =========================================================

    /**
     * Elimina la primera aparición de un dato (con {@code equals}).
     *
     * @param dato el dato a eliminar
     * @return {@code true} si se encontró y eliminó; {@code false} si no estaba
     */
    public boolean eliminar(T dato) {
        int indice = indiceDe(dato);
        if (indice == -1) {
            return false;
        }
        eliminarEn(indice);
        return true;
    }

    /**
     * Elimina el elemento de una posición. Los de adelante se corren un lugar
     * hacia atrás.
     *
     * @param indice posición, de 0 a {@code tamaño() - 1}
     * @return el dato que se eliminó
     * @throws IndexOutOfBoundsException si el índice está fuera de rango
     */
    public T eliminarEn(int indice) {
        Nodo<T> nodo = obtenerNodo(indice);
        desenganchar(nodo);
        return nodo.getDato();
    }

    /**
     * Deja la lista vacía. Aunque los nodos se apunten entre sí en círculo,
     * el recolector de basura de Java los borra igual: lo que importa es que
     * nadie de afuera los alcance, y al soltar la cabeza ya nadie los alcanza.
     */
    public void vaciar() {
        cabeza = null;
        tamaño = 0;
    }

    // =========================================================
    // TEXTO
    // =========================================================

    /**
     * Recorre una vuelta desde la cabeza con {@code siguiente}.
     *
     * @return por ejemplo {@code [Salida, Sixaola, Arca Comunal]}
     */
    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder("[");
        Nodo<T> actual = cabeza;
        for (int i = 0; i < tamaño; i++) {
            if (i > 0) {
                texto.append(", ");
            }
            texto.append(actual.getDato());
            actual = actual.getSiguiente();
        }
        return texto.append("]").toString();
    }

    /**
     * Recorre una vuelta hacia atrás con {@code anterior}, empezando por el
     * último ({@code cabeza.getAnterior()}). Si algún puntero {@code anterior}
     * estuviera mal enlazado, no sería el inverso de {@link #toString()}.
     *
     * @return por ejemplo {@code [Arca Comunal, Sixaola, Salida]}
     */
    public String toStringInverso() {
        StringBuilder texto = new StringBuilder("[");
        if (estaVacia()) {
            return texto.append("]").toString();
        }
        Nodo<T> actual = cabeza.getAnterior();
        for (int i = 0; i < tamaño; i++) {
            if (i > 0) {
                texto.append(", ");
            }
            texto.append(actual.getDato());
            actual = actual.getAnterior();
        }
        return texto.append("]").toString();
    }

    // =========================================================
    // AYUDANTES PRIVADOS
    // =========================================================

    /**
     * Engancha {@code nuevo} justo antes de {@code referencia}. Son cuatro
     * punteros, y el orden importa: hay que guardar el anterior de
     * {@code referencia} antes de cambiarlo.
     *
     * <pre>
     *   antes:    [anterior]◄──►[referencia]
     *   después:  [anterior]◄──►[nuevo]◄──►[referencia]
     * </pre>
     */
    private void enlazarAntesDe(Nodo<T> referencia, Nodo<T> nuevo) {
        Nodo<T> anterior = referencia.getAnterior();
        nuevo.setAnterior(anterior);
        nuevo.setSiguiente(referencia);
        anterior.setSiguiente(nuevo);
        referencia.setAnterior(nuevo);
    }

    /**
     * Saca un nodo uniendo a sus dos vecinos entre sí:
     * {@code anterior.siguiente = siguiente} y {@code siguiente.anterior = anterior}.
     *
     * <p>Casos borde:</p>
     * <ul>
     *   <li>Era el único: la lista queda vacía ({@code cabeza = null}). No se
     *       puede usar el caso general porque sus vecinos son él mismo.</li>
     *   <li>Era la cabeza: la nueva cabeza es su siguiente.</li>
     * </ul>
     */
    private void desenganchar(Nodo<T> nodo) {
        if (tamaño == 1) {
            cabeza = null;
        } else {
            nodo.getAnterior().setSiguiente(nodo.getSiguiente());
            nodo.getSiguiente().setAnterior(nodo.getAnterior());
            if (nodo == cabeza) {
                cabeza = nodo.getSiguiente();
            }
        }
        // Suelta el nodo borrado para que no siga apuntando a la lista
        nodo.setSiguiente(null);
        nodo.setAnterior(null);
        tamaño--;
    }

    private static void validarNoNulo(Object dato) {
        if (dato == null) {
            throw new IllegalArgumentException("La lista no acepta null");
        }
    }

    private static void validarMovimiento(Nodo<?> desde, int pasos) {
        if (desde == null) {
            throw new IllegalArgumentException("El nodo de partida no puede ser null");
        }
        if (pasos < 0) {
            throw new IllegalArgumentException("Los pasos no pueden ser negativos: " + pasos);
        }
    }
}
