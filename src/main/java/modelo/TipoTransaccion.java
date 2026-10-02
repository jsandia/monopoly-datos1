package modelo;

/**
 * Tipos de transacción. Los siete primeros son los mínimos del punto 11 del
 * enunciado; los demás son operaciones propias de este juego.
 */
public enum TipoTransaccion {
    COMPRA_PROPIEDAD, PAGO_ALQUILER, PAGO_AL_BANCO, PAGO_ENTRE_JUGADORES,
    GANANCIA_EVENTO, PERDIDA_EVENTO, PREMIO_INICIO,
    SALDO_INICIAL, COMPRA_CASA, COMPRA_HOTEL
}
