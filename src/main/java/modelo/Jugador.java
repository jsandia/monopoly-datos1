package modelo;

/**
 * Clase Jugador
 * Representa a cada participante dentro del juego.
 * Mantiene la informacion de saldo, posicion y sus propiedades.
 */
public class Jugador {

    // Atributos basicos del jugador
    private String id;
    private String nombre;
    private int saldo;
    private int posicionActual;
    private boolean activo;

    // Tarjeta RFID asociada a este jugador.
    private String uidTarjeta;

    // Lista enlazada propia para almacenar sus propiedades
    private ListaPropiedades propiedades;

    // Constructor principal del jugador
    public Jugador(String id, String nombre, int saldoInicial) {
        this.id = id;
        this.nombre = nombre;
        this.saldo = saldoInicial;
        this.posicionActual = 0;
        this.activo = true;
        this.uidTarjeta = null;
        this.propiedades = new ListaPropiedades();
    }

    // Asocia una tarjeta RFID al jugador.
    public void registrarTarjeta(String uid) {
        if (uid != null && !uid.trim().isEmpty()) {
            this.uidTarjeta = uid.trim().toUpperCase();
        }
    }

    public String getUidTarjeta() {
        return uidTarjeta;
    }

    public boolean tieneTarjetaRegistrada() {
        return uidTarjeta != null && !uidTarjeta.isEmpty();
    }

    public boolean validarTarjeta(String uid) {
        return uidTarjeta != null
                && uid != null
                && uidTarjeta.equalsIgnoreCase(uid.trim());
    }

    // Suma un monto al saldo del jugador
    public void acreditar(int monto) {
        if (monto > 0) {
            this.saldo += monto;
        }
    }

    // Resta dinero si cuenta con suficiente saldo
    public boolean debitar(int monto) {
        if (monto > 0 && monto <= this.saldo) {
            this.saldo -= monto;
            return true;
        }
        return false;
    }

    // Actualiza la posicion dentro del tablero
    public void mover(int casillasAvanzar, int totalCasillasTablero) {
        this.posicionActual = (this.posicionActual + casillasAvanzar) % totalCasillasTablero;
    }

    // Agrega una propiedad comprada a su lista propia
    public void agregarPropiedad(Propiedad propiedad) {
        if (propiedad != null) {
            this.propiedades.insertar(propiedad);
        }
    }

    // Remueve una propiedad de su lista
    public void quitarPropiedad(Propiedad propiedad) {
        if (propiedad != null) {
            this.propiedades.eliminar(propiedad);
        }
    }

    // Retorna la lista enlazada de propiedades
    public ListaPropiedades getPropiedades() {
        return propiedades;
    }

    // Calcula el costo total de sus propiedades
    public int calcularValorPropiedades() {
        return this.propiedades.obtenerValorTotal();
    }

    // Suma el dinero disponible mas el valor de las propiedades
    public int calcularPatrimonio() {
        return saldo + calcularValorPropiedades();
    }

    // Devuelve cuantas propiedades posee
    public int getCantidadPropiedades() {
        return propiedades.getCantidad();
    }

    // Getters y Setters
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
}