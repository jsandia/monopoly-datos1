package modelo;

import config.Configuracion;

/**
 * El banco del juego: tiene su propio dinero, las casas y los hoteles que
 * todavía no se han construido, y hace todas las operaciones de dinero.
 *
 * Cada operación primero valida y, solo si todo está bien, mueve el dinero
 * y registra la transacción. Si algo falla devuelve false y no cambia nada.
 *
 * Los métodos públicos son synchronized: los usan varios hilos del servidor.
 */
public class Banco {

    private int saldo;
    private int casasDisponibles;
    private int hotelesDisponibles;
    private RegistroTransacciones registro; // null = no se registra (algunas pruebas)

    /** Crea un banco con el dinero, las casas y los hoteles de Configuracion. */
    public Banco() {
        this(Configuracion.SALDO_BANCO, Configuracion.CASAS_BANCO, Configuracion.HOTELES_BANCO);
    }

    /** Crea un banco con cantidades propias. Los valores negativos se toman como 0. */
    public Banco(int saldoInicial, int casasIniciales, int hotelesIniciales) {
        this.saldo = Math.max(0, saldoInicial);
        this.casasDisponibles = Math.max(0, casasIniciales);
        this.hotelesDisponibles = Math.max(0, hotelesIniciales);
    }

    public synchronized void setRegistroTransacciones(RegistroTransacciones registro) {
        this.registro = registro;
    }

    public int getSaldo() {
        return saldo;
    }

    public int getCasasDisponibles() {
        return casasDisponibles;
    }

    public int getHotelesDisponibles() {
        return hotelesDisponibles;
    }

    // =========================================================
    // PAGOS ENTRE EL BANCO Y UN JUGADOR
    // =========================================================

    /** El banco le paga al jugador al pasar por Salida. */
    public synchronized boolean pagarSalida(Jugador jugador, int monto) {
        return pagarJugador(jugador, monto, TipoTransaccion.PREMIO_INICIO, "Premio por pasar por inicio");
    }

    /** El jugador le paga un impuesto al banco. */
    public synchronized boolean cobrarImpuesto(Jugador jugador, int impuesto) {
        return cobrarJugador(jugador, impuesto, TipoTransaccion.PAGO_AL_BANCO, "Pago de impuesto");
    }

    /** El jugador le paga al banco la multa para salir de la cárcel. */
    public synchronized boolean cobrarMultaCarcel(Jugador jugador, int multa) {
        return cobrarJugador(jugador, multa, TipoTransaccion.PAGO_AL_BANCO, "Multa para salir de la cárcel");
    }

    /** Carta de evento a favor del jugador: le paga el banco. */
    public synchronized boolean pagarGananciaEvento(Jugador jugador, int monto, String descripcion) {
        return descripcion != null
                && pagarJugador(jugador, monto, TipoTransaccion.GANANCIA_EVENTO, descripcion);
    }

    /** Carta de evento en contra del jugador: le paga al banco. */
    public synchronized boolean cobrarPerdidaEvento(Jugador jugador, int monto, String descripcion) {
        return descripcion != null
                && cobrarJugador(jugador, monto, TipoTransaccion.PERDIDA_EVENTO, descripcion);
    }

    // El banco le da dinero al jugador, si al banco le alcanza
    private boolean pagarJugador(Jugador jugador, int monto, TipoTransaccion tipo, String descripcion) {
        if (jugador == null || monto <= 0 || monto > saldo) return false;
        int anterior = jugador.getSaldo();
        saldo -= monto;
        jugador.acreditar(monto);
        registrar(jugador, tipo, descripcion, anterior);
        return true;
    }

    // El jugador le da dinero al banco, si al jugador le alcanza
    private boolean cobrarJugador(Jugador jugador, int monto, TipoTransaccion tipo, String descripcion) {
        if (jugador == null || monto <= 0) return false;
        int anterior = jugador.getSaldo();
        if (!jugador.debitar(monto)) return false;
        saldo += monto;
        registrar(jugador, tipo, descripcion, anterior);
        return true;
    }

    /**
     * Registra un movimiento entre el banco y un jugador. Si el saldo del
     * jugador subió, el banco fue el origen; si bajó, fue el destino.
     */
    private void registrar(Jugador jugador, TipoTransaccion tipo, String descripcion, int anterior) {
        if (registro == null) return;
        int diferencia = jugador.getSaldo() - anterior;
        int bancoAnterior = saldo + diferencia; // el banco se movió al revés que el jugador
        if (diferencia > 0) {
            registro.registrar(tipo, null, jugador, diferencia, descripcion,
                    bancoAnterior, saldo, anterior, jugador.getSaldo());
        } else if (diferencia < 0) {
            registro.registrar(tipo, jugador, null, -diferencia, descripcion,
                    anterior, jugador.getSaldo(), bancoAnterior, saldo);
        }
    }

    // =========================================================
    // PROPIEDADES Y ALQUILERES
    // =========================================================

    /** El jugador le compra al banco una propiedad sin dueño. */
    public synchronized boolean comprarPropiedad(Jugador jugador, Propiedad propiedad) {
        if (propiedad == null || propiedad.tienePropietario()) return false;
        if (!cobrarJugador(jugador, propiedad.getPrecioCompra(),
                TipoTransaccion.COMPRA_PROPIEDAD, propiedad.getNombre())) return false;
        propiedad.setPropietario(jugador);
        jugador.agregarPropiedad(propiedad);
        return true;
    }

    /**
     * El jugador le paga el alquiler al dueño de la propiedad. No hay pago si
     * no tiene dueño, si es suya o si no le alcanza el saldo.
     */
    public synchronized boolean cobrarAlquiler(Jugador jugador, Propiedad propiedad) {
        if (jugador == null || propiedad == null) return false;
        Jugador propietario = propiedad.getPropietario();
        if (propietario == null || propietario == jugador) return false;

        int alquiler = propiedad.calcularAlquiler();
        int anteriorJugador = jugador.getSaldo();
        int anteriorPropietario = propietario.getSaldo();
        if (alquiler <= 0 || !jugador.debitar(alquiler)) return false;
        propietario.acreditar(alquiler);

        if (registro != null) {
            registro.registrar(TipoTransaccion.PAGO_ALQUILER, jugador, propietario, alquiler,
                    "Alquiler de " + propiedad.getNombre(), anteriorJugador, jugador.getSaldo(),
                    anteriorPropietario, propietario.getSaldo());
        }
        return true;
    }

    // =========================================================
    // CASAS Y HOTELES
    // =========================================================

    /** @return null si el jugador puede comprar una casa ahí; si no, el motivo */
    public synchronized String motivoNoComprarCasa(Jugador jugador, Propiedad propiedad) {
        if (propiedad == null) return "Selecciona una propiedad válida.";
        String motivo = propiedad.motivoNoConstruir(jugador);
        if (motivo != null) return motivo;
        if (casasDisponibles <= 0) return "El banco no tiene casas disponibles.";
        if (jugador.getSaldo() < propiedad.getPrecioCasa()) return "Saldo insuficiente para comprar una casa.";
        return null;
    }

    /** Le vende una casa al jugador para esa propiedad. */
    public synchronized boolean comprarCasa(Jugador jugador, Propiedad propiedad) {
        if (motivoNoComprarCasa(jugador, propiedad) != null) return false;
        cobrarJugador(jugador, propiedad.getPrecioCasa(), TipoTransaccion.COMPRA_CASA, propiedad.getNombre());
        propiedad.agregarCasa();
        casasDisponibles--;
        return true;
    }

    /** @return null si el jugador puede comprar un hotel ahí; si no, el motivo */
    public synchronized String motivoNoComprarHotel(Jugador jugador, Propiedad propiedad) {
        if (propiedad == null) return "Selecciona una propiedad válida.";
        if (jugador == null || propiedad.getPropietario() != jugador) return "La propiedad no pertenece al jugador en turno.";
        if (!propiedad.esEdificable()) return "No se construyen hoteles en transportes ni servicios.";
        if (propiedad.tieneHotel()) return "La propiedad ya tiene un hotel.";
        if (propiedad.getCantidadCasas() != Configuracion.MAX_CASAS_POR_PROPIEDAD) return "Necesitas cuatro casas en esta propiedad para comprar un hotel.";
        if (hotelesDisponibles <= 0) return "El banco no tiene hoteles disponibles.";
        if (jugador.getSaldo() < propiedad.getPrecioHotel()) return "Saldo insuficiente para comprar un hotel.";
        return null;
    }

    /** Le vende un hotel al jugador: reemplaza las 4 casas, que vuelven al banco. */
    public synchronized boolean comprarHotel(Jugador jugador, Propiedad propiedad) {
        if (motivoNoComprarHotel(jugador, propiedad) != null) return false;
        cobrarJugador(jugador, propiedad.getPrecioHotel(), TipoTransaccion.COMPRA_HOTEL, propiedad.getNombre());
        propiedad.construirHotel();
        hotelesDisponibles--;
        casasDisponibles += Configuracion.MAX_CASAS_POR_PROPIEDAD;
        return true;
    }

    // =========================================================
    // PAGOS ENTRE JUGADORES
    // =========================================================

    /** Un jugador le paga a otro. Una sola transacción representa los dos movimientos. */
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

    // =========================================================
    // BANCARROTA
    // =========================================================

    /**
     * El deudor no alcanza a cubrir un pago obligatorio:
     * 1. entrega todo el saldo que tenga al acreedor (null = el banco);
     * 2. sus propiedades vuelven al banco sin dueño, y sus casas y hoteles
     *    vuelven al inventario del banco.
     *
     * Marcarlo inactivo y sacarlo de los turnos le toca a Juego.
     *
     * @param tipo        tipo del pago que no pudo cubrir (alquiler, impuesto...)
     * @param descripcion motivo del pago, para el historial
     * @return cuánto dinero alcanzó a entregar
     */
    public synchronized int liquidarBancarrota(Jugador deudor, Jugador acreedor,
            TipoTransaccion tipo, String descripcion) {
        int entregado = deudor.getSaldo();
        String detalle = descripcion + " (bancarrota)";

        if (entregado > 0) {
            if (acreedor == null) {
                cobrarJugador(deudor, entregado, tipo, detalle);
            } else {
                int anteriorAcreedor = acreedor.getSaldo();
                deudor.debitar(entregado);
                acreedor.acreditar(entregado);
                if (registro != null) {
                    registro.registrar(tipo, deudor, acreedor, entregado, detalle,
                            entregado, 0, anteriorAcreedor, acreedor.getSaldo());
                }
            }
        }

        for (Propiedad propiedad : deudor.entregarTodasLasPropiedades()) {
            // Al comprar un hotel las 4 casas ya habían vuelto al banco:
            // aquí solo regresa el hotel (o las casas sueltas que tenga)
            if (propiedad.tieneHotel()) {
                hotelesDisponibles++;
            }
            casasDisponibles += propiedad.getCantidadCasas();
            propiedad.devolverAlBanco();
        }
        return entregado;
    }
}
