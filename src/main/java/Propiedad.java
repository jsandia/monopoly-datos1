/**
 * Clase Propiedad
 *
 * Representa una propiedad comprable del tablero
 * de Monopoly CR.
 *
 * Una propiedad puede tener:
 *
 * - Un propietario
 * - Casas
 * - Un hotel
 * - Una hipoteca
 * - Diferentes valores de alquiler
 */
public class Propiedad {

    // =========================================================
    // ATRIBUTOS
    // =========================================================

    private String nombre;

    // Grupo de color al que pertenece la propiedad.
    private String grupoColor;

    // Precio para comprar la propiedad.
    private int precioCompra;

    // =========================================================
    // ALQUILERES
    // =========================================================

    // Alquiler sin casas.
    private int alquilerSinCasas;

    // Alquiler con 1 casa.
    private int alquilerUnaCasa;

    // Alquiler con 2 casas.
    private int alquilerDosCasas;

    // Alquiler con 3 casas.
    private int alquilerTresCasas;

    // Alquiler con 4 casas.
    private int alquilerCuatroCasas;

    // Alquiler con hotel.
    private int alquilerHotel;

    // =========================================================
    // CONSTRUCCIONES
    // =========================================================

    // Precio de cada casa.
    private int precioCasa;

    // Precio del hotel.
    private int precioHotel;

    // Cantidad de casas construidas.
    private int cantidadCasas;

    // Indica si tiene hotel.
    private boolean hotel;

    // =========================================================
    // HIPOTECA
    // =========================================================

    // Indica si la propiedad está hipotecada.
    private boolean hipotecada;

    // Dinero que recibe el jugador al hipotecarla.
    private int valorHipoteca;

    // =========================================================
    // PROPIETARIO
    // =========================================================

    // Jugador dueño de la propiedad.
    private Jugador propietario;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Crea una nueva propiedad.
     *
     * @param nombre nombre de la propiedad
     * @param grupoColor grupo de color
     * @param precioCompra precio de compra
     * @param alquilerSinCasas alquiler sin casas
     * @param alquilerUnaCasa alquiler con una casa
     * @param alquilerDosCasas alquiler con dos casas
     * @param alquilerTresCasas alquiler con tres casas
     * @param alquilerCuatroCasas alquiler con cuatro casas
     * @param alquilerHotel alquiler con hotel
     * @param precioCasa precio de construir una casa
     * @param precioHotel precio de construir un hotel
     * @param valorHipoteca valor que paga el banco por la hipoteca
     */
    public Propiedad(
            String nombre,
            String grupoColor,
            int precioCompra,
            int alquilerSinCasas,
            int alquilerUnaCasa,
            int alquilerDosCasas,
            int alquilerTresCasas,
            int alquilerCuatroCasas,
            int alquilerHotel,
            int precioCasa,
            int precioHotel,
            int valorHipoteca
    ) {

        this.nombre = nombre;
        this.grupoColor = grupoColor;

        this.precioCompra = precioCompra;

        this.alquilerSinCasas = alquilerSinCasas;
        this.alquilerUnaCasa = alquilerUnaCasa;
        this.alquilerDosCasas = alquilerDosCasas;
        this.alquilerTresCasas = alquilerTresCasas;
        this.alquilerCuatroCasas = alquilerCuatroCasas;
        this.alquilerHotel = alquilerHotel;

        this.precioCasa = precioCasa;
        this.precioHotel = precioHotel;

        this.valorHipoteca = valorHipoteca;

        // La propiedad empieza sin construcciones.
        this.cantidadCasas = 0;
        this.hotel = false;

        // La propiedad empieza sin hipoteca.
        this.hipotecada = false;

        // La propiedad empieza sin propietario.
        this.propietario = null;
    }


    // =========================================================
    // INFORMACIÓN BÁSICA
    // =========================================================

    /**
     * Devuelve el nombre de la propiedad.
     */
    public String getNombre() {
        return nombre;
    }


    /**
     * Devuelve el grupo de color.
     */
    public String getGrupoColor() {
        return grupoColor;
    }


    /**
     * Devuelve el precio de compra.
     */
    public int getPrecioCompra() {
        return precioCompra;
    }


    // =========================================================
    // PRECIOS DE CONSTRUCCIÓN
    // =========================================================

    /**
     * Devuelve el precio de una casa.
     */
    public int getPrecioCasa() {
        return precioCasa;
    }


    /**
     * Devuelve el precio de un hotel.
     */
    public int getPrecioHotel() {
        return precioHotel;
    }


    // =========================================================
    // CASAS Y HOTELES
    // =========================================================

    /**
     * Devuelve la cantidad de casas construidas.
     */
    public int getCantidadCasas() {
        return cantidadCasas;
    }


    /**
     * Indica si la propiedad tiene hotel.
     */
    public boolean tieneHotel() {
        return hotel;
    }


    // =========================================================
    // HIPOTECA
    // =========================================================

    /**
     * Indica si la propiedad está hipotecada.
     */
    public boolean estaHipotecada() {
        return hipotecada;
    }


    /**
     * Devuelve el valor de la hipoteca.
     */
    public int getValorHipoteca() {
        return valorHipoteca;
    }


    // =========================================================
    // PROPIETARIO
    // =========================================================

    /**
     * Devuelve el jugador propietario.
     */
    public Jugador getPropietario() {
        return propietario;
    }


    /**
     * Cambia el propietario de la propiedad.
     */
    public void setPropietario(Jugador propietario) {
        this.propietario = propietario;
    }


    /**
     * Indica si la propiedad tiene propietario.
     */
    public boolean tienePropietario() {

        return propietario != null;
    }


    // =========================================================
    // ALQUILER
    // =========================================================

    /**
     * Calcula el alquiler que debe pagar
     * un jugador al caer en esta propiedad.
     *
     * Sin casas:
     * alquilerSinCasas
     *
     * 1 casa:
     * alquilerUnaCasa
     *
     * 2 casas:
     * alquilerDosCasas
     *
     * 3 casas:
     * alquilerTresCasas
     *
     * 4 casas:
     * alquilerCuatroCasas
     *
     * Hotel:
     * alquilerHotel
     */
    public int calcularAlquiler() {

        // Una propiedad hipotecada no cobra alquiler.
        if (hipotecada) {
            return 0;
        }

        // El hotel tiene prioridad sobre las casas.
        if (hotel) {
            return alquilerHotel;
        }

        if (cantidadCasas == 0) {
            return alquilerSinCasas;
        }

        if (cantidadCasas == 1) {
            return alquilerUnaCasa;
        }

        if (cantidadCasas == 2) {
            return alquilerDosCasas;
        }

        if (cantidadCasas == 3) {
            return alquilerTresCasas;
        }

        if (cantidadCasas == 4) {
            return alquilerCuatroCasas;
        }

        return 0;
    }


    // =========================================================
    // CASAS
    // =========================================================

    /**
     * Agrega una casa a la propiedad.
     *
     * Reglas:
     *
     * - No puede estar hipotecada.
     * - No puede tener hotel.
     * - Máximo 4 casas.
     */
    public boolean agregarCasa() {

        if (hipotecada) {
            return false;
        }

        if (hotel) {
            return false;
        }

        if (cantidadCasas >= 4) {
            return false;
        }

        cantidadCasas++;

        return true;
    }


    /**
     * Quita una casa de la propiedad.
     */
    public boolean quitarCasa() {

        if (cantidadCasas <= 0) {
            return false;
        }

        cantidadCasas--;

        return true;
    }


    // =========================================================
    // HOTEL
    // =========================================================

    /**
     * Construye un hotel.
     *
     * Para construir un hotel se necesitan
     * exactamente 4 casas.
     *
     * Al construir el hotel las 4 casas
     * dejan de estar en la propiedad.
     */
    public boolean construirHotel() {

        if (hipotecada) {
            return false;
        }

        if (hotel) {
            return false;
        }

        if (cantidadCasas != 4) {
            return false;
        }

        cantidadCasas = 0;

        hotel = true;

        return true;
    }


    /**
     * Quita el hotel de la propiedad.
     */
    public boolean quitarHotel() {

        if (!hotel) {
            return false;
        }

        hotel = false;

        return true;
    }


    // =========================================================
    // HIPOTECA
    // =========================================================

    /**
     * Hipoteca la propiedad.
     *
     * Una propiedad no puede hipotecarse si:
     *
     * - Ya está hipotecada.
     * - Tiene casas.
     * - Tiene hotel.
     */
    public boolean hipotecar() {

        if (hipotecada) {
            return false;
        }

        if (cantidadCasas > 0) {
            return false;
        }

        if (hotel) {
            return false;
        }

        hipotecada = true;

        return true;
    }


    /**
     * Quita la hipoteca de la propiedad.
     */
    public boolean quitarHipoteca() {

        if (!hipotecada) {
            return false;
        }

        hipotecada = false;

        return true;
    }
}