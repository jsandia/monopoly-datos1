package modelo;

import estructuras.ListaCircularDoble;

/**
 * Clase Juego
 * Administra el estado global de la partida, el tablero circular, el banco y los turnos.
 * Permite ejecutar las acciones por medio de software mientras el hardware no este conectado.
 */
public class Juego {

    // Variables para el control de turnos y fin de juego
    private int maxTurnos;
    private int turnoActualGlobal;
    private boolean terminado;

    // Componentes e infraestructuras de la partida
    private ColaCircularTurnos colaTurnos;
    private ListaCircularDoble<Propiedad> tablero;
    private Banco banco;
    private RegistroTransacciones registro;

    // Constructor que inicializa el juego con limite de turnos y la cantidad de jugadores
    public Juego(int maxTurnos, int cantidadJugadores) {
        this(maxTurnos, cantidadJugadores, "local");
    }

    public Juego(int maxTurnos, int cantidadJugadores, String codigoPartida) {
        this.registro = new RegistroTransacciones(codigoPartida);
        this.maxTurnos = maxTurnos;
        this.turnoActualGlobal = 1;
        this.terminado = false;

        this.colaTurnos = new ColaCircularTurnos(cantidadJugadores);
        this.tablero = new ListaCircularDoble<>();
        this.banco = new Banco();
        this.banco.setRegistroTransacciones(registro);
    }

    // Agrega un jugador a la cola circular de turnos
    public void agregarJugador(Jugador jugador) {
        this.colaTurnos.encolar(jugador);
        registro.registrar(jugador, "SALDO INICIAL", "Ingreso a la partida", 0);
    }

    // Asigna el tablero con sus casillas/propiedades enlazadas
    public void setTablero(ListaCircularDoble<Propiedad> tablero) {
        this.tablero = tablero;
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

        int posicionAnterior = actual.getPosicionActual();

        // Si el tablero de propiedades interno no está poblado, usa las 40 casillas del juego
        int tamanoTablero = (tablero != null && tablero.tamaño() > 0) ? tablero.tamaño() : 40;
        actual.mover(totalPasos, tamanoTablero);

        // Si dio la vuelta completa al tablero, cobra la salida en el banco
        if (actual.getPosicionActual() < posicionAnterior) {
            banco.pagarSalida(actual, 200000);
        }
    }

    // Finaliza el turno del jugador activo y avanza la cola circular al siguiente
    public void terminarTurnoActual() {
        colaTurnos.pasarTurno();
        this.turnoActualGlobal++;

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
    public ListaCircularDoble<Propiedad> getTablero() {
        return tablero;
    }

    // Retorna el contador del turno global
    public int getTurnoActualGlobal() {
        return turnoActualGlobal;
    }

    public java.nio.file.Path getArchivoTransacciones() {
        return registro.getArchivo();
    }

    public int getMaxTurnos() {
        return maxTurnos;
    }
}