package cr.ac.tec.monopoly.modelo;

/**
 * Casilla que se puede comprar.
 *
 * El grupo ("Café", "Rojo", "Transporte", ...) es lo que en el tablero se ve
 * como la franja de color. Falta el propietario: se agrega cuando exista Jugador.
 */
public class Propiedad extends Casilla {

    private final String grupo;
    private final int precio;
    private final int alquiler;

    public Propiedad(int id, String nombre, String grupo, int precio, int alquiler) {
        super(id, nombre); // primero se construye la parte "Casilla", siempre va de primero
        this.grupo = grupo;
        this.precio = precio;
        this.alquiler = alquiler;
    }

    public String getGrupo() {
        return grupo;
    }

    public int getPrecio() {
        return precio;
    }

    public int getAlquiler() {
        return alquiler;
    }

    @Override
    public String getDescripcion() {
        return "Propiedad del grupo " + grupo + ".\n"
                + "Precio de compra: ₡" + precio + "\n"
                + "Alquiler: ₡" + alquiler;
    }
}
