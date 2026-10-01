package estructuras;

/**
 * Nodo de una lista doblemente enlazada: guarda un dato y dos referencias,
 * al nodo siguiente y al nodo anterior.
 *
 * <pre>
 *   ◄── anterior ──[ dato ]── siguiente ──►
 * </pre>
 *
 * <p>Lo usan {@link ListaDoble} (donde los extremos apuntan a {@code null})
 * y {@link ListaCircularDoble} (donde nunca hay {@code null}: el último
 * apunta al primero y viceversa).</p>
 *
 * @param <T> tipo del dato que guarda el nodo (Casilla, Transaccion, etc.)
 */
public class Nodo<T> {

    private T dato;
    private Nodo<T> siguiente;
    private Nodo<T> anterior;

    /**
     * Crea un nodo suelto: guarda el dato y deja ambas referencias en null.
     * Es la lista la que después decide a quién apunta.
     *
     * @param dato el dato a guardar
     */
    public Nodo(T dato) {
        this.dato = dato;
        this.siguiente = null;
        this.anterior = null;
    }

    /** @return el dato guardado en este nodo */
    public T getDato() {
        return dato;
    }

    /** @param dato el nuevo dato para este nodo */
    public void setDato(T dato) {
        this.dato = dato;
    }

    /** @return el nodo siguiente ({@code null} solo en una lista no circular) */
    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    /** @param siguiente el nodo al que debe apuntar hacia adelante */
    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }

    /** @return el nodo anterior ({@code null} solo en una lista no circular) */
    public Nodo<T> getAnterior() {
        return anterior;
    }

    /** @param anterior el nodo al que debe apuntar hacia atrás */
    public void setAnterior(Nodo<T> anterior) {
        this.anterior = anterior;
    }
}
