package modelo;

/**
 * Clase ListaPropiedades
 * Estructura de datos propia enlazada para manejar las propiedades de un jugador.
 * Se creo para no utilizar ArrayList ni librerias externas de Java.
 */
public class ListaPropiedades {

    private NodoPropiedad cabeza;
    private int cantidad; // Reemplaza a 'tamano' para evitar la 'ñ'

    // Inicializa la lista vacia
    public ListaPropiedades() {
        this.cabeza = null;
        this.cantidad = 0;
    }

    // Inserta una nueva propiedad al final si no es nula y no esta repetida
    public void insertar(Propiedad propiedad) {
        if (propiedad == null || contiene(propiedad)) return;

        NodoPropiedad nuevo = new NodoPropiedad(propiedad);
        if (cabeza == null) {
            cabeza = nuevo;
        } else {
            NodoPropiedad actual = cabeza;
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nuevo);
        }
        cantidad++;
    }

    // Busca y elimina una propiedad despejando los enlaces
    public boolean eliminar(Propiedad propiedad) {
        if (cabeza == null || propiedad == null) return false;

        // Si la propiedad es la primera de la lista
        if (cabeza.getPropiedad().equals(propiedad)) {
            cabeza = cabeza.getSiguiente();
            cantidad--;
            return true;
        }

        // Si esta mas adelante en la lista
        NodoPropiedad actual = cabeza;
        while (actual.getSiguiente() != null) {
            if (actual.getSiguiente().getPropiedad().equals(propiedad)) {
                actual.setSiguiente(actual.getSiguiente().getSiguiente());
                cantidad--;
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    // Revisa si una propiedad ya pertenece a la lista
    public boolean contiene(Propiedad propiedad) {
        NodoPropiedad actual = cabeza;
        while (actual != null) {
            if (actual.getPropiedad().equals(propiedad)) {
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    /**
     * Recorre los nodos y registra el movimiento en las propiedades del dueño.
     * Cada propiedad conserva su propio progreso desde que fue adquirida.
     */
    public void registrarMovimiento(Jugador jugador, int pasos) {
        NodoPropiedad actual = cabeza;
        while (actual != null) {
            Propiedad propiedad = actual.getPropiedad();
            if (propiedad.getPropietario() == jugador) propiedad.registrarMovimiento(pasos);
            actual = actual.getSiguiente();
        }
    }

    // Suma el precio de compra de todas las propiedades guardadas
    public int obtenerValorTotal() {
        int total = 0;
        NodoPropiedad actual = cabeza;
        while (actual != null) {
            total += actual.getPropiedad().getPrecioCompra();
            actual = actual.getSiguiente();
        }
        return total;
    }

    // Suma las casas de todas las propiedades (para las cartas de reparaciones)
    public int contarCasas() {
        int total = 0;
        NodoPropiedad actual = cabeza;
        while (actual != null) {
            total += actual.getPropiedad().getCantidadCasas();
            actual = actual.getSiguiente();
        }
        return total;
    }

    // Cuenta cuantas propiedades tienen hotel (para las cartas de reparaciones)
    public int contarHoteles() {
        int total = 0;
        NodoPropiedad actual = cabeza;
        while (actual != null) {
            if (actual.getPropiedad().tieneHotel()) {
                total++;
            }
            actual = actual.getSiguiente();
        }
        return total;
    }

    // Devuelve la cantidad actual de propiedades
    public int getCantidad() {
        return cantidad;
    }
}
