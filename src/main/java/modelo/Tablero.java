package modelo;

import estructuras.ListaCircularDoble;
import estructuras.Nodo;

/**
 * El tablero: 40 casillas en una lista circular doblemente enlazada,
 * en el mismo orden que el Monopoly clásico.
 *
 * Por qué circular doble: avanzar N casillas son N saltos a getSiguiente()
 * y después de la 39 se vuelve sola a la 0 (Salida) sin ningún "if".
 * Retroceder (cartas de "retrocedé 3") son saltos a getAnterior().
 */
public class Tablero {

    // ListaCircularDoble<Casilla>: la misma lista genérica, pero aquí "T" es Casilla.
    // Como Propiedad, CasillaEvento y CasillaEspecial son Casilla, caben las tres.
    private final ListaCircularDoble<Casilla> casillas = new ListaCircularDoble<>();

    public Tablero() {
        crearCasillas();
    }

    /**
     * Arma las 40 casillas en orden. Para cambiar un nombre o un precio,
     * se edita la línea correspondiente aquí y nada más.
     */
    private void crearCasillas() {
        // Lado inferior (de derecha a izquierda)
        especial("Salida", CasillaEspecial.Tipo.SALIDA, 0);
        propiedad("Sixaola", "Café", 60, 2);
        evento("Arca Comunal", CasillaEvento.Tipo.ARCA_COMUNAL);
        propiedad("Cahuita", "Café", 60, 4);
        especial("Impuesto de renta", CasillaEspecial.Tipo.IMPUESTO, 200);
        propiedad("Aeropuerto Juan Santamaría", "Transporte", 200, 25);
        propiedad("Guápiles", "Celeste", 100, 6);
        evento("Suerte", CasillaEvento.Tipo.SUERTE);
        propiedad("Siquirres", "Celeste", 100, 6);
        propiedad("Puerto Viejo", "Celeste", 120, 8);

        // Lado izquierdo (de abajo hacia arriba)
        especial("Cárcel / De visita", CasillaEspecial.Tipo.CARCEL, 0);
        propiedad("La Fortuna", "Rosado", 140, 10);
        propiedad("ICE", "Servicios", 150, 10);
        propiedad("Ciudad Quesada", "Rosado", 140, 10);
        propiedad("Zarcero", "Rosado", 160, 12);
        propiedad("Puerto Caldera", "Transporte", 200, 25);
        propiedad("Liberia", "Naranja", 180, 14);
        evento("Arca Comunal", CasillaEvento.Tipo.ARCA_COMUNAL);
        propiedad("Nicoya", "Naranja", 180, 14);
        propiedad("Tamarindo", "Naranja", 200, 16);

        // Lado superior (de izquierda a derecha)
        especial("Parqueo libre", CasillaEspecial.Tipo.PARQUEO_LIBRE, 0);
        propiedad("Jacó", "Rojo", 220, 18);
        evento("Suerte", CasillaEvento.Tipo.SUERTE);
        propiedad("Quepos", "Rojo", 220, 18);
        propiedad("Monteverde", "Rojo", 240, 20);
        propiedad("Aeropuerto Daniel Oduber", "Transporte", 200, 25);
        propiedad("Paraíso", "Amarillo", 260, 22);
        propiedad("Tres Ríos", "Amarillo", 260, 22);
        propiedad("AyA", "Servicios", 150, 10);
        propiedad("Turrialba", "Amarillo", 280, 24);

        // Lado derecho (de arriba hacia abajo)
        especial("Vaya a la cárcel", CasillaEspecial.Tipo.IR_A_CARCEL, 0);
        propiedad("Heredia", "Verde", 300, 26);
        propiedad("Belén", "Verde", 300, 26);
        evento("Arca Comunal", CasillaEvento.Tipo.ARCA_COMUNAL);
        propiedad("San Pedro", "Verde", 320, 28);
        propiedad("Puerto Limón", "Transporte", 200, 25);
        evento("Suerte", CasillaEvento.Tipo.SUERTE);
        propiedad("Escazú", "Azul", 350, 35);
        especial("Impuesto de lujo", CasillaEspecial.Tipo.IMPUESTO, 100);
        propiedad("Santa Ana", "Azul", 400, 50);
    }

    // El id de cada casilla es el tamaño de la lista en ese momento (0, 1, 2...).
    // Así el id siempre coincide con la posición y no hay que numerar a mano.

    private void propiedad(String nombre, String grupo, int precio, int alquiler) {
        casillas.agregarAlFinal(new Propiedad(casillas.tamaño(), nombre, grupo, precio, alquiler));
    }

    private void evento(String nombre, CasillaEvento.Tipo tipo) {
        casillas.agregarAlFinal(new CasillaEvento(casillas.tamaño(), nombre, tipo));
    }

    private void especial(String nombre, CasillaEspecial.Tipo tipo, int monto) {
        casillas.agregarAlFinal(new CasillaEspecial(casillas.tamaño(), nombre, tipo, monto));
    }

    /** @param indice posición en el tablero (0 = Salida) */
    public Casilla obtenerCasilla(int indice) {
        return casillas.obtener(indice);
    }

    public int tamaño() {
        return casillas.tamaño();
    }

    /**
     * @return el nodo de Salida. Es el punto de partida para mover fichas:
     *         desde él se avanza con getSiguiente() y se retrocede con getAnterior().
     */
    public Nodo<Casilla> getPrimerNodo() {
        return casillas.getCabeza();
    }

    /**
     * @param indice posición en el tablero (0 = Salida)
     * @return el nodo de esa casilla, para seguir caminando con
     *         getSiguiente() o getAnterior() desde ahí
     */
    public Nodo<Casilla> obtenerNodo(int indice) {
        Nodo<Casilla> actual = casillas.getCabeza();
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual;
    }
}
