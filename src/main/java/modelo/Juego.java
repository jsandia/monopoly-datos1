package modelo;

import config.Configuracion;
import estructuras.ColaCircular;
import estructuras.Nodo;

/**
 * Clase Juego
 * Administra el estado global de la partida: el tablero circular, el banco,
 * los turnos, los mazos de cartas y la carcel.
 * Permite ejecutar las acciones por medio de software mientras el hardware no este conectado.
 *
 * Solo el servidor tiene un Juego. Los metodos que cambian el estado devuelven
 * un texto con lo que paso, para que el servidor se lo mande a los clientes.
 */
public class Juego {

    // Los montos y reglas (Salida, multa, intentos en la carcel...) estan en Configuracion

    // Variables para el control de turnos y fin de juego
    private int maxTurnos;
    private int turnoActualGlobal;
    private boolean terminado;

    // Componentes e infraestructuras de la partida
    private ColaCircularTurnos colaTurnos;
    private final Tablero tablero;
    private Banco banco;
    private RegistroTransacciones registro;

    // Mazos: se saca la carta del frente, se aplica y se mete al final
    private final ColaCircular<CartaEvento> mazoArcaComunal;
    private final ColaCircular<CartaEvento> mazoSuerte;

    // Constructor que inicializa el juego con limite de turnos y la cantidad de jugadores
    public Juego(int maxTurnos, int cantidadJugadores) {
        this(maxTurnos, cantidadJugadores, "local");
    }

    public Juego(int maxTurnos, int cantidadJugadores, String codigoPartida) {
        this(maxTurnos, cantidadJugadores, codigoPartida, true);
    }

    /**
     * @param barajarMazos false deja las cartas en el orden de CartaEvento
     *                     (solo para pruebas, donde hace falta saber que sale)
     */
    public Juego(int maxTurnos, int cantidadJugadores, String codigoPartida, boolean barajarMazos) {
        this.registro = new RegistroTransacciones(codigoPartida);
        this.maxTurnos = maxTurnos;
        this.turnoActualGlobal = 1;
        this.terminado = false;

        this.colaTurnos = new ColaCircularTurnos(cantidadJugadores);
        this.tablero = new Tablero();
        this.banco = new Banco();
        this.banco.setRegistroTransacciones(registro);

        this.mazoArcaComunal = crearMazo(CartaEvento.crearMazoArcaComunal(), barajarMazos);
        this.mazoSuerte = crearMazo(CartaEvento.crearMazoSuerte(), barajarMazos);
    }

    // Mete las cartas en una cola circular del tamano exacto del mazo
    private static ColaCircular<CartaEvento> crearMazo(CartaEvento[] cartas, boolean barajar) {
        if (barajar) {
            barajar(cartas);
        }
        ColaCircular<CartaEvento> mazo = new ColaCircular<>(cartas.length);
        for (CartaEvento carta : cartas) {
            mazo.encolar(carta);
        }
        return mazo;
    }

    // Fisher-Yates: recorre de atras hacia adelante cambiando cada carta
    // con otra al azar de las que quedan antes. Todas las mezclas son igual de probables.
    private static void barajar(CartaEvento[] cartas) {
        for (int i = cartas.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            CartaEvento temporal = cartas[i];
            cartas[i] = cartas[j];
            cartas[j] = temporal;
        }
    }

    // Agrega un jugador a la cola circular de turnos
    public void agregarJugador(Jugador jugador) {
        this.colaTurnos.encolar(jugador);
        if (jugador.getSaldo() > 0) {
            registro.registrar(TipoTransaccion.SALDO_INICIAL, null, jugador, jugador.getSaldo(),
                    "Asignacion inicial de dinero", banco.getSaldo(), banco.getSaldo(), 0, jugador.getSaldo());
        }
    }

    // Genera dos numeros aleatorios de 1 a 6 para simular el tiro de dados en software
    public int[] lanzarDadosSoftware() {
        int dado1 = (int) (Math.random() * 6) + 1;
        int dado2 = (int) (Math.random() * 6) + 1;
        return new int[]{dado1, dado2};
    }

    // Mueve al jugador de turno la cantidad de casillas indicadas por los dados
    public void ejecutarMovimientoJugadorActual(int totalPasos) {
        Jugador actual = colaTurnos.obtenerTurnoActual();
        if (actual == null || !actual.isActivo()) return;
        avanzar(actual, totalPasos, new StringBuilder());
    }

    // =========================================================
    // TIRO COMPLETO: CARCEL + MOVIMIENTO + EFECTO DE LA CASILLA
    // =========================================================

    /**
     * Aplica una tirada al jugador en turno con todas las reglas:
     * si esta preso intenta salir; si se mueve, cobra Salida al pasar
     * y resuelve la casilla donde cae (alquileres, impuestos, cartas y carcel).
     *
     * @return lo que paso, una linea por evento, para mostrar a los jugadores
     */
    public synchronized String jugarTiro(int dado1, int dado2) {
        Jugador actual = colaTurnos.obtenerTurnoActual();
        StringBuilder log = new StringBuilder();
        if (actual == null || !actual.isActivo()) return "";

        int suma = dado1 + dado2;
        log.append(actual.getNombre()).append(" sacó [").append(dado1).append("][")
                .append(dado2).append("] = ").append(suma);

        if (actual.isEnCarcel()) {
            if (!intentarSalirConDados(actual, dado1 == dado2, log)) {
                return log.toString(); // sigue preso: no se mueve
            }
        }

        avanzar(actual, suma, log);
        resolverCasilla(actual, log);
        return log.toString();
    }

    // Reglas clasicas de la carcel al tirar estando preso.
    // Devuelve true si sale (y entonces se mueve la suma de los dados).
    private boolean intentarSalirConDados(Jugador jugador, boolean dobles, StringBuilder log) {
        if (dobles) {
            jugador.liberarDeCarcel();
            log.append("\n¡Dobles! ").append(jugador.getNombre()).append(" sale de la cárcel.");
            return true;
        }

        jugador.sumarIntentoCarcel();

        if (jugador.getTurnosEnCarcel() < Configuracion.MAX_INTENTOS_CARCEL) {
            log.append("\nNo sacó dobles. Sigue en la cárcel (intento ")
                    .append(jugador.getTurnosEnCarcel()).append(" de ")
                    .append(Configuracion.MAX_INTENTOS_CARCEL).append(").");
            return false;
        }

        // Tercer intento fallido: paga la multa a la fuerza y sale
        if (banco.cobrarMultaCarcel(jugador, Configuracion.MULTA_CARCEL)) {
            jugador.liberarDeCarcel();
            log.append("\nTercer intento sin dobles: paga la multa de ₡").append(Configuracion.MULTA_CARCEL)
                    .append(" y sale de la cárcel.");
            return true;
        }

        // Todavia no hay bancarrota: si no le alcanza, se queda preso
        log.append("\nTercer intento sin dobles, pero no le alcanza para la multa de ₡")
                .append(Configuracion.MULTA_CARCEL).append(". Sigue en la cárcel.");
        return false;
    }

    /**
     * Paga la multa antes de tirar para salir de la carcel.
     *
     * @return true si estaba preso y le alcanzo para pagar
     */
    public synchronized boolean pagarMultaCarcel() {
        Jugador actual = colaTurnos.obtenerTurnoActual();
        if (actual == null || !actual.isEnCarcel()) return false;
        if (!banco.cobrarMultaCarcel(actual, Configuracion.MULTA_CARCEL)) return false;
        actual.liberarDeCarcel();
        return true;
    }

    /**
     * Usa una carta "Salga de la carcel" antes de tirar. La carta vuelve
     * al final de su mazo, como en el juego original.
     *
     * @return true si estaba preso y tenia carta
     */
    public synchronized boolean usarCartaSalirCarcel() {
        Jugador actual = colaTurnos.obtenerTurnoActual();
        if (actual == null || !actual.isEnCarcel()) return false;
        CartaEvento carta = actual.usarCartaSalirCarcel();
        if (carta == null) return false;
        mazoDe(carta.getMazo()).encolar(carta);
        actual.liberarDeCarcel();
        return true;
    }

    // =========================================================
    // MOVIMIENTO Y CASILLAS
    // =========================================================

    // Avanza pasos casillas; si da la vuelta al tablero cobra la Salida
    private void avanzar(Jugador jugador, int pasos, StringBuilder log) {
        int posicionAnterior = jugador.getPosicionActual();
        jugador.mover(pasos, tablero.tamaño());

        log.append("\n").append(jugador.getNombre()).append(" cae en ")
                .append(tablero.obtenerCasilla(jugador.getPosicionActual()).getNombre()).append(".");

        if (jugador.getPosicionActual() < posicionAnterior) {
            banco.pagarSalida(jugador, Configuracion.PREMIO_SALIDA);
            log.append("\nPasó por Salida y cobra ₡").append(Configuracion.PREMIO_SALIDA).append(".");
        }
    }

    // Lo que pasa al caer en la casilla donde quedo el jugador
    private void resolverCasilla(Jugador jugador, StringBuilder log) {
        Casilla casilla = tablero.obtenerCasilla(jugador.getPosicionActual());

        if (casilla instanceof Propiedad propiedad) {
            Jugador propietario = propiedad.getPropietario();
            if (propietario == null || propietario == jugador || propiedad.estaHipotecada()) {
                return;
            }

            int alquiler = propiedad.calcularAlquiler();
            if (banco.cobrarAlquiler(jugador, propiedad)) {
                log.append("\n").append(jugador.getNombre()).append(" paga ₡")
                        .append(alquiler).append(" de alquiler a ")
                        .append(propietario.getNombre()).append(" por ")
                        .append(propiedad.getNombre()).append(".");
            } else if (jugador.getSaldo() < alquiler) {
                log.append("\n").append(jugador.getNombre())
                        .append(" no tiene suficiente saldo para pagar ₡")
                        .append(alquiler).append(" de alquiler a ")
                        .append(propietario.getNombre()).append(".");
            }
        } else if (casilla instanceof CasillaEspecial especial
                && especial.getTipo() == CasillaEspecial.Tipo.IMPUESTO) {
            if (banco.cobrarImpuesto(jugador, especial.getMonto())) {
                log.append("\n").append(jugador.getNombre()).append(" paga ₡")
                        .append(especial.getMonto()).append(" de impuesto al banco.");
            } else {
                log.append("\n").append(jugador.getNombre())
                        .append(" no tiene suficiente saldo para pagar el impuesto de ₡")
                        .append(especial.getMonto()).append(".");
            }
        } else if (casilla instanceof CasillaEspecial especial
                && especial.getTipo() == CasillaEspecial.Tipo.IR_A_CARCEL) {
            enviarACarcel(jugador, log);

        } else if (casilla instanceof CasillaEvento evento) {
            tomarCarta(jugador, evento.getTipo(), log);
        }
    }

    private void enviarACarcel(Jugador jugador, StringBuilder log) {
        jugador.enviarACarcel(Configuracion.POSICION_CARCEL);
        log.append("\n").append(jugador.getNombre())
                .append(" va directo a la cárcel, sin pasar por Salida.");
    }

    // =========================================================
    // CARTAS DE EVENTO
    // =========================================================

    // Saca la carta del frente, la aplica y la mete al final.
    // "Salga de la carcel" es la excepcion: el jugador se la queda.
    private void tomarCarta(Jugador jugador, CasillaEvento.Tipo tipoMazo, StringBuilder log) {
        ColaCircular<CartaEvento> mazo = mazoDe(tipoMazo);
        CartaEvento carta = mazo.desencolar();
        if (carta == null) {
            // No deberia pasar: solo la carta de carcel sale del mazo sin volver
            log.append("\nEl mazo está vacío.");
            return;
        }

        String nombreMazo = (tipoMazo == CasillaEvento.Tipo.SUERTE) ? "Suerte" : "Arca Comunal";
        log.append("\n").append(nombreMazo).append(": \"").append(carta.getTexto()).append("\"");

        int saldoAntes = jugador.getSaldo();
        boolean seLaQueda = aplicarCarta(jugador, carta, log);
        if (!seLaQueda) {
            mazo.encolar(carta);
        }

        // Confirma el efecto en plata para que se vea que la carta se aplico
        if (jugador.getSaldo() != saldoAntes) {
            log.append("\nSaldo de ").append(jugador.getNombre()).append(": ₡").append(saldoAntes)
                    .append(" → ₡").append(jugador.getSaldo()).append(".");
        }
    }

    // Aplica el efecto de una carta. Devuelve true si el jugador se queda con ella.
    private boolean aplicarCarta(Jugador jugador, CartaEvento carta, StringBuilder log) {
        int valor = carta.getValor();
        String texto = carta.getTexto();

        switch (carta.getAccion()) {
            case COBRAR_BANCO -> banco.pagarGananciaEvento(jugador, valor, texto);

            case PAGAR_BANCO -> {
                if (!banco.cobrarPerdidaEvento(jugador, valor, texto)) {
                    log.append("\nNo le alcanza para pagar ₡").append(valor).append(".");
                }
            }

            case COBRAR_A_CADA_JUGADOR -> {
                for (int i = 0; i < colaTurnos.getCantidad(); i++) {
                    Jugador otro = colaTurnos.obtenerPorIndice(i);
                    if (otro == jugador || !otro.isActivo()) continue;
                    if (!banco.pagarEntreJugadores(otro, jugador, valor, texto)) {
                        log.append("\n").append(otro.getNombre()).append(" no tiene para pagar ₡").append(valor).append(".");
                    }
                }
            }

            case PAGAR_A_CADA_JUGADOR -> {
                for (int i = 0; i < colaTurnos.getCantidad(); i++) {
                    Jugador otro = colaTurnos.obtenerPorIndice(i);
                    if (otro == jugador || !otro.isActivo()) continue;
                    if (!banco.pagarEntreJugadores(jugador, otro, valor, texto)) {
                        log.append("\nNo le alcanza para pagarle ₡").append(valor)
                                .append(" a ").append(otro.getNombre()).append(".");
                    }
                }
            }

            case IR_A_CASILLA -> {
                // Pasos hacia adelante hasta la casilla destino (el + tamaño evita negativos)
                int pasos = (valor - jugador.getPosicionActual() + tablero.tamaño()) % tablero.tamaño();
                avanzar(jugador, pasos, log);
                resolverCasilla(jugador, log);
            }

            case IR_AL_TRANSPORTE_MAS_CERCANO -> {
                // Camina con getSiguiente() hasta encontrar un aeropuerto o puerto
                Nodo<Casilla> nodo = tablero.obtenerNodo(jugador.getPosicionActual());
                int pasos = 0;
                do {
                    nodo = nodo.getSiguiente();
                    pasos++;
                } while (!(nodo.getDato() instanceof Propiedad p && p.getGrupo().equals("Transporte")));
                avanzar(jugador, pasos, log);
                resolverCasilla(jugador, log);
            }

            case RETROCEDER -> {
                // Aqui se usa el puntero anterior de la lista circular doble.
                // Retroceder nunca cobra Salida, aunque cruce la casilla 0.
                Nodo<Casilla> nodo = tablero.obtenerNodo(jugador.getPosicionActual());
                for (int i = 0; i < valor; i++) {
                    nodo = nodo.getAnterior();
                }
                jugador.setPosicionActual(nodo.getDato().getId());
                jugador.registrarMovimiento(-valor);
                log.append("\n").append(jugador.getNombre()).append(" retrocede a ")
                        .append(nodo.getDato().getNombre()).append(".");
                resolverCasilla(jugador, log);
            }

            case IR_A_CARCEL -> enviarACarcel(jugador, log);

            case SALIR_DE_CARCEL -> {
                if (jugador.guardarCartaSalirCarcel(carta)) {
                    log.append("\n").append(jugador.getNombre()).append(" guarda la carta para usarla después.");
                    return true;
                }
                return false; // ya tiene 2 (no deberia pasar): vuelve al mazo
            }

            case REPARACIONES -> {
                ListaPropiedades propiedades = jugador.getPropiedades();
                int monto = propiedades.contarCasas() * valor
                        + propiedades.contarHoteles() * carta.getValorPorHotel();
                if (monto == 0) {
                    log.append("\nNo tiene casas ni hoteles: no paga nada.");
                } else if (banco.cobrarPerdidaEvento(jugador, monto, texto)) {
                    log.append("\nPaga ₡").append(monto).append(" en reparaciones.");
                } else {
                    log.append("\nNo le alcanza para pagar ₡").append(monto).append(" de reparaciones.");
                }
            }
        }
        return false;
    }

    private ColaCircular<CartaEvento> mazoDe(CasillaEvento.Tipo tipo) {
        return (tipo == CasillaEvento.Tipo.SUERTE) ? mazoSuerte : mazoArcaComunal;
    }

    // =========================================================
    // TURNOS
    // =========================================================

    // Finaliza el turno del jugador activo y avanza la cola circular al siguiente
    public void terminarTurnoActual() {
        colaTurnos.pasarTurno();
        this.turnoActualGlobal++;
        registro.setTurno(turnoActualGlobal);

        // Verifica si la partida debe terminar por limite de turnos o bancarrota general
        if (this.turnoActualGlobal > this.maxTurnos || colaTurnos.contarJugadoresActivos() <= 1) {
            if (!this.terminado) registro.escribir("FIN DE PARTIDA | Turno " + turnoActualGlobal);
            this.terminado = true;
        }
    }

    // Retorna el jugador que tiene el turno actual
    public Jugador getJugadorActual() {
        return colaTurnos.obtenerTurnoActual();
    }

    // Retorna true si la partida ya finalizo
    public boolean esFinDeJuego() {
        return terminado;
    }

    // Retorna la instancia del banco
    public Banco getBanco() {
        return banco;
    }

    // Retorna el tablero del juego
    public Tablero getTablero() {
        return tablero;
    }

    public ColaCircular<CartaEvento> getMazoArcaComunal() {
        return mazoArcaComunal;
    }

    public ColaCircular<CartaEvento> getMazoSuerte() {
        return mazoSuerte;
    }

    // Retorna el contador del turno global
    public int getTurnoActualGlobal() {
        return turnoActualGlobal;
    }

    public RegistroTransacciones getRegistroTransacciones() {
        return registro;
    }

    public java.nio.file.Path getArchivoTransacciones() {
        return registro.getArchivo();
    }

    public int getMaxTurnos() {
        return maxTurnos;
    }
}
