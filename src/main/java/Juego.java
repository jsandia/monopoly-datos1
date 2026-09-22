/**
 * Clase Juego
 * Contiene el estado actual de la partida, detecta los trunos de cada jugador.
 * Define cuanod se acaba la partida segun las condiciones para finalizar un apartida.
 */
public class Juego {

    // --- Atributos de control de partida ---
    private int maxTurnos;
    private int turnoActual;
    private boolean terminado;       // Estado del juego: true si finalizó la partida

    // Agregar referencias a la clase tablero
    // Agregar la cola circular para podeer gestionar los trunos.

    /**
     * Constructor de la clase Juego.
     * Resibe la cantidad de turnos maximos permitidos para la partida
     */
    public Juego(int maxTurnos) {
        this.maxTurnos = maxTurnos;
        this.turnoActual = 1;        // La partida inicia en el turno 1
        this.terminado = false;      // El juego inicia en estado activo
    }

    // --- Control de Turnos ---

    /**
     * Incrementa el contador de turnos global.
     * Evalua si se llega al limite de turnos permitidos por partida.
     */
    public void avanzarTurnoGlobal() {
        this.turnoActual++;
        if (this.turnoActual > this.maxTurnos) {
            this.terminado = true; // Finaliza la partida.
        }
    }

    /**
     * Verifica si la partida finalizo por el limite de trunos.
     * @return true si la partida debe darse por terminada, false si la partida debe continuar
     */
    public boolean esFinDeJuego() {
        return this.terminado;
    }

    // --- Getters y Setters ---

    public int getMaxTurnos() {
        return maxTurnos;
    }

    public int getTurnoActual() {
        return turnoActual;
    }

    public boolean isTerminado() {
        return terminado;
    }

    public void setTerminado(boolean terminado) {
        this.terminado = terminado;
    }
}