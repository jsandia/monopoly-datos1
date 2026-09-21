import java.util.ArrayList;

/**
 * Clase Jugador
 * Representa un jugador como participante del Juego.
 * Contiene la informacion de cada Jugador.
 */
public class Jugador {

    // --- Atributos del Jugador ---

    private String id;
    private String nombre;
    private int saldo;
    private int posicionActual;
    private boolean activo;

    // Lista de propiedades que posee el jugador
    private ArrayList<Propiedad> propiedades;

    //Agregar atributo de La lista de propiedades cuando este listo.

    /**
     * Constructor para crear los jugadores.
     * Resive los atributos de cada jugador
     */
    public Jugador(String id, String nombre, int saldoInicial) {
        this.id = id;
        this.nombre = nombre;
        this.saldo = saldoInicial;
        this.posicionActual = 0;
        this.activo = true;

        // Inicializar la lista de propiedades
        this.propiedades = new ArrayList<>();
    }

    // --- Métodos de gestión de dinero ---

    /**
     * Suma el dinero ganado por cada jugador a su saldo actual.
     */
    public void acreditar(int monto) {
        if (monto > 0) {
            this.saldo += monto;
        }
    }

    /**
     * Resta el dinero gastado por cada jugador de su saldo.
     * Retorna true si se pudo restar el monto del saldo actual del jugador.
     * Retorna false si los fondos del jugador no son suficientes.
     */
    public boolean debitar(int monto) {
        if (monto <= this.saldo) {
            this.saldo -= monto;
            return true;
        }

        return false;
    }

    // --- Métodos de movimiento ---

    /**
     * Mueve al jugador por el tablero según el resultado de los dados.
     */
    public void mover(
            int casillasAvanzar,
            int totalCasillasTablero
    ) {

        this.posicionActual =
                (this.posicionActual + casillasAvanzar)
                        % totalCasillasTablero;
    }

    // --- Métodos de gestión de propiedades ---

    /**
     * Agrega una propiedad a la lista de propiedades del jugador.
     */
    public void agregarPropiedad(Propiedad propiedad) {

        if (propiedad != null && !propiedades.contains(propiedad)) {
            propiedades.add(propiedad);
        }
    }

    /**
     * Elimina una propiedad de la lista del jugador.
     */
    public void quitarPropiedad(Propiedad propiedad) {

        if (propiedad != null) {
            propiedades.remove(propiedad);
        }
    }

    /**
     * Devuelve la lista de propiedades del jugador.
     */
    public ArrayList<Propiedad> getPropiedades() {

        return propiedades;
    }

    /**
     * Calcula el valor total de compra
     * de todas las propiedades del jugador.
     */
    public int calcularValorPropiedades() {

        int total = 0;

        for (Propiedad propiedad : propiedades) {
            total += propiedad.getPrecioCompra();
        }

        return total;
    }

    /**
     * Calcula el patrimonio total del jugador.
     *
     * Patrimonio = dinero + valor de las propiedades.
     */
    public int calcularPatrimonio() {

        return saldo + calcularValorPropiedades();
    }

    // --- Getters y Setters Básicos ---

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getSaldo() {
        return saldo;
    }

    public void setSaldo(int saldo) {
        this.saldo = saldo;
    }

    public int getPosicionActual() {
        return posicionActual;
    }

    public void setPosicionActual(int posicionActual) {
        this.posicionActual = posicionActual;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // --- Métodos adicionales de propiedades ---

    /**
     * Devuelve la cantidad de propiedades que posee el jugador.
     */
    public int getCantidadPropiedades() {

        return propiedades.size();
    }
}