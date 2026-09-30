package modelo;

/**
 * Carta de los mazos de Arca Comunal y Suerte.
 *
 * La carta solo DESCRIBE qué pasa (el texto y la acción con su valor); no se
 * aplica sola. Quien la aplica es el servidor (Juego), porque es el único que
 * puede tocar saldos y posiciones. Así la misma carta se puede mostrar en el
 * cliente sin que el cliente pueda "ejecutarla".
 *
 * Los mazos se entregan como arreglos para cargarlos en una ColaCircular:
 * se saca la carta del frente, se aplica, y se vuelve a meter al final.
 * Así "la carta usada pasa al final" y el mazo nunca se acaba.
 */
public class CartaEvento {

    /** Qué hace la carta. El significado de "valor" depende de la acción. */
    public enum Accion {
        /** El banco le paga "valor" al jugador. */
        COBRAR_BANCO,
        /** El jugador le paga "valor" al banco. */
        PAGAR_BANCO,
        /** Cada uno de los otros jugadores le paga "valor" al jugador. */
        COBRAR_A_CADA_JUGADOR,
        /** El jugador le paga "valor" a cada uno de los otros jugadores. */
        PAGAR_A_CADA_JUGADOR,
        /** Avanza hasta la casilla con id "valor". Si pasa por Salida, cobra. */
        IR_A_CASILLA,
        /** Retrocede "valor" casillas (saltos a getAnterior()). No cobra Salida. */
        RETROCEDER,
        /** Avanza hasta el próximo Transporte (aeropuerto o puerto). "valor" no se usa. */
        IR_AL_TRANSPORTE_MAS_CERCANO,
        /** Va directo a la cárcel (casilla "valor") sin pasar por Salida ni cobrar. */
        IR_A_CARCEL,
        /** Sale de la cárcel gratis. "valor" no se usa. */
        SALIR_DE_CARCEL,
        /** Paga "valor" por cada casa y "valorPorHotel" por cada hotel que tenga. */
        REPARACIONES
    }

    // Ids fijos de casillas del Tablero que usan las cartas de IR_A_CASILLA.
    // Si se cambia el orden del Tablero, hay que actualizar estos números.
    private static final int SALIDA = 0;
    private static final int CARCEL = 10;
    private static final int MONTEVERDE = 24;
    private static final int SANTA_ANA = 39;

    private final int id;
    private final CasillaEvento.Tipo mazo;
    private final String texto;
    private final Accion accion;
    private final int valor;
    private final int valorPorHotel; // solo lo usa REPARACIONES; en las demás es 0

    /**
     * @param id            identificador único (Arca Comunal 1-10, Suerte 11-20)
     * @param mazo          a qué mazo pertenece
     * @param texto         lo que se le muestra al jugador
     * @param accion        qué hace la carta
     * @param valor         monto, id de casilla o cantidad de pasos, según la acción
     * @param valorPorHotel monto por hotel (solo REPARACIONES)
     */
    public CartaEvento(int id, CasillaEvento.Tipo mazo, String texto,
                       Accion accion, int valor, int valorPorHotel) {
        this.id = id;
        this.mazo = mazo;
        this.texto = texto;
        this.accion = accion;
        this.valor = valor;
        this.valorPorHotel = valorPorHotel;
    }

    /** Versión corta para todas las cartas que no son de REPARACIONES. */
    public CartaEvento(int id, CasillaEvento.Tipo mazo, String texto, Accion accion, int valor) {
        this(id, mazo, texto, accion, valor, 0);
    }

    // "static": se llama sobre la clase, sin tener una carta creada:
    // CartaEvento.crearMazoArcaComunal(). Es el mismo papel que crearCasillas()
    // en Tablero: todos los textos y montos están en un solo lugar.

    /**
     * Las 10 cartas de Arca Comunal, basadas en las más conocidas del Monopoly original.
     *
     * @return arreglo nuevo con las cartas en orden (se puede barajar antes de encolar)
     */
    public static CartaEvento[] crearMazoArcaComunal() {
        CasillaEvento.Tipo arca = CasillaEvento.Tipo.ARCA_COMUNAL;
        return new CartaEvento[] {
            new CartaEvento(1, arca, "Avance hasta Salida. Cobre ₡200.",
                    Accion.IR_A_CASILLA, SALIDA),
            new CartaEvento(2, arca, "Error del banco a su favor. Cobre ₡200.",
                    Accion.COBRAR_BANCO, 200),
            new CartaEvento(3, arca, "Honorarios del médico. Pague ₡50.",
                    Accion.PAGAR_BANCO, 50),
            new CartaEvento(4, arca, "Por la venta de acciones usted gana ₡50.",
                    Accion.COBRAR_BANCO, 50),
            new CartaEvento(5, arca, "Salga de la cárcel gratis.",
                    Accion.SALIR_DE_CARCEL, 0),
            new CartaEvento(6, arca, "Vaya directo a la cárcel. No pase por Salida ni cobre ₡200.",
                    Accion.IR_A_CARCEL, CARCEL),
            new CartaEvento(7, arca, "Vence su fondo de vacaciones. Cobre ₡100.",
                    Accion.COBRAR_BANCO, 100),
            new CartaEvento(8, arca, "Es su cumpleaños. Cobre ₡10 de cada jugador.",
                    Accion.COBRAR_A_CADA_JUGADOR, 10),
            new CartaEvento(9, arca, "Pague ₡100 de gastos de hospital.",
                    Accion.PAGAR_BANCO, 100),
            new CartaEvento(10, arca, "Reparaciones de calles: pague ₡40 por casa y ₡115 por hotel.",
                    Accion.REPARACIONES, 40, 115),
        };
    }

    /**
     * Las 10 cartas de Suerte (Chance / Fortuna), basadas en las más conocidas
     * del Monopoly original, con los lugares cambiados por los de nuestro tablero.
     *
     * @return arreglo nuevo con las cartas en orden (se puede barajar antes de encolar)
     */
    public static CartaEvento[] crearMazoSuerte() {
        CasillaEvento.Tipo suerte = CasillaEvento.Tipo.SUERTE;
        return new CartaEvento[] {
            new CartaEvento(11, suerte, "Avance hasta Salida. Cobre ₡200.",
                    Accion.IR_A_CASILLA, SALIDA),
            new CartaEvento(12, suerte, "Avance hasta Monteverde. Si pasa por Salida, cobre ₡200.",
                    Accion.IR_A_CASILLA, MONTEVERDE),
            new CartaEvento(13, suerte, "Dé un paseo por Santa Ana. Avance hasta allí.",
                    Accion.IR_A_CASILLA, SANTA_ANA),
            new CartaEvento(14, suerte, "Avance hasta el transporte más cercano. Si pasa por Salida, cobre ₡200.",
                    Accion.IR_AL_TRANSPORTE_MAS_CERCANO, 0),
            new CartaEvento(15, suerte, "El banco le paga un dividendo de ₡50.",
                    Accion.COBRAR_BANCO, 50),
            new CartaEvento(16, suerte, "Salga de la cárcel gratis.",
                    Accion.SALIR_DE_CARCEL, 0),
            new CartaEvento(17, suerte, "Retroceda 3 casillas.",
                    Accion.RETROCEDER, 3),
            new CartaEvento(18, suerte, "Vaya directo a la cárcel. No pase por Salida ni cobre ₡200.",
                    Accion.IR_A_CARCEL, CARCEL),
            new CartaEvento(19, suerte, "Reparaciones generales a sus propiedades: pague ₡25 por casa y ₡100 por hotel.",
                    Accion.REPARACIONES, 25, 100),
            new CartaEvento(20, suerte, "Lo eligieron presidente de la junta directiva. Pague ₡50 a cada jugador.",
                    Accion.PAGAR_A_CADA_JUGADOR, 50),
        };
    }

    public int getId() {
        return id;
    }

    public CasillaEvento.Tipo getMazo() {
        return mazo;
    }

    public String getTexto() {
        return texto;
    }

    public Accion getAccion() {
        return accion;
    }

    public int getValor() {
        return valor;
    }

    public int getValorPorHotel() {
        return valorPorHotel;
    }

    /** @return el texto, que es lo que se muestra en el cliente y en EVENTO|id|texto */
    @Override
    public String toString() {
        return texto;
    }
}
