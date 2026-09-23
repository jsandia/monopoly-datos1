import modelo.Jugador;
import modelo.Propiedad;

/**
 * Clase Banco
 *
 * Representa el banco del juego Monopoly CR.
 *
 * El banco administra:
 *
 * - Dinero
 * - Casas
 * - Hoteles
 * - Compra de propiedades
 * - Alquileres
 * - Impuestos
 * - Pago por pasar por la salida
 * - Compra y venta de casas
 * - Compra y venta de hoteles
 * - Hipotecas
 * - Cancelación de hipotecas
 */
 class Banco {

    // =========================================================
    // ATRIBUTOS
    // =========================================================

    private int saldo;

    // Cantidad de casas que tiene disponibles el banco.
    private int casasDisponibles;

    // Cantidad de hoteles que tiene disponibles el banco.
    private int hotelesDisponibles;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Crea un banco con los valores iniciales del juego.
     *
     * Dinero: ₡1.000.000
     * Casas: 32
     * Hoteles: 12
     */
    public Banco() {

        this.saldo = 1_000_000;
        this.casasDisponibles = 32;
        this.hotelesDisponibles = 12;
    }


    /**
     * Constructor para crear un banco
     * con cantidades personalizadas.
     */
    public Banco(
            int saldoInicial,
            int casasIniciales,
            int hotelesIniciales
    ) {

        if (saldoInicial >= 0) {
            this.saldo = saldoInicial;
        } else {
            this.saldo = 0;
        }

        if (casasIniciales >= 0) {
            this.casasDisponibles = casasIniciales;
        } else {
            this.casasDisponibles = 0;
        }

        if (hotelesIniciales >= 0) {
            this.hotelesDisponibles = hotelesIniciales;
        } else {
            this.hotelesDisponibles = 0;
        }
    }


    // =========================================================
    // GETTERS
    // =========================================================

    /**
     * Devuelve el dinero disponible del banco.
     */
    public int getSaldo() {
        return saldo;
    }


    /**
     * Devuelve la cantidad de casas disponibles.
     */
    public int getCasasDisponibles() {
        return casasDisponibles;
    }


    /**
     * Devuelve la cantidad de hoteles disponibles.
     */
    public int getHotelesDisponibles() {
        return hotelesDisponibles;
    }


    // =========================================================
    // DINERO DEL BANCO
    // =========================================================

    /**
     * Hace que el banco reciba dinero.
     */
    public void recibirDinero(int monto) {

        if (monto > 0) {
            saldo += monto;
        }
    }


    /**
     * Hace que el banco entregue dinero.
     *
     * Retorna true si pudo entregar el dinero.
     * Retorna false si el monto es inválido
     * o el banco no tiene suficiente.
     */
    public boolean entregarDinero(int monto) {

        if (monto <= 0) {
            return false;
        }

        if (monto > saldo) {
            return false;
        }

        saldo -= monto;

        return true;
    }


    // =========================================================
    // PAGAR A JUGADOR
    // =========================================================

    /**
     * El banco paga dinero a un jugador.
     */
    public boolean pagarJugador(
            Jugador jugador,
            int monto
    ) {

        if (jugador == null) {
            return false;
        }

        if (monto <= 0) {
            return false;
        }

        if (!entregarDinero(monto)) {
            return false;
        }

        jugador.acreditar(monto);

        return true;
    }


    // =========================================================
    // COBRAR A JUGADOR
    // =========================================================

    /**
     * El banco cobra dinero a un jugador.
     */
    public boolean cobrarJugador(
            Jugador jugador,
            int monto
    ) {

        if (jugador == null) {
            return false;
        }

        if (monto <= 0) {
            return false;
        }

        if (!jugador.debitar(monto)) {
            return false;
        }

        recibirDinero(monto);

        return true;
    }


    // =========================================================
    // SALIDA
    // =========================================================

    /**
     * Paga al jugador cuando pasa por la casilla de salida.
     */
    public boolean pagarSalida(
            Jugador jugador,
            int monto
    ) {

        return pagarJugador(jugador, monto);
    }


    // =========================================================
    // IMPUESTOS
    // =========================================================

    /**
     * Cobra un impuesto al jugador.
     */
    public boolean cobrarImpuesto(
            Jugador jugador,
            int impuesto
    ) {

        return cobrarJugador(jugador, impuesto);
    }


    // =========================================================
    // COMPRA DE PROPIEDAD
    // =========================================================

    /**
     * Permite que un jugador compre una propiedad.
     *
     * La propiedad debe estar libre.
     */
    public boolean comprarPropiedad(
            Jugador jugador,
            Propiedad propiedad
    ) {

        if (jugador == null) {
            return false;
        }

        if (propiedad == null) {
            return false;
        }

        // La propiedad ya tiene dueño.
        if (propiedad.tienePropietario()) {
            return false;
        }

        int precio = propiedad.getPrecioCompra();

        if (precio <= 0) {
            return false;
        }

        // El jugador debe tener suficiente dinero.
        if (!jugador.debitar(precio)) {
            return false;
        }

        // El banco recibe el dinero.
        recibirDinero(precio);

        // Se asigna el dueño.
        propiedad.setPropietario(jugador);

        // Se agrega la propiedad al jugador.
        jugador.agregarPropiedad(propiedad);

        return true;
    }


    // =========================================================
    // COBRAR ALQUILER
    // =========================================================

    /**
     * Cobra el alquiler cuando un jugador
     * cae en la propiedad de otro jugador.
     */
    public boolean cobrarAlquiler(
            Jugador jugador,
            Propiedad propiedad
    ) {

        if (jugador == null) {
            return false;
        }

        if (propiedad == null) {
            return false;
        }

        Jugador propietario =
                propiedad.getPropietario();

        // La propiedad no tiene dueño.
        if (propietario == null) {
            return false;
        }

        // El jugador cayó en su propia propiedad.
        if (propietario == jugador) {
            return false;
        }

        // Una propiedad hipotecada no cobra alquiler.
        if (propiedad.estaHipotecada()) {
            return false;
        }

        int alquiler =
                propiedad.calcularAlquiler();

        if (alquiler <= 0) {
            return false;
        }

        // El jugador debe poder pagar.
        if (!jugador.debitar(alquiler)) {
            return false;
        }

        // El dueño recibe el alquiler.
        propietario.acreditar(alquiler);

        return true;
    }


    // =========================================================
    // COMPRAR CASA
    // =========================================================

    /**
     * Permite que un jugador compre una casa.
     */
    public boolean comprarCasa(
            Jugador jugador,
            Propiedad propiedad
    ) {

        if (jugador == null) {
            return false;
        }

        if (propiedad == null) {
            return false;
        }

        // El jugador debe ser el dueño.
        if (propiedad.getPropietario() != jugador) {
            return false;
        }

        // No se puede construir sobre una propiedad hipotecada.
        if (propiedad.estaHipotecada()) {
            return false;
        }

        // No se pueden construir casas si ya existe hotel.
        if (propiedad.tieneHotel()) {
            return false;
        }

        // Máximo 4 casas.
        if (propiedad.getCantidadCasas() >= 4) {
            return false;
        }

        // El banco debe tener casas disponibles.
        if (casasDisponibles <= 0) {
            return false;
        }

        int precio =
                propiedad.getPrecioCasa();

        if (precio <= 0) {
            return false;
        }

        // Primero comprobamos si puede pagar.
        if (jugador.getSaldo() < precio) {
            return false;
        }

        // Quitamos el dinero.
        if (!jugador.debitar(precio)) {
            return false;
        }

        // Intentamos construir la casa.
        if (!propiedad.agregarCasa()) {

            // Si falla, devolvemos el dinero.
            jugador.acreditar(precio);

            return false;
        }

        // El banco recibe el dinero.
        recibirDinero(precio);

        // El banco entrega una casa.
        casasDisponibles--;

        return true;
    }


    // =========================================================
    // VENDER CASA
    // =========================================================

    /**
     * El jugador vende una casa al banco.
     *
     * Recibe la mitad del precio de compra.
     */
    public boolean venderCasa(
            Jugador jugador,
            Propiedad propiedad
    ) {

        if (jugador == null) {
            return false;
        }

        if (propiedad == null) {
            return false;
        }

        // Debe ser dueño.
        if (propiedad.getPropietario() != jugador) {
            return false;
        }

        // Debe tener al menos una casa.
        if (propiedad.getCantidadCasas() <= 0) {
            return false;
        }

        int precioVenta =
                propiedad.getPrecioCasa() / 2;

        if (precioVenta <= 0) {
            return false;
        }

        // El banco debe tener dinero para pagar.
        if (!entregarDinero(precioVenta)) {
            return false;
        }

        // Quitamos la casa.
        if (!propiedad.quitarCasa()) {

            // Si falla, devolvemos el dinero al banco.
            recibirDinero(precioVenta);

            return false;
        }

        // El jugador recibe el dinero.
        jugador.acreditar(precioVenta);

        // La casa vuelve al banco.
        casasDisponibles++;

        return true;
    }


    // =========================================================
    // COMPRAR HOTEL
    // =========================================================

    /**
     * Permite que un jugador compre un hotel.
     *
     * Para construirlo debe tener exactamente
     * cuatro casas.
     */
    public boolean comprarHotel(
            Jugador jugador,
            Propiedad propiedad
    ) {

        if (jugador == null) {
            return false;
        }

        if (propiedad == null) {
            return false;
        }

        // Debe ser dueño.
        if (propiedad.getPropietario() != jugador) {
            return false;
        }

        // No se puede construir sobre una propiedad hipotecada.
        if (propiedad.estaHipotecada()) {
            return false;
        }

        // Ya tiene hotel.
        if (propiedad.tieneHotel()) {
            return false;
        }

        // Necesita exactamente 4 casas.
        if (propiedad.getCantidadCasas() != 4) {
            return false;
        }

        // Debe haber hoteles disponibles.
        if (hotelesDisponibles <= 0) {
            return false;
        }

        int precio =
                propiedad.getPrecioHotel();

        if (precio <= 0) {
            return false;
        }

        // El jugador debe poder pagar.
        if (!jugador.debitar(precio)) {
            return false;
        }

        // Construimos el hotel.
        if (!propiedad.construirHotel()) {

            // Si falla, devolvemos el dinero.
            jugador.acreditar(precio);

            return false;
        }

        // El banco recibe el dinero.
        recibirDinero(precio);

        /*
         * El banco entrega un hotel.
         */
        hotelesDisponibles--;

        /*
         * Las cuatro casas que tenía la propiedad
         * regresan al banco.
         */
        casasDisponibles += 4;

        return true;
    }


    // =========================================================
    // VENDER HOTEL
    // =========================================================

    /**
     * El jugador vende un hotel al banco.
     *
     * Recibe la mitad del precio del hotel.
     */
    public boolean venderHotel(
            Jugador jugador,
            Propiedad propiedad
    ) {

        if (jugador == null) {
            return false;
        }

        if (propiedad == null) {
            return false;
        }

        // Debe ser dueño.
        if (propiedad.getPropietario() != jugador) {
            return false;
        }

        // Debe tener hotel.
        if (!propiedad.tieneHotel()) {
            return false;
        }

        int precioVenta =
                propiedad.getPrecioHotel() / 2;

        if (precioVenta <= 0) {
            return false;
        }

        // El banco debe poder pagar.
        if (!entregarDinero(precioVenta)) {
            return false;
        }

        // Quitamos el hotel.
        if (!propiedad.quitarHotel()) {

            // Devolvemos el dinero al banco.
            recibirDinero(precioVenta);

            return false;
        }

        // El jugador recibe el dinero.
        jugador.acreditar(precioVenta);

        // El hotel vuelve al banco.
        hotelesDisponibles++;

        return true;
    }


    // =========================================================
    // HIPOTECAR PROPIEDAD
    // =========================================================

    /**
     * Hipoteca una propiedad.
     *
     * El banco paga al jugador el valor
     * establecido para la hipoteca.
     */
    public boolean hipotecarPropiedad(
            Jugador jugador,
            Propiedad propiedad
    ) {

        if (jugador == null) {
            return false;
        }

        if (propiedad == null) {
            return false;
        }

        // Debe ser dueño.
        if (propiedad.getPropietario() != jugador) {
            return false;
        }

        // Ya está hipotecada.
        if (propiedad.estaHipotecada()) {
            return false;
        }

        // No se puede hipotecar con casas.
        if (propiedad.getCantidadCasas() > 0) {
            return false;
        }

        // No se puede hipotecar con hotel.
        if (propiedad.tieneHotel()) {
            return false;
        }

        int valorHipoteca =
                propiedad.getValorHipoteca();

        if (valorHipoteca <= 0) {
            return false;
        }

        // El banco debe tener suficiente dinero.
        if (!entregarDinero(valorHipoteca)) {
            return false;
        }

        // Marcamos la propiedad como hipotecada.
        if (!propiedad.hipotecar()) {

            // Devolvemos el dinero al banco.
            recibirDinero(valorHipoteca);

            return false;
        }

        // El jugador recibe el dinero.
        jugador.acreditar(valorHipoteca);

        return true;
    }


    // =========================================================
    // QUITAR HIPOTECA
    // =========================================================

    /**
     * Quita la hipoteca de una propiedad.
     *
     * El jugador paga:
     *
     * valor de hipoteca + 10% de interés.
     */
    public boolean quitarHipoteca(
            Jugador jugador,
            Propiedad propiedad
    ) {

        if (jugador == null) {
            return false;
        }

        if (propiedad == null) {
            return false;
        }

        // Debe ser dueño.
        if (propiedad.getPropietario() != jugador) {
            return false;
        }

        // Debe estar hipotecada.
        if (!propiedad.estaHipotecada()) {
            return false;
        }

        int valorHipoteca =
                propiedad.getValorHipoteca();

        if (valorHipoteca <= 0) {
            return false;
        }

        // 10% de interés.
        int interes =
                valorHipoteca / 10;

        int montoTotal =
                valorHipoteca + interes;

        // El jugador debe poder pagar.
        if (!jugador.debitar(montoTotal)) {
            return false;
        }

        // Quitamos la hipoteca.
        if (!propiedad.quitarHipoteca()) {

            // Si falla, devolvemos el dinero.
            jugador.acreditar(montoTotal);

            return false;
        }

        // El banco recibe el pago.
        recibirDinero(montoTotal);

        return true;
    }
}