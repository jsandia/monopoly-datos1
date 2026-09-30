package modelo;

import java.time.ZonedDateTime;

/** Una transferencia completa; los datos quedan congelados al efectuar el pago. */
public record Transaccion(
        long numero, ZonedDateTime fechaHora, int turno, TipoTransaccion tipo,
        String idOrigen, String nombreOrigen, String idDestino, String nombreDestino,
        long monto, String descripcion, int saldoOrigenAnterior, int saldoOrigenPosterior,
        int saldoDestinoAnterior, int saldoDestinoPosterior
) {
    public static final String ID_BANCO = "BANCO";

    public Transaccion {
        if (numero <= 0 || turno <= 0 || monto <= 0 || fechaHora == null || tipo == null
                || idOrigen == null || nombreOrigen == null || idDestino == null
                || nombreDestino == null || descripcion == null) {
            throw new IllegalArgumentException("Datos de transaccion invalidos");
        }
    }

    public boolean participaJugador(String identificador) {
        return idOrigen.equals(identificador) || idDestino.equals(identificador);
    }

    public String formatear() {
        return numero + " | " + fechaHora + " | " + turno + " | " + tipo
                + " | " + limpiar(idOrigen + " - " + nombreOrigen)
                + " | " + limpiar(idDestino + " - " + nombreDestino)
                + " | " + monto + " | " + limpiar(descripcion)
                + " | " + saldoOrigenAnterior + " -> " + saldoOrigenPosterior
                + " | " + saldoDestinoAnterior + " -> " + saldoDestinoPosterior;
    }

    private static String limpiar(String texto) {
        return texto.replace('\n', ' ').replace('\r', ' ').replace('|', '/');
    }
}
