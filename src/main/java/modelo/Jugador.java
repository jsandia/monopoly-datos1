package modelo;

import estructuras.ListaSimple;
import estructuras.NodoSimple;

/**
 * Clase Jugador
 * Representa a cada participante dentro del juego.
 * Mantiene la informacion de saldo, posicion y sus propiedades.
 *
 * La posicion es solo el id de la casilla donde esta la ficha. Quien la
 * mueve es Juego, caminando por los nodos del Tablero.
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

    // Sus propiedades, en el orden en que las compro. ListaSimple porque
    // solo se agrega, se recorre y, a lo sumo, se quita una.
    private final ListaSimple<Propiedad> propiedades;

    // Carcel: si esta preso y cuantas veces ha tirado sin sacar dobles
    private boolean enCarcel;
    private int turnosEnCarcel;

    // Cartas "Salga de la carcel" que se guardo. Maximo 2: hay una en cada mazo.
    // Se guarda la carta misma (no solo un contador) para devolverla a su mazo al usarla.
    private final CartaEvento[] cartasSalirCarcel = new CartaEvento[2];

    // Turnos que le faltan por perder (carta "pierda su proximo turno")
    private int turnosPorPerder;

    // Constructor principal del jugador
    public Jugador(String id, String nombre, int saldoInicial) {
        this.id = id;
        this.nombre = nombre;
        this.saldo = saldoInicial;
        this.posicionActual = 0;
        this.activo = true;
        this.uidTarjeta = null;
        this.propiedades = new ListaSimple<>();
    }

    // =========================================================
    // TARJETA RFID
    // =========================================================

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
        if (monto <= this.saldo) {
            this.saldo -= monto;
            return true;
        }
        return false;
    }

    /**
     * Le cuenta a cada una de sus propiedades cuantas casillas camino, para
     * la regla de "una vuelta desde la compra antes de construir". Juego lo
     * llama al avanzar (pasos positivos) y al retroceder (negativos).
     */
    public void registrarRecorrido(int pasos) {
        NodoSimple<Propiedad> actual = propiedades.getPrimero();
        while (actual != null) {
            Propiedad propiedad = actual.getDato();
            // Solo cuenta si sigue siendo suya: el recorrido de otro dueño no vale
            if (propiedad.getPropietario() == this) {
                propiedad.registrarMovimiento(pasos);
            }
            actual = actual.getSiguiente();
        }
    }

    // =========================================================
    // CARCEL
    // =========================================================

    // Lo pone preso en la casilla de la carcel. No usa mover(), asi que no cobra Salida.
    public void enviarACarcel(int posicionCarcel) {
        this.posicionActual = posicionCarcel;
        this.enCarcel = true;
        this.turnosEnCarcel = 0;
    }

    // Sale de la carcel; se queda en la misma casilla ("de visita")
    public void liberarDeCarcel() {
        this.enCarcel = false;
        this.turnosEnCarcel = 0;
    }

    // Suma un intento fallido de sacar dobles
    public void sumarIntentoCarcel() {
        this.turnosEnCarcel++;
    }

    public boolean isEnCarcel() {
        return enCarcel;
    }

    public int getTurnosEnCarcel() {
        return turnosEnCarcel;
    }

    // Guarda una carta "Salga de la carcel". Devuelve false si ya tiene 2.
    public boolean guardarCartaSalirCarcel(CartaEvento carta) {
        for (int i = 0; i < cartasSalirCarcel.length; i++) {
            if (cartasSalirCarcel[i] == null) {
                cartasSalirCarcel[i] = carta;
                return true;
            }
        }
        return false;
    }

    // Entrega una de sus cartas para usarla (y devolverla al mazo). null si no tiene.
    public CartaEvento usarCartaSalirCarcel() {
        for (int i = 0; i < cartasSalirCarcel.length; i++) {
            if (cartasSalirCarcel[i] != null) {
                CartaEvento carta = cartasSalirCarcel[i];
                cartasSalirCarcel[i] = null;
                return carta;
            }
        }
        return null;
    }

    public int getCantidadCartasSalirCarcel() {
        int total = 0;
        for (CartaEvento carta : cartasSalirCarcel) {
            if (carta != null) {
                total++;
            }
        }
        return total;
    }

    // =========================================================
    // TURNOS PERDIDOS
    // =========================================================

    // La carta "pierda su proximo turno" suma uno
    public void perderProximoTurno() {
        turnosPorPerder++;
    }

    /**
     * Si tiene un turno por perder, lo gasta y devuelve true (el turno se
     * salta). Si no, devuelve false y juega normal.
     */
    public boolean consumirTurnoPerdido() {
        if (turnosPorPerder > 0) {
            turnosPorPerder--;
            return true;
        }
        return false;
    }

    public int getTurnosPorPerder() {
        return turnosPorPerder;
    }

    // =========================================================
    // PROPIEDADES (ListaSimple<Propiedad>)
    // =========================================================

    // Agrega una propiedad comprada al final de su lista, sin repetirla
    public void agregarPropiedad(Propiedad propiedad) {
        if (propiedad != null && !propiedades.contiene(propiedad)) {
            propiedades.agregarAlFinal(propiedad);
        }
    }

    // Quita una propiedad de su lista
    public void quitarPropiedad(Propiedad propiedad) {
        propiedades.eliminar(propiedad);
    }

    /**
     * Entrega todas sus propiedades (por bancarrota) y deja su lista vacia.
     * Primero se copian a un arreglo recorriendo los nodos, y despues se
     * vacia la lista: asi quien las recibe no depende de los nodos borrados.
     */
    public Propiedad[] entregarTodasLasPropiedades() {
        Propiedad[] entregadas = new Propiedad[propiedades.tamaño()];
        NodoSimple<Propiedad> actual = propiedades.getPrimero();
        for (int i = 0; actual != null; i++) {
            entregadas[i] = actual.getDato();
            actual = actual.getSiguiente();
        }
        propiedades.vaciar();
        return entregadas;
    }

    // Suma el precio de compra de todas sus propiedades
    public int calcularValorPropiedades() {
        int total = 0;
        NodoSimple<Propiedad> actual = propiedades.getPrimero();
        while (actual != null) {
            total += actual.getDato().getPrecioCompra();
            actual = actual.getSiguiente();
        }
        return total;
    }

    // Total de casas en todas sus propiedades (para las cartas de reparaciones)
    public int contarCasas() {
        int total = 0;
        NodoSimple<Propiedad> actual = propiedades.getPrimero();
        while (actual != null) {
            total += actual.getDato().getCantidadCasas();
            actual = actual.getSiguiente();
        }
        return total;
    }

    // Cuantas de sus propiedades tienen hotel (para las cartas de reparaciones)
    public int contarHoteles() {
        int total = 0;
        NodoSimple<Propiedad> actual = propiedades.getPrimero();
        while (actual != null) {
            if (actual.getDato().tieneHotel()) {
                total++;
            }
            actual = actual.getSiguiente();
        }
        return total;
    }

    // Suma el dinero disponible mas el valor de las propiedades
    public int calcularPatrimonio() {
        return saldo + calcularValorPropiedades();
    }

    // Devuelve cuantas propiedades posee
    public int getCantidadPropiedades() {
        return propiedades.tamaño();
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
