package modelo;

import estructuras.ListaDoble;
import estructuras.Nodo;

/**
 * Historial de transacciones de una partida, guardado en una ListaDoble.
 *
 * Por qué ListaDoble: el enunciado pide recorrerlo desde la más antigua y
 * desde la más reciente. La más antigua es el primer nodo y se avanza con
 * getSiguiente(); la más reciente es el último nodo y se retrocede con
 * getAnterior(). Agregar al final es O(1).
 *
 * Los métodos de consulta devuelven arreglos con el resultado, pero el
 * almacenamiento es siempre la lista enlazada.
 */
public final class HistorialTransacciones {
    private final ListaDoble<Transaccion> transacciones = new ListaDoble<>();

    public synchronized void agregar(Transaccion transaccion) {
        if (transaccion == null) throw new IllegalArgumentException("Transacción requerida");
        transacciones.agregarAlFinal(transaccion);
    }

    public synchronized int getCantidad() {
        return transacciones.tamaño();
    }

    public Transaccion[] desdeLaMasAntigua() {
        return consultar(null, null, false);
    }

    public Transaccion[] desdeLaMasReciente() {
        return consultar(null, null, true);
    }

    public Transaccion[] buscarPorJugador(String identificador) {
        if (identificador == null) throw new IllegalArgumentException("Jugador requerido");
        return consultar(identificador, null, false);
    }

    public Transaccion[] buscarPorTipo(TipoTransaccion tipo) {
        if (tipo == null) throw new IllegalArgumentException("Tipo requerido");
        return consultar(null, tipo, false);
    }

    /**
     * Recorre la lista en el sentido pedido y se queda con las que cumplen
     * los filtros. Un filtro en null significa "cualquiera".
     *
     * @param jugador          id del jugador que debe participar (origen o destino), o null
     * @param tipo             tipo de transacción, o null
     * @param recientesPrimero true: desde el último nodo con getAnterior();
     *                         false: desde el primero con getSiguiente()
     */
    public synchronized Transaccion[] consultar(String jugador, TipoTransaccion tipo, boolean recientesPrimero) {
        Transaccion[] coincidencias = new Transaccion[transacciones.tamaño()];
        int cantidad = 0;

        Nodo<Transaccion> actual = recientesPrimero ? transacciones.getUltimo() : transacciones.getPrimero();
        // No es circular: el recorrido termina al llegar a null en cualquiera de los dos sentidos
        while (actual != null) {
            Transaccion transaccion = actual.getDato();
            if ((jugador == null || transaccion.participaJugador(jugador))
                    && (tipo == null || transaccion.tipo() == tipo)) {
                coincidencias[cantidad++] = transaccion;
            }
            actual = recientesPrimero ? actual.getAnterior() : actual.getSiguiente();
        }

        Transaccion[] resultado = new Transaccion[cantidad];
        System.arraycopy(coincidencias, 0, resultado, 0, cantidad);
        return resultado;
    }

    public String imprimirTodas() {
        return imprimir(null, null, false);
    }

    public String imprimir(String jugador, TipoTransaccion tipo, boolean recientesPrimero) {
        StringBuilder texto = new StringBuilder();
        for (Transaccion transaccion : consultar(jugador, tipo, recientesPrimero)) {
            texto.append(transaccion.formatear()).append(System.lineSeparator());
        }
        return texto.toString();
    }
}
