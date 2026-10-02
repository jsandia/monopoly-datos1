package modelo;

import config.Configuracion;

/**
 * Casilla que se puede comprar y en la que se construyen casas y hoteles.
 *
 * Además de lo que pide el enunciado (id, nombre, precio de compra,
 * alquiler y propietario) lleva las casas, el hotel y cuánto ha recorrido
 * el dueño desde que la compró: para construir hay que dar una vuelta
 * completa al tablero después de la compra.
 */
public class Propiedad extends Casilla {

    private final String grupo;
    private final int precio;
    private final int alquiler;
    private final int precioCasa;           // 0 = no se construye (transportes y servicios)
    private final int[] alquileresConstrucciones; // con 1, 2, 3, 4 casas y con hotel
    private final Tablero tablero;          // para saber si el dueño tiene el grupo completo

    private Jugador propietario;
    private int casas;
    private boolean tieneHotel;
    private int recorridoDesdeCompra;
    private boolean vueltaDesdeCompraCompletada;

    /**
     * @param precio   precio de compra
     * @param alquiler alquiler sin casas
     * @param tablero  el tablero donde está esta propiedad
     */
    public Propiedad(int id, String nombre, String grupo, int precio, int alquiler, Tablero tablero) {
        super(id, nombre);
        this.grupo = grupo;
        this.precio = precio;
        this.alquiler = alquiler;
        this.tablero = tablero;
        this.alquileresConstrucciones = alquileresClasicos(alquiler);
        this.precioCasa = switch (grupo) {
            case "Café", "Celeste" -> 50;
            case "Rosado", "Naranja" -> 100;
            case "Rojo", "Amarillo" -> 150;
            case "Verde", "Azul" -> 200;
            case "Transporte", "Servicios" -> 0;
            default -> precio / 2;
        };
    }

    // Valores de las escrituras del tablero clásico: 1, 2, 3, 4 casas y hotel
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

    public String getGrupo() {
        return grupo;
    }

    public int getPrecioCompra() {
        return precio;
    }

    public int getAlquiler() {
        return alquiler;
    }

    // =========================================================
    // PROPIETARIO
    // =========================================================

    public Jugador getPropietario() {
        return propietario;
    }

    public boolean tienePropietario() {
        return propietario != null;
    }

    /** Al cambiar de dueño, el nuevo empieza a contar su vuelta desde cero. */
    public void setPropietario(Jugador propietario) {
        if (this.propietario != propietario) {
            recorridoDesdeCompra = 0;
            vueltaDesdeCompraCompletada = false;
        }
        this.propietario = propietario;
    }

    /**
     * Suma lo que caminó el dueño (negativo si retrocedió). Una vez cumplida
     * la vuelta, queda habilitada para siempre.
     */
    void registrarMovimiento(int pasos) {
        if (propietario == null || vueltaDesdeCompraCompletada) return;
        recorridoDesdeCompra += pasos;
        if (recorridoDesdeCompra >= tablero.tamaño()) vueltaDesdeCompraCompletada = true;
    }

    // =========================================================
    // ALQUILER
    // =========================================================

    /**
     * Con hotel o casas se usa la tabla de la escritura. Sin construcciones,
     * el alquiler se duplica si el dueño tiene todo el grupo de color.
     */
    public int calcularAlquiler() {
        if (tieneHotel) return alquileresConstrucciones[4];
        if (casas > 0) return alquileresConstrucciones[casas - 1];
        if (esEdificable() && tablero.tieneGrupoCompleto(propietario, grupo)) return alquiler * 2;
        return alquiler;
    }

    // =========================================================
    // CASAS Y HOTELES
    // =========================================================

    public boolean esEdificable() {
        return precioCasa > 0;
    }

    public int getPrecioCasa() {
        return precioCasa;
    }

    /** El hotel cuesta lo mismo que una casa del grupo. */
    public int getPrecioHotel() {
        return precioCasa;
    }

    public int getCantidadCasas() {
        return casas;
    }

    public boolean tieneHotel() {
        return tieneHotel;
    }

    /** @return null si el jugador puede construir la próxima casa aquí; si no, el motivo */
    public String motivoNoConstruir(Jugador jugador) {
        if (jugador == null || propietario != jugador) return "La propiedad no pertenece al jugador en turno.";
        if (!esEdificable()) return "No se construyen casas en transportes ni servicios.";
        if (tieneHotel || casas >= Configuracion.MAX_CASAS_POR_PROPIEDAD) return "La propiedad ya tiene el máximo de casas o un hotel.";
        if (!vueltaDesdeCompraCompletada) {
            return "Debes completar una vuelta desde la compra de " + getNombre()
                    + ". Te faltan " + (tablero.tamaño() - recorridoDesdeCompra) + " casillas de recorrido.";
        }
        return null;
    }

    /** @return true si se pudo agregar una casa (máximo 4 y sin hotel) */
    public boolean agregarCasa() {
        if (!esEdificable() || tieneHotel || casas >= Configuracion.MAX_CASAS_POR_PROPIEDAD) return false;
        casas++;
        return true;
    }

    /** @return true si se pudo cambiar las 4 casas por un hotel */
    public boolean construirHotel() {
        if (!esEdificable() || tieneHotel || casas != Configuracion.MAX_CASAS_POR_PROPIEDAD) return false;
        casas = 0;
        tieneHotel = true;
        return true;
    }

    /**
     * Deja la propiedad como al inicio de la partida: sin dueño, sin casas y
     * sin hotel. Se usa cuando su dueño queda en bancarrota; las casas y el
     * hotel los recupera el Banco antes de llamar a esto.
     */
    void devolverAlBanco() {
        setPropietario(null);
        casas = 0;
        tieneHotel = false;
    }

    @Override
    public String getDescripcion() {
        String dueño = (propietario != null) ? "Dueño: " + propietario.getNombre() : "Sin dueño";
        return "Propiedad del grupo " + grupo + ".\n"
                + "Precio de compra: ₡" + precio + "\n"
                + "Alquiler base: ₡" + alquiler + "\n"
                + "Estado: " + dueño + "\n"
                + "Casas: " + casas + " | Hotel: " + (tieneHotel ? "Sí" : "No");
    }
}
