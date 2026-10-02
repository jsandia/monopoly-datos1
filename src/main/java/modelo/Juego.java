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

    // Se calculan al terminar la partida
    private Jugador ganador;
    private boolean hayEmpate;

    // Propiedad ajena donde cayo el jugador en turno y cuyo alquiler todavia
    // no pago. Lo debe el jugador del frente de la cola. null = no debe nada.
    private Propiedad alquilerPendiente;

    // Componentes e infraestructuras de la partida

    // Turnos: el del frente es el que juega. Pasar el turno es sacarlo del
    // frente y meterlo al final. Es la misma ColaCircular de los mazos,
    // pero con T = Jugador.
    private final ColaCircular<Jugador> turnos;
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

        this.turnos = new ColaCircular<>(cantidadJugadores);
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

    // Agrega un jugador al final de la cola de turnos: juegan en el orden en que entran
    public synchronized void agregarJugador(Jugador jugador) {
        if (!turnos.encolar(jugador)) {
            throw new IllegalStateException("La partida ya tiene " + turnos.capacidad() + " jugadores");
        }
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
        Jugador actual = turnos.verFrente();
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

        // Tercer intento fallido: la multa es obligatoria
        if (jugador.getSaldo() < Configuracion.MULTA_CARCEL) {
            log.append("\nTercer intento sin dobles.");
            declararBancarrota(jugador, null, TipoTransaccion.PAGO_AL_BANCO,
                    "Multa para salir de la cárcel", Configuracion.MULTA_CARCEL, log);
            return false;
        }
        banco.cobrarMultaCarcel(jugador, Configuracion.MULTA_CARCEL);
        jugador.liberarDeCarcel();
        log.append("\nTercer intento sin dobles: paga la multa de ₡").append(Configuracion.MULTA_CARCEL)
                .append(" y sale de la cárcel.");
        return true;
    }

    /**
     * Paga la multa antes de tirar para salir de la carcel.
     *
     * @return true si estaba preso y le alcanzo para pagar
     */
    public synchronized boolean pagarMultaCarcel() {
        Jugador actual = turnos.verFrente();
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
        Jugador actual = turnos.verFrente();
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

    /**
     * Mueve la ficha casilla por casilla: cada paso es un salto a
     * getSiguiente() en la lista circular del tablero. Despues de la ultima
     * casilla, getSiguiente() ya es Salida, asi que no hace falta ningun
     * calculo de "dar la vuelta".
     *
     * Si en el camino la ficha pisa el nodo de Salida (de paso o para
     * quedarse), cobra el premio.
     */
    private void avanzar(Jugador jugador, int pasos, StringBuilder log) {
        Nodo<Casilla> salida = tablero.getPrimerNodo();
        Nodo<Casilla> actual = tablero.obtenerNodo(jugador.getPosicionActual());
        boolean pasoPorSalida = false;

        for (int i = 0; i < pasos; i++) {
            actual = actual.getSiguiente();
            if (actual == salida) {
                pasoPorSalida = true;
            }
        }

        jugador.setPosicionActual(actual.getDato().getId());
        jugador.registrarRecorrido(pasos);

        log.append("\n").append(jugador.getNombre()).append(" cae en ")
                .append(actual.getDato().getNombre()).append(".");

        if (pasoPorSalida) {
            banco.pagarSalida(jugador, Configuracion.PREMIO_SALIDA);
            log.append("\nPasó por Salida y cobra ₡").append(Configuracion.PREMIO_SALIDA).append(".");
        }
    }

    /**
     * Cuenta cuantos pasos hacia adelante hay desde donde esta el jugador
     * hasta la casilla con ese id, caminando por los nodos. Si ya esta ahi, 0.
     * Como la lista es circular, se limita a una vuelta: si en una vuelta no
     * aparece, la casilla no existe (y sin el limite el ciclo no terminaria).
     */
    private int pasosHasta(Jugador jugador, int idDestino) {
        Nodo<Casilla> actual = tablero.obtenerNodo(jugador.getPosicionActual());
        for (int pasos = 0; pasos < tablero.tamaño(); pasos++) {
            if (actual.getDato().getId() == idDestino) {
                return pasos;
            }
            actual = actual.getSiguiente();
        }
        throw new IllegalArgumentException("No existe la casilla " + idDestino);
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
            if (jugador.getSaldo() < alquiler) {
                // No le alcanza: la bancarrota es inmediata, no se espera ningun pago
                declararBancarrota(jugador, propietario, TipoTransaccion.PAGO_ALQUILER,
                        "Alquiler de " + propiedad.getNombre(), alquiler, log);
            } else {
                // Queda debiendo: se cobra con pagarAlquilerPendiente()
                alquilerPendiente = propiedad;
                log.append("\n").append(jugador.getNombre()).append(" debe pagar ₡")
                        .append(alquiler).append(" de alquiler a ")
                        .append(propietario.getNombre()).append(" por ")
                        .append(propiedad.getNombre()).append(".");
            }
        } else if (casilla instanceof CasillaEspecial especial
                && especial.getTipo() == CasillaEspecial.Tipo.IMPUESTO) {
            if (jugador.getSaldo() < especial.getMonto()) {
                declararBancarrota(jugador, null, TipoTransaccion.PAGO_AL_BANCO,
                        "Pago de impuesto", especial.getMonto(), log);
            } else if (banco.cobrarImpuesto(jugador, especial.getMonto())) {
                log.append("\n").append(jugador.getNombre()).append(" paga ₡")
                        .append(especial.getMonto()).append(" de impuesto al banco.");
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
                if (jugador.getSaldo() < valor) {
                    declararBancarrota(jugador, null, TipoTransaccion.PERDIDA_EVENTO, texto, valor, log);
                } else {
                    banco.cobrarPerdidaEvento(jugador, valor, texto);
                }
            }

            case COBRAR_A_CADA_JUGADOR -> {
                // Aqui el que puede quebrar es otro jugador, fuera de su turno
                for (Jugador otro : jugadoresEnOrdenDeTurno()) {
                    if (otro == jugador || !otro.isActivo()) continue;
                    if (otro.getSaldo() < valor) {
                        declararBancarrota(otro, jugador, TipoTransaccion.PAGO_ENTRE_JUGADORES, texto, valor, log);
                    } else {
                        banco.pagarEntreJugadores(otro, jugador, valor, texto);
                    }
                }
            }

            case PAGAR_A_CADA_JUGADOR -> {
                for (Jugador otro : jugadoresEnOrdenDeTurno()) {
                    if (otro == jugador || !otro.isActivo()) continue;
                    if (jugador.getSaldo() < valor) {
                        // Le paga lo que le quede a este, y los demas ya no reciben nada
                        declararBancarrota(jugador, otro, TipoTransaccion.PAGO_ENTRE_JUGADORES, texto, valor, log);
                        break;
                    }
                    banco.pagarEntreJugadores(jugador, otro, valor, texto);
                }
            }

            case IR_A_CASILLA -> {
                // Siempre hacia adelante: si la casilla quedo atras, da la vuelta y cobra Salida
                avanzar(jugador, pasosHasta(jugador, valor), log);
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
                Nodo<Casilla> nodo = tablero.retroceder(
                        tablero.obtenerNodo(jugador.getPosicionActual()), valor);
                jugador.setPosicionActual(nodo.getDato().getId());
                jugador.registrarRecorrido(-valor);
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
                int monto = jugador.contarCasas() * valor
                        + jugador.contarHoteles() * carta.getValorPorHotel();
                if (monto == 0) {
                    log.append("\nNo tiene casas ni hoteles: no paga nada.");
                } else if (jugador.getSaldo() < monto) {
                    declararBancarrota(jugador, null, TipoTransaccion.PERDIDA_EVENTO, texto, monto, log);
                } else {
                    banco.cobrarPerdidaEvento(jugador, monto, texto);
                    log.append("\nPaga ₡").append(monto).append(" en reparaciones.");
                }
            }

            case PERDER_TURNO -> {
                jugador.perderProximoTurno();
                log.append("\n").append(jugador.getNombre()).append(" pierde su próximo turno.");
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

    /**
     * Termina el turno: el jugador del frente sale de la cola y, si sigue
     * activo, vuelve a entrar al final. Asi el siguiente queda al frente.
     *
     * <pre>
     *   antes:   frente → [Ana, Beto, Caro, Dani]
     *   despues: frente → [Beto, Caro, Dani, Ana]
     *
     *   si Ana quebro en su turno, simplemente no se vuelve a encolar:
     *   despues: frente → [Beto, Caro, Dani]
     * </pre>
     *
     * Si al que le toca tiene un turno por perder, lo gasta y se pasa al
     * siguiente. Un turno perdido no suma al contador global.
     *
     * @return los avisos del cambio de turno (por ejemplo, quien perdio su
     *         turno); vacio si no hubo nada especial
     */
    public synchronized String terminarTurnoActual() {
        if (terminado) return "";
        if (alquilerPendiente != null) {
            throw new IllegalStateException("No se puede terminar el turno con un alquiler sin pagar");
        }
        StringBuilder log = new StringBuilder();

        Jugador saliente = turnos.desencolar();
        if (saliente.isActivo()) {
            turnos.encolar(saliente);
        }

        this.turnoActualGlobal++;
        registro.setTurno(turnoActualGlobal);
        if (turnoActualGlobal > maxTurnos) {
            terminarPartida();
            return log.toString();
        }

        // Cada jugador gasta como mucho un turno perdido por vuelta, asi que el ciclo termina
        while (turnos.verFrente().consumirTurnoPerdido()) {
            if (log.length() > 0) log.append("\n");
            log.append(turnos.verFrente().getNombre()).append(" pierde este turno.");
            pasarAlSiguiente();
        }
        return log.toString();
    }

    // Desencolar del frente y encolar al final: eso es pasar el turno
    private void pasarAlSiguiente() {
        turnos.encolar(turnos.desencolar());
    }

    /**
     * Saca de la cola a un jugador que no esta al frente (quebro fuera de
     * su turno). Se da una vuelta completa a la cola y se vuelven a encolar
     * todos menos el. El orden de los demas no cambia.
     */
    private void sacarDeLaCola(Jugador eliminado) {
        int cantidad = turnos.tamaño();
        for (int i = 0; i < cantidad; i++) {
            Jugador jugador = turnos.desencolar();
            if (jugador != eliminado) {
                turnos.encolar(jugador);
            }
        }
    }

    // =========================================================
    // ALQUILER PENDIENTE
    // =========================================================
    // Al caer en una propiedad ajena el alquiler no se cobra solo: queda
    // pendiente hasta que el jugador lo autorice (con su tarjeta RFID, pero
    // eso lo valida el servidor; Juego no sabe de tarjetas).

    /** @return true si el jugador en turno debe un alquiler */
    public synchronized boolean hayAlquilerPendiente() {
        return alquilerPendiente != null;
    }

    /** @return la propiedad cuyo alquiler se debe, o null */
    public synchronized Propiedad getAlquilerPendiente() {
        return alquilerPendiente;
    }

    /**
     * Cobra el alquiler pendiente al jugador en turno y se lo da al dueño.
     * Si en el camino el saldo dejo de alcanzar, es bancarrota.
     *
     * @return lo que paso, para mostrarlo a los jugadores
     * @throws IllegalStateException si no hay ningun alquiler pendiente
     */
    public synchronized String pagarAlquilerPendiente() {
        if (alquilerPendiente == null) {
            throw new IllegalStateException("No hay ningun alquiler pendiente");
        }
        Propiedad propiedad = alquilerPendiente;
        alquilerPendiente = null;

        Jugador jugador = turnos.verFrente();
        Jugador propietario = propiedad.getPropietario();
        int alquiler = propiedad.calcularAlquiler();
        StringBuilder log = new StringBuilder();

        if (jugador.getSaldo() < alquiler) {
            declararBancarrota(jugador, propietario, TipoTransaccion.PAGO_ALQUILER,
                    "Alquiler de " + propiedad.getNombre(), alquiler, log);
        } else {
            banco.cobrarAlquiler(jugador, propiedad);
            log.append(jugador.getNombre()).append(" paga ₡")
                    .append(alquiler).append(" de alquiler a ")
                    .append(propietario.getNombre()).append(" por ")
                    .append(propiedad.getNombre()).append(".");
        }
        return log.toString().strip();
    }

    // =========================================================
    // BANCARROTA Y FIN DE PARTIDA
    // =========================================================

    /**
     * Un jugador no alcanza a cubrir un pago obligatorio:
     * entrega lo que tiene, sus propiedades vuelven al banco y queda fuera.
     *
     * Si es el del turno, se queda al frente de la cola hasta que termine su
     * turno (terminarTurnoActual no lo vuelve a encolar). Si no es el del
     * turno, sale de la cola de una vez.
     *
     * @param acreedor a quien le debia (null = el banco)
     * @param monto    lo que debia pagar, solo para el mensaje
     */
    private void declararBancarrota(Jugador deudor, Jugador acreedor, TipoTransaccion tipo,
            String motivo, int monto, StringBuilder log) {
        int entregado = banco.liquidarBancarrota(deudor, acreedor, tipo, motivo);
        deudor.setActivo(false);
        deudor.liberarDeCarcel();

        // Sus cartas "Salga de la carcel" vuelven a su mazo
        CartaEvento carta;
        while ((carta = deudor.usarCartaSalirCarcel()) != null) {
            mazoDe(carta.getMazo()).encolar(carta);
        }

        if (deudor != turnos.verFrente()) {
            sacarDeLaCola(deudor);
        }

        String nombreAcreedor = (acreedor == null) ? "el banco" : acreedor.getNombre();
        log.append("\n💀 ").append(deudor.getNombre()).append(" no puede pagar ₡").append(monto)
                .append(" (").append(motivo).append(") y queda en BANCARROTA.")
                .append(" Le entrega ₡").append(entregado).append(" a ").append(nombreAcreedor)
                .append(" y sus propiedades vuelven al banco.");

        if (contarJugadoresActivos() <= 1) {
            terminarPartida();
        }
    }

    /**
     * Cierra la partida y elige ganador: el de mayor patrimonio entre los
     * activos (si queda uno solo, es ese). En empate gana el primero en el
     * orden de turnos y se marca hayEmpate.
     */
    private void terminarPartida() {
        if (terminado) return;
        terminado = true;

        for (Jugador jugador : jugadoresEnOrdenDeTurno()) {
            if (!jugador.isActivo()) continue;
            if (ganador == null || jugador.calcularPatrimonio() > ganador.calcularPatrimonio()) {
                ganador = jugador;
                hayEmpate = false;
            } else if (jugador.calcularPatrimonio() == ganador.calcularPatrimonio()) {
                hayEmpate = true;
            }
        }

        String linea = "FIN DE PARTIDA | Turno " + turnoActualGlobal;
        if (ganador != null) {
            linea += " | Ganador: " + ganador.getNombre()
                    + " | Patrimonio: " + ganador.calcularPatrimonio()
                    + (hayEmpate ? " | Empate en patrimonio" : "");
        }
        registro.escribir(linea);
    }

    /** @return el ganador, o null si la partida no ha terminado */
    public synchronized Jugador getGanador() {
        return ganador;
    }

    /** @return true si el ganador empato en patrimonio con otro jugador */
    public synchronized boolean hayEmpate() {
        return hayEmpate;
    }

    /**
     * Devuelve a todos los jugadores, empezando por el que tiene el turno,
     * sin cambiar el turno.
     *
     * Una cola solo deja ver el frente. Para ver a todos se da una vuelta
     * completa: se saca cada jugador del frente, se anota y se vuelve a meter
     * al final. Despues de tamaño() vueltas la cola queda exactamente igual.
     */
    private Jugador[] jugadoresEnOrdenDeTurno() {
        Jugador[] jugadores = new Jugador[turnos.tamaño()];
        for (int i = 0; i < jugadores.length; i++) {
            Jugador jugador = turnos.desencolar();
            jugadores[i] = jugador;
            turnos.encolar(jugador);
        }
        return jugadores;
    }

    private int contarJugadoresActivos() {
        int activos = 0;
        for (Jugador jugador : jugadoresEnOrdenDeTurno()) {
            if (jugador.isActivo()) {
                activos++;
            }
        }
        return activos;
    }

    // Retorna el jugador que tiene el turno actual: el del frente de la cola
    public synchronized Jugador getJugadorActual() {
        return turnos.verFrente();
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
