package modelo;

import java.time.ZonedDateTime;

/**
 * Una operación de dinero ya hecha. Es un record: sus datos no cambian
 * después de crearla, así el historial no se puede alterar.
 *
 * Además de lo que pide el enunciado (número, fecha y hora, turno, tipo,
 * origen, destino, monto y descripción) guarda el saldo de los dos
 * participantes antes y después, para poder revisar cada pago.
 * El banco aparece con el id {@link #ID_BANCO}.
 */
public record Transaccion(
        int numero, ZonedDateTime fechaHora, int turno, TipoTransaccion tipo,
        String idOrigen, String nombreOrigen, String idDestino, String nombreDestino,
        int monto, String descripcion, int saldoOrigenAnterior, int saldoOrigenPosterior,
        int saldoDestinoAnterior, int saldoDestinoPosterior
) {
    public static final String ID_BANCO = "BANCO";

    /** Constructor compacto: valida los datos antes de crear la transacción. */
    public Transaccion {
        if (numero <= 0 || turno <= 0 || monto <= 0 || fechaHora == null || tipo == null
                || idOrigen == null || nombreOrigen == null || idDestino == null
                || nombreDestino == null || descripcion == null) {
            throw new IllegalArgumentException("Datos de transacción inválidos");
        }
    }

    /** @return true si el jugador es el origen o el destino */
    public boolean participaJugador(String identificador) {
        return idOrigen.equals(identificador) || idDestino.equals(identificador);
    }

    /** @return una línea del TXT, con las columnas de RegistroTransacciones.COLUMNAS */
    public String formatear() {
        return numero + " | " + fechaHora + " | " + turno + " | " + tipo
                + " | " + limpiar(idOrigen + " - " + nombreOrigen)
                + " | " + limpiar(idDestino + " - " + nombreDestino)
                + " | " + monto + " | " + limpiar(descripcion)
                + " | " + saldoOrigenAnterior + " -> " + saldoOrigenPosterior
                + " | " + saldoDestinoAnterior + " -> " + saldoDestinoPosterior;
    }

    // Un salto de línea o un "|" dentro de un texto rompería las columnas del TXT
    private static String limpiar(String texto) {
        return texto.replace('\n', ' ').replace('\r', ' ').replace('|', '/');
    }
}
