package modelo;

/**
 * Clase ColaCircularTurnos
 * Cola circular para administrar los turnos de la partida.
 * Rotara secuencialmente el turno entre los 4 jugadores activos.
 */
public class ColaCircularTurnos {

    private Jugador[] jugadores;
    private int frente;
    private int fin;
    private int cantidad;
    private int capacidad;

    // Constructor que define la capacidad de la cola segun el numero de jugadores
    public ColaCircularTurnos(int capacidad) {
        this.capacidad = capacidad;
        this.jugadores = new Jugador[capacidad];
        this.frente = 0;
        this.fin = -1;
        this.cantidad = 0;
    }

    // Agrega un jugador a la cola circular al inicio de la partida
    public boolean encolar(Jugador jugador) {
        if (cantidad == capacidad) {
            return false; // Cola llena
        }
        fin = (fin + 1) % capacidad;
        jugadores[fin] = jugador;
        cantidad++;
        return true;
    }

    // Devuelve el jugador que posee el turno actual sin sacarlo de la cola
    public Jugador obtenerTurnoActual() {
        if (estaVacia()) {
            return null;
        }
        return jugadores[frente];
    }

    // Pasa el turno al siguiente jugador rotando la cola
    public Jugador pasarTurno() {
        if (estaVacia()) {
            return null;
        }

        // Mueve el puntero del frente en forma circular
        frente = (frente + 1) % capacidad;

        // Si el siguiente jugador ya no esta activo (bancarrota), salta al siguiente
        if (!jugadores[frente].isActivo() && contarJugadoresActivos() > 1) {
            return pasarTurno();
        }

        return jugadores[frente];
    }

    // Cuenta cuantos jugadores continúan activos en la partida
    public int contarJugadoresActivos() {
        int activos = 0;
        for (int i = 0; i < cantidad; i++) {
            if (jugadores[i] != null && jugadores[i].isActivo()) {
                activos++;
            }
        }
        return activos;
    }

    // Devuelve el jugador en el orden en que se agrego (0 = el primero).
    // No cambia el turno; sirve para recorrer a todos (cartas de "cada jugador").
    public Jugador obtenerPorIndice(int indice) {
        if (indice < 0 || indice >= cantidad) {
            return null;
        }
        return jugadores[indice];
    }

    // Verifica si la cola no tiene jugadores
    public boolean estaVacia() {
        return cantidad == 0;
    }

    // Retorna la cantidad de jugadores ingresados a la cola
    public int getCantidad() {
        return cantidad;
    }
}