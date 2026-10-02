package modelo;

import java.util.Random;

/**
 * Un dado de seis caras del juego.
 *
 * El número puede venir de dos lugares, y en los dos casos queda guardado
 * aquí como la última cara que salió:
 * <ul>
 *   <li>del dado físico: la Pico 2W tira, manda "DADOS;4;3" y el servidor
 *       lo guarda con {@link #registrarValor(int)};</li>
 *   <li>de la computadora: el botón de "dados de prueba" usa
 *       {@link #lanzar()}.</li>
 * </ul>
 *
 * La regla de qué cara es válida está en un solo lugar,
 * {@link #esValido(int)}, y la usan el servidor (para lo que llega del
 * cajero) y esta misma clase.
 *
 * Nota: usa java.util.Random, que el proyecto permite solo para los dados.
 */
public class Dado {

    /** Cuántas caras tiene: los valores van de 1 a CARAS. */
    public static final int CARAS = 6;

    /** Generador de números al azar. Se recibe en el constructor para poder probar con semilla. */
    private final Random azar;

    /** La última cara que salió. 0 significa que todavía no se ha tirado. */
    private int valor;

    /** Crea un dado con azar normal. */
    public Dado() {
        this(new Random());
    }

    /**
     * Crea un dado con un generador dado. Con {@code new Random(semilla)}
     * siempre sale la misma secuencia: sirve para que las pruebas sean repetibles.
     *
     * @param azar el generador de números al azar
     */
    public Dado(Random azar) {
        this.azar = azar;
        this.valor = 0;
    }

    /**
     * Tira el dado en la computadora.
     *
     * {@code nextInt(CARAS)} da un número de 0 a 5; sumándole 1 queda de 1 a 6.
     *
     * @return la cara que salió, de 1 a 6
     */
    public int lanzar() {
        valor = azar.nextInt(CARAS) + 1;
        return valor;
    }

    /**
     * Guarda la cara que salió en el dado físico del cajero.
     *
     * @param cara el valor que mandó el cajero
     * @throws IllegalArgumentException si la cara no está entre 1 y 6
     */
    public void registrarValor(int cara) {
        if (!esValido(cara)) {
            throw new IllegalArgumentException("Un dado no puede sacar " + cara);
        }
        valor = cara;
    }

    /** @return la última cara que salió (0 si todavía no se ha tirado) */
    public int getValor() {
        return valor;
    }

    /**
     * "static": es una regla de todos los dados, no de uno en particular.
     * Se llama como {@code Dado.esValido(4)}, sin crear un dado.
     *
     * @param cara el valor a revisar
     * @return true si un dado de seis caras puede sacar ese valor
     */
    public static boolean esValido(int cara) {
        return cara >= 1 && cara <= CARAS;
    }
}
