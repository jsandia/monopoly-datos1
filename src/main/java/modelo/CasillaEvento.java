package modelo;

/**
 * Casilla donde el jugador toma una carta del mazo.
 * Hay dos mazos, como en el Monopoly clásico: Suerte y Arca Comunal.
 */
public class CasillaEvento extends Casilla {

    // Un enum es un tipo con valores fijos. Mejor que un String porque el
    // compilador no deja escribir "SUERTE" mal, y se puede usar en switch.
    public enum Tipo { SUERTE, ARCA_COMUNAL }

    private final Tipo tipo;

    public CasillaEvento(int id, String nombre, Tipo tipo) {
        super(id, nombre);
        this.tipo = tipo;
    }

    public Tipo getTipo() {
        return tipo;
    }

    @Override
    public String getDescripcion() {
        String mazo = (tipo == Tipo.SUERTE) ? "Suerte" : "Arca Comunal";
        return "Casilla de evento.\nAl caer aquí se toma una carta del mazo de " + mazo
                + " y se aplica lo que diga.";
    }
}
