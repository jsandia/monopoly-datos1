package modelo;

import estructuras.ListaCircularDoble;
import estructuras.Nodo;

/** Historial sobre la lista propia del proyecto, sin colecciones de Java. */
public final class HistorialTransacciones {
    private final ListaCircularDoble<Transaccion> transacciones = new ListaCircularDoble<>();

    public synchronized void agregar(Transaccion transaccion) {
        if (transaccion == null) throw new IllegalArgumentException("Transaccion requerida");
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

    /** Los arreglos son resultados de consulta; el almacenamiento sigue siendo enlazado. */
    public synchronized Transaccion[] consultar(String jugador, TipoTransaccion tipo, boolean recientesPrimero) {
        Transaccion[] coincidencias = new Transaccion[transacciones.tamaño()];
        Nodo<Transaccion> actual = transacciones.getCabeza();
        if (actual != null && recientesPrimero) actual = actual.getAnterior();
        int cantidad = 0;
        // La lista es circular: el contador evita volver a procesar el primer nodo.
        for (int i = 0; i < transacciones.tamaño(); i++) {
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
