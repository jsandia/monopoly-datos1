package cr.ac.tec.monopoly.modelo;

/**
 * Clase base de toda casilla del tablero.
 *
 * Es "abstract": no se puede hacer "new Casilla(...)". Solo existen sus hijas
 * (Propiedad, CasillaEvento, CasillaEspecial). Lo común a todas (id y nombre)
 * vive aquí; lo que cambia según el tipo se declara abstracto y cada hija lo
 * implementa a su manera. Eso es la herencia + polimorfismo que pide el enunciado.
 */
public abstract class Casilla {

    // "final": una vez creada la casilla, su id y nombre no cambian
    private final int id;
    private final String nombre;

    // "protected": solo las hijas pueden llamarlo (con super(...))
    protected Casilla(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    /**
     * Sin cuerpo: cada hija está obligada a escribir el suyo.
     * Al llamar casilla.getDescripcion() Java ejecuta la versión de la hija
     * real, aunque la variable esté declarada como Casilla.
     */
    public abstract String getDescripcion();

    @Override
    public String toString() {
        return nombre;
    }
}
