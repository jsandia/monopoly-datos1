package cr.ac.tec.monopoly.estructuras;

/**
 * Lista circular doblemente enlazada.
 *
 * Cada nodo apunta al siguiente y al anterior. El último nodo apunta
 * como siguiente al primero (cabeza), y la cabeza apunta como anterior
 * al último. Así nunca hay un "final": recorriendo siempre se vuelve al inicio.
 *
 * Se usa para el tablero: avanzar N casillas son N saltos a siguiente,
 * y retroceder son saltos a anterior.
 *
 * @param <T> tipo de los datos que guarda la lista
 */
public class ListaCircularDoble<T> {

    private Nodo<T> cabeza;
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

    /** @return true si la lista no tiene elementos */
    public boolean estaVacia() {
        return tamaño == 0;
    }

    /**
     * Agrega un dato al final de la lista.
     * En una lista circular, "el final" es el nodo anterior a la cabeza.
     *
     * @param dato el dato a agregar
     */
    public void agregarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);

        if (estaVacia()) {
            // Único nodo: se apunta a sí mismo en ambas direcciones
            cabeza = nuevo;
            nuevo.setSiguiente(nuevo);
            nuevo.setAnterior(nuevo);
        } else {
            Nodo<T> ultimo = cabeza.getAnterior();

            // Enganchar el nuevo entre el último y la cabeza
            nuevo.setAnterior(ultimo);
            nuevo.setSiguiente(cabeza);
            ultimo.setSiguiente(nuevo);
            cabeza.setAnterior(nuevo);
        }

        tamaño++;
    }

    /**
     * @return el nodo cabeza (null si la lista está vacía). Sirve para recorrer
     *         la lista con getSiguiente()/getAnterior() desde afuera.
     */
    public Nodo<T> getCabeza() {
        return cabeza;
    }

    /**
     * Agrega un dato al inicio de la lista.
     * Truco de la lista circular: agregar al inicio es lo mismo que agregar al
     * final y luego correr la cabeza un paso hacia atrás.
     *
     * @param dato el dato a agregar
     */
    public void agregarAlInicio(T dato) {
        agregarAlFinal(dato);
        cabeza = cabeza.getAnterior();
    }

    /**
     * Devuelve el dato en la posición indicada, contando desde la cabeza.
     * A diferencia de un arreglo, no hay acceso directo: hay que caminar
     * "indice" saltos desde la cabeza. Por eso es O(n).
     *
     * @param indice posición (0 = cabeza)
     * @return el dato en esa posición
     * @throws IndexOutOfBoundsException si el índice no está entre 0 y tamaño-1
     */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamaño) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango (tamaño " + tamaño + ")");
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    /**
     * Elimina el primer nodo cuyo dato sea igual (equals) al dado.
     *
     * Como es circular, el recorrido se controla con un contador (tamaño)
     * y no con "hasta llegar a null": aquí nunca hay null.
     *
     * @param dato el dato a eliminar
     * @return true si se encontró y eliminó, false si no estaba
     */
    public boolean eliminar(T dato) {
        Nodo<T> actual = cabeza;
        for (int i = 0; i < tamaño; i++) {
            if (actual.getDato().equals(dato)) {
                if (tamaño == 1) {
                    cabeza = null;
                } else {
                    // Desenganchar: el anterior salta al siguiente y el siguiente
                    // apunta hacia atrás al anterior. Nadie apunta ya a "actual".
                    actual.getAnterior().setSiguiente(actual.getSiguiente());
                    actual.getSiguiente().setAnterior(actual.getAnterior());
                    // Si eliminamos la cabeza, la cabeza pasa a ser el siguiente
                    if (actual == cabeza) {
                        cabeza = actual.getSiguiente();
                    }
                }
                tamaño--;
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    /**
     * Representación de la lista recorriendo desde la cabeza.
     *
     * @return por ejemplo "[Salida, Mediterráneo, Arca]"
     */
    @Override
    public String toString() {
        if (estaVacia()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        Nodo<T> actual = cabeza;
        for (int i = 0; i < tamaño; i++) {
            sb.append(actual.getDato());
            if (i < tamaño - 1) {
                sb.append(", ");
            }
            actual = actual.getSiguiente();
        }
        sb.append("]");
        return sb.toString();
    }
}

