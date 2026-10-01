package estructuras;

/**
 * Nodo de una lista simplemente enlazada: guarda un dato y una sola
 * referencia, al nodo siguiente.
 *
 * <p>Es distinto de {@link Nodo} a propósito. {@code Nodo} tiene también
 * {@code anterior}, y en una lista simple ese puntero nunca se usaría:
 * sería memoria desperdiciada y algo difícil de justificar.</p>
 *
 * <pre>
 *   [ dato | siguiente ] ──► [ dato | siguiente ] ──► null
 * </pre>
 *
 * @param <T> tipo del dato que guarda el nodo (por ejemplo, {@code Propiedad})
 */
public class NodoSimple<T> {

    private T dato;
    private NodoSimple<T> siguiente;

    /**
     * Crea un nodo suelto, con {@code siguiente} en {@code null}.
     * Es la lista la que después decide a quién apunta.
     *
     * @param dato el dato a guardar
     */
    public NodoSimple(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    /** @return el dato guardado en este nodo */
    public T getDato() {
        return dato;
    }

    /** @param dato el nuevo dato para este nodo */
    public void setDato(T dato) {
        this.dato = dato;
    }

    /** @return el nodo que sigue, o {@code null} si este es el último */
    public NodoSimple<T> getSiguiente() {
        return siguiente;
    }

    /** @param siguiente el nodo al que debe apuntar este */
    public void setSiguiente(NodoSimple<T> siguiente) {
        this.siguiente = siguiente;
    }
}
