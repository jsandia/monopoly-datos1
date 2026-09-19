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

    // Agregar un metodo de calcularPropiedades/Patrimonio.
}