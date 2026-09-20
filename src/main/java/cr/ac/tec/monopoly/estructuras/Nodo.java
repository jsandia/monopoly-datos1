package cr.ac.tec.monopoly.estructuras;

/**
 * Nodo de una lista doblemente enlazada.
 * Guarda un dato y dos referencias: al nodo siguiente y al nodo anterior.
 *
 * @param <T> tipo del dato que guarda el nodo (Casilla, Jugador, etc.)
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

    public T getDato() {
        return dato;
    }

    public void setDato(T dato) {
        this.dato = dato;
    }

    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }

    public Nodo<T> getAnterior() {
        return anterior;
    }

    public void setAnterior(Nodo<T> anterior) {
        this.anterior = anterior;
    }
}
