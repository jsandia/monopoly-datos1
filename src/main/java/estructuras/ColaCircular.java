package estructuras;

/**
 * Cola circular genérica sobre un arreglo de tamaño fijo (FIFO: el primero
 * que entra es el primero que sale).
 *
 * <p>En el juego se usa para los mazos de Suerte y Arca Comunal: se saca la
 * carta del frente con {@link #desencolar()}, se aplica, y se vuelve a meter
 * al final con {@link #encolar(Object)}. Así "la carta usada pasa al final" y
 * el mazo nunca se acaba.</p>
 *
 * <h2>Cómo funciona</h2>
 * <p>No se mueven los elementos dentro del arreglo. Lo que se mueven son dos
 * índices:</p>
 * <ul>
 *   <li>{@code frente}: dónde está el próximo elemento que va a salir.</li>
 *   <li>{@code fin}: dónde está el último elemento que entró.</li>
 * </ul>
 * <p>Cuando un índice llega al final del arreglo, vuelve a 0 con el operador
 * módulo: {@code (indice + 1) % capacidad}. Por eso es "circular": las
 * casillas que se liberaron al principio se reutilizan.</p>
 *
 * <pre>
 * capacidad = 4, después de encolar A, B, C y desencolar A:
 *
 *   índice:   0     1     2     3
 *           [   ] [ B ] [ C ] [   ]
 *                   ^     ^
 *                frente  fin          cantidad = 2
 *
 * encolar D y E: fin avanza 2 → 3 → 0 (da la vuelta)
 *           [ E ] [ B ] [ C ] [ D ]
 *             ^     ^
 *            fin  frente              cantidad = 4 (llena)
 * </pre>
 *
 * <p>Todas las operaciones son O(1): no hay ciclos, solo cuentas con índices.</p>
 *
 * @param <T> tipo de los elementos (por ejemplo, {@code CartaEvento})
 */
public class ColaCircular<T> {

    /**
     * Los elementos. Es {@code Object[]} y no {@code T[]} porque Java no deja
     * hacer {@code new T[n]}: los genéricos se "borran" al compilar (type
     * erasure) y en tiempo de ejecución Java no sabe qué es T. Se guarda como
     * Object y se convierte a T al sacarlo.
     */
    private final Object[] elementos;

    /** Índice del elemento que va a salir en el próximo {@link #desencolar()}. */
    private int frente;

    /** Índice del último elemento que entró. Empieza en -1 porque no hay ninguno. */
    private int fin;

    /**
     * Cuántos elementos hay. Hace falta porque con solo {@code frente} y
     * {@code fin} no se distingue una cola vacía de una llena: en las dos
     * {@code fin} queda justo detrás de {@code frente}.
     */
    private int cantidad;

    /**
     * Crea una cola vacía.
     *
     * @param capacidad cuántos elementos caben como máximo
     * @throws IllegalArgumentException si la capacidad no es positiva
     */
    public ColaCircular(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que 0: " + capacidad);
        }
        this.elementos = new Object[capacidad];
        this.frente = 0;
        this.fin = -1;
        this.cantidad = 0;
    }

    /**
     * Mete un elemento al final de la cola.
     *
     * @param dato el elemento a agregar
     * @return {@code true} si entró; {@code false} si la cola estaba llena
     */
    public boolean encolar(T dato) {
        if (estaLlena()) {
            return false;
        }
        fin = (fin + 1) % elementos.length; // si fin era el último índice, vuelve a 0
        elementos[fin] = dato;
        cantidad++;
        return true;
    }

    /**
     * Saca el elemento del frente (el más antiguo).
     *
     * @return el elemento que salió, o {@code null} si la cola estaba vacía
     */
    public T desencolar() {
        if (estaVacia()) {
            return null;
        }
        T dato = elemento(frente);
        elementos[frente] = null; // suelta la referencia para no guardar basura
        frente = (frente + 1) % elementos.length;
        cantidad--;
        return dato;
    }

    /**
     * Mira el elemento del frente sin sacarlo.
     *
     * @return el elemento del frente, o {@code null} si la cola está vacía
     */
    public T verFrente() {
        if (estaVacia()) {
            return null;
        }
        return elemento(frente);
    }

    /** @return {@code true} si no hay elementos */
    public boolean estaVacia() {
        return cantidad == 0;
    }

    /** @return {@code true} si ya no cabe ningún elemento más */
    public boolean estaLlena() {
        return cantidad == elementos.length;
    }

    /** @return cuántos elementos hay ahora en la cola */
    public int tamaño() {
        return cantidad;
    }

    /** @return cuántos elementos caben como máximo */
    public int capacidad() {
        return elementos.length;
    }

    /**
     * Convierte la casilla del arreglo a T. El {@code @SuppressWarnings} calla
     * el aviso del compilador: el cast es seguro porque {@link #encolar(Object)}
     * solo deja meter elementos de tipo T.
     */
    @SuppressWarnings("unchecked")
    private T elemento(int indice) {
        return (T) elementos[indice];
    }

    /**
     * @return los elementos del frente al fin, por ejemplo {@code [B, C, D]}
     */
    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder("[");
        for (int i = 0; i < cantidad; i++) {
            int indice = (frente + i) % elementos.length;
            if (i > 0) {
                texto.append(", ");
            }
            texto.append(elementos[indice]);
        }
        return texto.append("]").toString();
    }
}
