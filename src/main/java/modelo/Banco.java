package modelo;
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
public class Banco {

    // =========================================================
    // ATRIBUTOS
    // =========================================================

    private int saldo;
    private RegistroTransacciones registro;

    public synchronized void setRegistroTransacciones(RegistroTransacciones registro) {
        this.registro = registro;
    }

    private void registrar(Jugador jugador, TipoTransaccion tipo, String detalle, int anterior) {
        if (registro == null) return;
        int diferencia = jugador.getSaldo() - anterior;
        // El movimiento del banco es el inverso del movimiento del jugador.
        int bancoAnterior = saldo + diferencia;
        if (diferencia > 0) {
            registro.registrar(tipo, null, jugador, diferencia, detalle,
                    bancoAnterior, saldo, anterior, jugador.getSaldo());
        } else if (diferencia < 0) {
            registro.registrar(tipo, jugador, null, -(long) diferencia, detalle,
                    anterior, jugador.getSaldo(), bancoAnterior, saldo);
        }
    }


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
    public synchronized void recibirDinero(int monto) {

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
    public synchronized boolean entregarDinero(int monto) {

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
    public synchronized boolean pagarJugador(Jugador jugador, int monto) {
        return pagarJugador(jugador, monto, TipoTransaccion.PAGO_DEL_BANCO, "Banco");
    }

    private boolean pagarJugador(Jugador jugador, int monto, TipoTransaccion tipo, String descripcion) {
        int saldoAnterior = jugador == null ? 0 : jugador.getSaldo();


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

        registrar(jugador, tipo, descripcion, saldoAnterior);

        return true;
    }


    // =========================================================
    // COBRAR A JUGADOR
    // =========================================================

    /**
     * El banco cobra dinero a un jugador.
     */
    public synchronized boolean cobrarJugador(Jugador jugador, int monto) {
        return cobrarJugador(jugador, monto, TipoTransaccion.PAGO_AL_BANCO, "Banco");
    }

    private boolean cobrarJugador(Jugador jugador, int monto, TipoTransaccion tipo, String descripcion) {
        int saldoAnterior = jugador == null ? 0 : jugador.getSaldo();


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

        registrar(jugador, tipo, descripcion, saldoAnterior);

        return true;
    }


    // =========================================================
    // SALIDA
    // =========================================================

    /**
     * Paga al jugador cuando pasa por la casilla de salida.
     */
    public synchronized boolean pagarSalida(
            Jugador jugador,
            int monto
    ) {

        if (jugador == null || monto <= 0 || !entregarDinero(monto)) return false;
        int anterior = jugador.getSaldo();
        jugador.acreditar(monto);
        registrar(jugador, TipoTransaccion.PREMIO_INICIO, "Premio por pasar por inicio", anterior);
        return true;
    }


    // =========================================================
    // IMPUESTOS
    // =========================================================

    /**
     * Cobra un impuesto al jugador.
     */
    public synchronized boolean cobrarImpuesto(
            Jugador jugador,
            int impuesto
    ) {

        if (jugador == null || impuesto <= 0) return false;
        int anterior = jugador.getSaldo();
        if (!jugador.debitar(impuesto)) return false;
        recibirDinero(impuesto);
        registrar(jugador, TipoTransaccion.PAGO_AL_BANCO, "Pago de impuesto", anterior);
        return true;
    }


    // =========================================================
    // CARCEL
    // =========================================================

    /**
     * Cobra la multa para salir de la cárcel.
     */
    public synchronized boolean cobrarMultaCarcel(
            Jugador jugador,
            int multa
    ) {

        return cobrarJugador(jugador, multa, TipoTransaccion.PAGO_AL_BANCO,
                "Multa para salir de la cárcel");
    }


    // =========================================================
    // COMPRA DE PROPIEDAD
    // =========================================================

    /**
     * Permite que un jugador compre una propiedad.
     *
     * La propiedad debe estar libre.
     */
    public synchronized boolean comprarPropiedad(
            Jugador jugador,
            Propiedad propiedad
    ) {
        int saldoAnterior = jugador == null ? 0 : jugador.getSaldo();


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

        registrar(jugador, TipoTransaccion.COMPRA_PROPIEDAD, propiedad.getNombre(), saldoAnterior);

        return true;
    }


    // =========================================================
    // COBRAR ALQUILER
    // =========================================================

    /**
     * Cobra el alquiler cuando un jugador
     * cae en la propiedad de otro jugador.
     */
    public synchronized boolean cobrarAlquiler(
            Jugador jugador,
            Propiedad propiedad
    ) {
        int saldoAnterior = jugador == null ? 0 : jugador.getSaldo();


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

        if (registro != null) {
            registro.registrar(TipoTransaccion.PAGO_ALQUILER, jugador, propietario, alquiler,
                    "Alquiler de " + propiedad.getNombre(), saldoAnterior, jugador.getSaldo(),
                    propietario.getSaldo() - alquiler, propietario.getSaldo());
        }

        return true;
    }


    // =========================================================
    // COMPRAR CASA
    // =========================================================

    /**
     * Permite que un jugador compre una casa.
     */
    public synchronized boolean comprarCasa(
            Jugador jugador,
            Propiedad propiedad
    ) {
        int saldoAnterior = jugador == null ? 0 : jugador.getSaldo();

        if (motivoNoComprarCasa(jugador, propiedad) != null) return false;

        int precio =
                propiedad.getPrecioCasa();

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

        registrar(jugador, TipoTransaccion.COMPRA_CASA, propiedad.getNombre(), saldoAnterior);

        return true;
    }


    // =========================================================
    // DISPONIBILIDAD DE CASAS
    // =========================================================

    /** Devuelve null si se permite la compra; de lo contrario, explica el motivo. */
    public synchronized String motivoNoComprarCasa(Jugador jugador, Propiedad propiedad) {
        if (propiedad == null) return "Selecciona una propiedad válida.";
        String motivo = propiedad.motivoNoConstruir(jugador);
        if (motivo != null) return motivo;
        if (casasDisponibles <= 0) return "El banco no tiene casas disponibles.";
        if (jugador.getSaldo() < propiedad.getPrecioCasa()) return "Saldo insuficiente para comprar una casa.";
        return null;
    }

    // =========================================================
    // VENDER CASA
    // =========================================================

    /**
     * El jugador vende una casa al banco.
     *
     * Recibe la mitad del precio de compra.
     */
    public synchronized boolean venderCasa(
            Jugador jugador,
            Propiedad propiedad
    ) {
        int saldoAnterior = jugador == null ? 0 : jugador.getSaldo();


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

        registrar(jugador, TipoTransaccion.VENTA_CASA, propiedad.getNombre(), saldoAnterior);

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
    public synchronized boolean comprarHotel(
            Jugador jugador,
            Propiedad propiedad
    ) {
        int saldoAnterior = jugador == null ? 0 : jugador.getSaldo();

        if (motivoNoComprarHotel(jugador, propiedad) != null) return false;

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

        registrar(jugador, TipoTransaccion.COMPRA_HOTEL, propiedad.getNombre(), saldoAnterior);

        return true;
    }


    // =========================================================
    // DISPONIBILIDAD DE HOTELES
    // =========================================================

    /** null si se puede comprar; valida dueño, cuatro casas, saldo y existencias. */
    public synchronized String motivoNoComprarHotel(Jugador jugador, Propiedad propiedad) {
        if (propiedad == null) return "Selecciona una propiedad válida.";
        if (jugador == null || propiedad.getPropietario() != jugador) return "La propiedad no pertenece al jugador en turno.";
        if (!propiedad.esEdificable()) return "No se construyen hoteles en transportes ni servicios.";
        if (propiedad.estaHipotecada()) return "La propiedad está hipotecada.";
        if (propiedad.tieneHotel()) return "La propiedad ya tiene un hotel.";
        if (propiedad.getCantidadCasas() != 4) return "Necesitas cuatro casas en esta propiedad para comprar un hotel.";
        if (hotelesDisponibles <= 0) return "El banco no tiene hoteles disponibles.";
        if (jugador.getSaldo() < propiedad.getPrecioHotel()) return "Saldo insuficiente para comprar un hotel.";
        return null;
    }

    // =========================================================
    // VENDER HOTEL
    // =========================================================

    /**
     * El jugador vende un hotel al banco.
     *
     * Recibe la mitad del precio del hotel.
     */
    public synchronized boolean venderHotel(
            Jugador jugador,
            Propiedad propiedad
    ) {
        int saldoAnterior = jugador == null ? 0 : jugador.getSaldo();


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

        registrar(jugador, TipoTransaccion.VENTA_HOTEL, propiedad.getNombre(), saldoAnterior);

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
    public synchronized boolean hipotecarPropiedad(
            Jugador jugador,
            Propiedad propiedad
    ) {
        int saldoAnterior = jugador == null ? 0 : jugador.getSaldo();


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

        registrar(jugador, TipoTransaccion.HIPOTECA, propiedad.getNombre(), saldoAnterior);

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
    public synchronized boolean quitarHipoteca(
            Jugador jugador,
            Propiedad propiedad
    ) {
        int saldoAnterior = jugador == null ? 0 : jugador.getSaldo();


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

        registrar(jugador, TipoTransaccion.CANCELACION_HIPOTECA, propiedad.getNombre(), saldoAnterior);

        return true;
    }


    // =========================================================
    // PAGOS CON TARJETA RFID
    // =========================================================

    /** Pago validado entre jugadores; una sola transaccion representa ambos movimientos. */
    public synchronized boolean pagarEntreJugadores(Jugador origen, Jugador destino, int monto, String descripcion) {
        if (origen == null || destino == null || origen == destino || !origen.isActivo()
                || !destino.isActivo() || monto <= 0 || descripcion == null
                || origen.getId().equals(destino.getId())
                || destino.getSaldo() > Integer.MAX_VALUE - monto) return false;
        int anteriorOrigen = origen.getSaldo();
        int anteriorDestino = destino.getSaldo();
        if (!origen.debitar(monto)) return false;
        destino.acreditar(monto);
        if (registro != null) {
            registro.registrar(TipoTransaccion.PAGO_ENTRE_JUGADORES, origen, destino, monto, descripcion,
                    anteriorOrigen, origen.getSaldo(), anteriorDestino, destino.getSaldo());
        }
        return true;
    }

    /** Estas operaciones las invoca el servidor al resolver una carta de evento. */
    public synchronized boolean pagarGananciaEvento(Jugador jugador, int monto, String descripcion) {
        if (descripcion == null) return false;
        return pagarJugador(jugador, monto, TipoTransaccion.GANANCIA_EVENTO, descripcion);
    }

    public synchronized boolean cobrarPerdidaEvento(Jugador jugador, int monto, String descripcion) {
        if (descripcion == null) return false;
        return cobrarJugador(jugador, monto, TipoTransaccion.PERDIDA_EVENTO, descripcion);
    }

    private boolean validarTarjetaPago(Jugador jugador, String uid) {
        return jugador != null && jugador.validarTarjeta(uid);
    }

    public synchronized boolean comprarPropiedadConTarjeta(Jugador jugador, Propiedad propiedad, String uid) {
        if (!validarTarjetaPago(jugador, uid)) return false;
        return comprarPropiedad(jugador, propiedad);
    }

}
