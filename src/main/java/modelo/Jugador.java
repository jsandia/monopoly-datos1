package modelo;

import estructuras.ListaSimple;
import estructuras.NodoSimple;

/**
 * Un participante de la partida: su saldo, su posición, sus propiedades,
 * su tarjeta RFID y su situación en la cárcel.
 *
 * La posición es solo el id de la casilla donde está la ficha. Quien la
 * mueve es Juego, caminando por los nodos del Tablero.
 */
public class Jugador {

    private final String id;
    private final String nombre;
    private int saldo;
    private int posicionActual;
    private boolean activo;          // false = quedó en bancarrota
    private String uidTarjeta;       // tarjeta RFID que lo identifica (null = sin tarjeta)

    // Sus propiedades, en el orden en que las compró. ListaSimple porque
    // solo se agregan y se recorren.
    private final ListaSimple<Propiedad> propiedades = new ListaSimple<>();

    // Cárcel: si está preso y cuántas veces ha tirado sin sacar dobles
    private boolean enCarcel;
    private int turnosEnCarcel;

    // Cartas "Salga de la cárcel" guardadas. Máximo 2: hay una en cada mazo.
    // Se guarda la carta misma (no solo un contador) para devolverla a su mazo al usarla.
    private final CartaEvento[] cartasSalirCarcel = new CartaEvento[2];

    // Turnos que le faltan por perder (carta "pierda su próximo turno")
    private int turnosPorPerder;

    public Jugador(String id, String nombre, int saldoInicial) {
        this.id = id;
        this.nombre = nombre;
        this.saldo = saldoInicial;
        this.posicionActual = 0;
        this.activo = true;
    }

    // =========================================================
    // DINERO
    // =========================================================

    /** Suma dinero al saldo. Los montos que no son positivos se ignoran. */
    public void acreditar(int monto) {
        if (monto > 0) {
            saldo += monto;
        }
    }

    /**
     * Resta dinero si le alcanza.
     *
     * @return false si el monto no es positivo o si no le alcanza el saldo
     */
    public boolean debitar(int monto) {
        if (monto <= 0 || monto > saldo) {
            return false;
        }
        saldo -= monto;
        return true;
    }

    /** Patrimonio = saldo + precio de compra de todas sus propiedades. */
    public int calcularPatrimonio() {
        return saldo + calcularValorPropiedades();
    }

    // =========================================================
    // TARJETA RFID
    // =========================================================

    /** Asocia la tarjeta al jugador. El UID se guarda en mayúsculas. */
    public void registrarTarjeta(String uid) {
        if (uid != null && !uid.isBlank()) {
            uidTarjeta = uid.trim().toUpperCase();
        }
    }

    public String getUidTarjeta() {
        return uidTarjeta;
    }

    public boolean tieneTarjetaRegistrada() {
        return uidTarjeta != null;
    }

    /** @return true si esa tarjeta es la de este jugador (sin importar mayúsculas) */
    public boolean validarTarjeta(String uid) {
        return uidTarjeta != null && uid != null && uidTarjeta.equalsIgnoreCase(uid.trim());
    }

    // =========================================================
    // PROPIEDADES (ListaSimple<Propiedad>)
    // =========================================================

    /** Agrega una propiedad comprada al final de su lista, sin repetirla. */
    public void agregarPropiedad(Propiedad propiedad) {
        if (propiedad != null && !propiedades.contiene(propiedad)) {
            propiedades.agregarAlFinal(propiedad);
        }
    }

    public int getCantidadPropiedades() {
        return propiedades.tamaño();
    }

    /**
     * Entrega todas sus propiedades (por bancarrota) y deja su lista vacía.
     * Primero se copian a un arreglo recorriendo los nodos, y después se
     * vacía la lista: así quien las recibe no depende de los nodos borrados.
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

    /** Suma el precio de compra de todas sus propiedades. */
    public int calcularValorPropiedades() {
        int total = 0;
        NodoSimple<Propiedad> actual = propiedades.getPrimero();
        while (actual != null) {
            total += actual.getDato().getPrecioCompra();
            actual = actual.getSiguiente();
        }
        return total;
    }

    /** Total de casas en todas sus propiedades (para las cartas de reparaciones). */
    public int contarCasas() {
        int total = 0;
        NodoSimple<Propiedad> actual = propiedades.getPrimero();
        while (actual != null) {
            total += actual.getDato().getCantidadCasas();
            actual = actual.getSiguiente();
        }
        return total;
    }

    /** Cuántas de sus propiedades tienen hotel (para las cartas de reparaciones). */
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

    /**
     * Le cuenta a cada una de sus propiedades cuántas casillas caminó, para
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
    // CÁRCEL
    // =========================================================

    /** Lo pone preso en la casilla de la cárcel. No camina, así que no cobra Salida. */
    public void enviarACarcel(int posicionCarcel) {
        posicionActual = posicionCarcel;
        enCarcel = true;
        turnosEnCarcel = 0;
    }

    /** Sale de la cárcel y se queda en la misma casilla ("de visita"). */
    public void liberarDeCarcel() {
        enCarcel = false;
        turnosEnCarcel = 0;
    }

    /** Suma un intento fallido de sacar dobles. */
    public void sumarIntentoCarcel() {
        turnosEnCarcel++;
    }

    public boolean isEnCarcel() {
        return enCarcel;
    }

    public int getTurnosEnCarcel() {
        return turnosEnCarcel;
    }

    /** Guarda una carta "Salga de la cárcel". @return false si ya tiene 2 */
    public boolean guardarCartaSalirCarcel(CartaEvento carta) {
        for (int i = 0; i < cartasSalirCarcel.length; i++) {
            if (cartasSalirCarcel[i] == null) {
                cartasSalirCarcel[i] = carta;
                return true;
            }
        }
        return false;
    }

    /** Entrega una de sus cartas para usarla (y devolverla al mazo). @return null si no tiene */
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

    /** La carta "pierda su próximo turno" suma uno. */
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
    // DATOS BÁSICOS
    // =========================================================

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
