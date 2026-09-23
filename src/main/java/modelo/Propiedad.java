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

        // Valores por defecto proporcionales al precio de compra
        this.precioCasa = precio / 2;
        this.precioHotel = precio / 2;
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
        this.propietario = propietario;
    }

    public boolean tienePropietario() {
        return propietario != null;
    }

    // --- ALQUILER ---
    public int calcularAlquiler() {
        if (hipotecada) return 0;
        if (tieneHotel) return alquiler * 5;
        if (casas > 0) return alquiler * (1 + casas);
        return alquiler;
    }

    // --- CASAS Y HOTELES ---
    public int getCantidadCasas() {
        return casas;
    }

    public int getPrecioCasa() {
        return precioCasa;
    }

    public boolean agregarCasa() {
        if (casas < 4 && !tieneHotel) {
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
        if (casas == 4 && !tieneHotel) {
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