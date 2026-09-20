package cr.ac.tec.monopoly.modelo;

/**
 * Casillas que no se compran ni dan cartas: las cuatro esquinas y los impuestos.
 */
public class CasillaEspecial extends Casilla {

    public enum Tipo { SALIDA, CARCEL, PARQUEO_LIBRE, IR_A_CARCEL, IMPUESTO }

    private final Tipo tipo;
    private final int monto; // solo lo usa IMPUESTO; en las demás es 0

    public CasillaEspecial(int id, String nombre, Tipo tipo, int monto) {
        super(id, nombre);
        this.tipo = tipo;
        this.monto = monto;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getMonto() {
        return monto;
    }

    @Override
    public String getDescripcion() {
        // switch "con flechas": cada caso devuelve un valor y no hace falta break.
        // Como cubre todos los valores del enum, el compilador exige que estén todos.
        return switch (tipo) {
            case SALIDA -> "Punto de partida.\nCada vez que se pasa por aquí se cobran ₡200.";
            case CARCEL -> "Cárcel.\nQuien solo pasa por aquí está \"de visita\".";
            case PARQUEO_LIBRE -> "Parqueo libre.\nNo pasa nada, es un descanso.";
            case IR_A_CARCEL -> "Vaya directo a la cárcel,\nsin pasar por Salida ni cobrar ₡200.";
            case IMPUESTO -> "Impuesto.\nSe pagan ₡" + monto + " al banco.";
        };
    }
}
