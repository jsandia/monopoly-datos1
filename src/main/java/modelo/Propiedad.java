package modelo;

/**
 * Casilla que se puede comprar, administrar casas/hoteles e hipotecar.
 * Hereda de Casilla para encajar en el Tablero de la GUI,
 * e incluye toda la logica financiera para funcionar con el Banco.
 */
public class Propiedad extends Casilla {

    private final String grupo;
    private final int precio;
    private final int alquiler;
    private final int[] alquileresConstrucciones;
    private Tablero tablero;
    private long recorridoDesdeCompra;
    private boolean vueltaDesdeCompraCompletada;

    // Atributos para gestion del Banco y Jugadores
    private Jugador propietario;
    private int casas;
    private boolean tieneHotel;
    private boolean hipotecada;

    private int precioCasa;
    private int precioHotel;
    private int valorHipoteca;

    public Propiedad(int id, String nombre, String grupo, int precio, int alquiler) {
        super(id, nombre);
        this.grupo = grupo;
        this.precio = precio;
        this.alquiler = alquiler;
        this.propietario = null;
        this.casas = 0;
        this.tieneHotel = false;
        this.hipotecada = false;

        this.alquileresConstrucciones = alquileresClasicos(alquiler);
        this.precioCasa = switch (grupo) {
            case "Café", "Celeste" -> 50;
            case "Rosado", "Naranja" -> 100;
            case "Rojo", "Amarillo" -> 150;
            case "Verde", "Azul" -> 200;
            case "Transporte", "Servicios" -> 0;
            default -> precio / 2;
        };
        this.precioHotel = precioCasa;
        this.valorHipoteca = precio / 2;
    }

    // --- GETTERS BASICOS ---
    public String getGrupo() {
        return grupo;
    }

    public int getPrecio() {
        return precio;
    }

    public int getPrecioCompra() {
        return precio;
    }

    public int getAlquiler() {
        return alquiler;
    }

    // --- PROPIETARIO ---
    public Jugador getPropietario() {
        return propietario;
    }

    public void setPropietario(Jugador propietario) {
        if (this.propietario != propietario) {
            recorridoDesdeCompra = 0;
            vueltaDesdeCompraCompletada = false;
        }
        this.propietario = propietario;
    }

    /** Cuenta el recorrido desde la compra. Una vuelta cumplida permanece habilitada. */
    void registrarMovimiento(int pasos) {
        if (propietario == null || vueltaDesdeCompraCompletada) return;
        recorridoDesdeCompra += pasos;
        int longitudVuelta = tablero == null ? 40 : tablero.tamaño();
        if (recorridoDesdeCompra >= longitudVuelta) vueltaDesdeCompraCompletada = true;
    }

    public boolean tienePropietario() {
        return propietario != null;
    }

    // --- ALQUILER ---
    public int calcularAlquiler() {
        if (hipotecada) return 0;
        if (tieneHotel) return alquileresConstrucciones[4];
        if (casas > 0) return alquileresConstrucciones[casas - 1];
        if (esEdificable() && tablero != null && tablero.tieneGrupoCompleto(propietario, grupo)) {
            return alquiler * 2;
        }
        return alquiler;
    }

    // Valores de las escrituras del tablero clásico: 1, 2, 3, 4 casas y hotel.
    private static int[] alquileresClasicos(int base) {
        return switch (base) {
            case 2 -> new int[]{10, 30, 90, 160, 250};
            case 4 -> new int[]{20, 60, 180, 320, 450};
            case 6 -> new int[]{30, 90, 270, 400, 550};
            case 8 -> new int[]{40, 100, 300, 450, 600};
            case 10 -> new int[]{50, 150, 450, 625, 750};
            case 12 -> new int[]{60, 180, 500, 700, 900};
            case 14 -> new int[]{70, 200, 550, 750, 950};
            case 16 -> new int[]{80, 220, 600, 800, 1000};
            case 18 -> new int[]{90, 250, 700, 875, 1050};
            case 20 -> new int[]{100, 300, 750, 925, 1100};
            case 22 -> new int[]{110, 330, 800, 975, 1150};
            case 24 -> new int[]{120, 360, 850, 1025, 1200};
            case 26 -> new int[]{130, 390, 900, 1100, 1275};
            case 28 -> new int[]{150, 450, 1000, 1200, 1400};
            case 35 -> new int[]{175, 500, 1100, 1300, 1500};
            case 50 -> new int[]{200, 600, 1400, 1700, 2000};
            default -> new int[]{base * 2, base * 3, base * 4, base * 5, base * 6};
        };
    }

    void setTablero(Tablero tablero) {
        this.tablero = tablero;
    }

    public boolean esEdificable() {
        return precioCasa > 0;
    }

    /** null si las reglas de la propiedad permiten comprar la próxima casa. */
    public String motivoNoConstruir(Jugador jugador) {
        if (jugador == null || propietario != jugador) return "La propiedad no pertenece al jugador en turno.";
        if (!esEdificable()) return "No se construyen casas en transportes ni servicios.";
        if (hipotecada) return "La propiedad está hipotecada.";
        if (tieneHotel || casas >= 4) return "La propiedad ya tiene el máximo de casas o un hotel.";
        if (!vueltaDesdeCompraCompletada) {
            int longitudVuelta = tablero == null ? 40 : tablero.tamaño();
            return "Debes completar una vuelta desde la compra de " + getNombre()
                    + ". Te faltan " + (longitudVuelta - recorridoDesdeCompra) + " casillas de recorrido.";
        }
        return null;
    }

    // --- CASAS Y HOTELES ---
    public int getCantidadCasas() {
        return casas;
    }

    public int getPrecioCasa() {
        return precioCasa;
    }

    public boolean agregarCasa() {
        if (esEdificable() && !hipotecada && casas < 4 && !tieneHotel) {
            casas++;
            return true;
        }
        return false;
    }

    public boolean quitarCasa() {
        if (casas > 0) {
            casas--;
            return true;
        }
        return false;
    }

    public boolean tieneHotel() {
        return tieneHotel;
    }

    public int getPrecioHotel() {
        return precioHotel;
    }

    public boolean construirHotel() {
        if (esEdificable() && !hipotecada && casas == 4 && !tieneHotel) {
            casas = 0;
            tieneHotel = true;
            return true;
        }
        return false;
    }

    public boolean quitarHotel() {
        if (tieneHotel) {
            tieneHotel = false;
            casas = 4;
            return true;
        }
        return false;
    }

    // --- HIPOTECA ---
    public boolean estaHipotecada() {
        return hipotecada;
    }

    public int getValorHipoteca() {
        return valorHipoteca;
    }

    public boolean hipotecar() {
        if (!hipotecada && casas == 0 && !tieneHotel) {
            hipotecada = true;
            return true;
        }
        return false;
    }

    public boolean quitarHipoteca() {
        if (hipotecada) {
            hipotecada = false;
            return true;
        }
        return false;
    }

    @Override
    public String getDescripcion() {
        String estadoProp = (propietario != null) ? "Dueño: " + propietario.getNombre() : "Sin dueño";
        return "Propiedad del grupo " + grupo + ".\n"
                + "Precio de compra: ₡" + precio + "\n"
                + "Alquiler base: ₡" + alquiler + "\n"
                + "Estado: " + estadoProp + "\n"
                + "Casas: " + casas + " | Hotel: " + (tieneHotel ? "Sí" : "No");
    }
}
