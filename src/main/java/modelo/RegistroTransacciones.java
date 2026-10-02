package modelo;

import config.Configuracion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Crea las transacciones de una partida: les pone número, fecha y turno,
 * las guarda en el historial (ListaDoble) y las va escribiendo en un TXT
 * propio de la partida, dentro de la carpeta "transacciones".
 */
public final class RegistroTransacciones {

    public static final String COLUMNAS = "Número | Fecha y hora | Turno | Tipo | Origen | Destino | Monto"
            + " | Descripción | Saldo origen (anterior -> posterior) | Saldo destino (anterior -> posterior)";

    private final Path archivo;
    private final String codigo;
    private final HistorialTransacciones historial = new HistorialTransacciones();
    private int siguienteNumero = 1;
    private int turno = 1;
    private String errorEscritura;

    public RegistroTransacciones(String codigo) {
        this(codigo, Path.of(Configuracion.CARPETA_TRANSACCIONES));
    }

    /** @param carpeta dónde se crea el TXT de la partida (las pruebas usan una temporal) */
    public RegistroTransacciones(String codigo, Path carpeta) {
        if (codigo == null) throw new IllegalArgumentException("Código requerido");
        this.codigo = codigo.replace('\n', ' ').replace('\r', ' ').replace('|', '/');
        try {
            Files.createDirectories(carpeta);
            String fecha = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            // createTempFile garantiza un nombre único: nunca sobrescribe otra partida
            archivo = Files.createTempFile(carpeta, "partida_"
                    + codigo.replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + fecha + "_", ".txt");
            Files.writeString(archivo, encabezado(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el historial de la partida", e);
        }
    }

    public Path getArchivo() {
        return archivo.toAbsolutePath();
    }

    public HistorialTransacciones getHistorial() {
        return historial;
    }

    /** Turno con el que se marcan las transacciones siguientes. Lo actualiza Juego. */
    public synchronized void setTurno(int turno) {
        if (turno <= 0) throw new IllegalArgumentException("Turno inválido");
        this.turno = turno;
    }

    /**
     * Crea una transacción, la agrega al historial y la escribe en el TXT.
     * Un jugador en null significa el banco.
     */
    public synchronized void registrar(TipoTransaccion tipo, Jugador origen, Jugador destino,
            int monto, String descripcion, int saldoOrigenAnterior, int saldoOrigenPosterior,
            int saldoDestinoAnterior, int saldoDestinoPosterior) {
        Transaccion transaccion = new Transaccion(siguienteNumero, ZonedDateTime.now(), turno, tipo,
                origen == null ? Transaccion.ID_BANCO : origen.getId(),
                origen == null ? "Banco" : origen.getNombre(),
                destino == null ? Transaccion.ID_BANCO : destino.getId(),
                destino == null ? "Banco" : destino.getNombre(), monto, descripcion,
                saldoOrigenAnterior, saldoOrigenPosterior, saldoDestinoAnterior, saldoDestinoPosterior);
        // Primero la memoria: si después falla el disco, igual se puede exportar
        historial.agregar(transaccion);
        siguienteNumero++;
        escribir(transaccion.formatear());
    }

    /**
     * El historial filtrado como texto, con el encabezado de columnas.
     * Un filtro en null significa "cualquiera".
     */
    public synchronized String consultar(String jugador, TipoTransaccion tipo, boolean recientesPrimero) {
        return encabezado() + historial.imprimir(jugador, tipo, recientesPrimero);
    }

    /** Toda la partida en orden cronológico, para guardarla en un TXT. */
    public synchronized String exportar() {
        return consultar(null, null, false);
    }

    /** Agrega una línea al TXT de la partida. */
    public synchronized void escribir(String texto) {
        try {
            Files.writeString(archivo, texto + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
            errorEscritura = null;
        } catch (IOException e) {
            // El pago ya se hizo: un error de disco no debe hacer que se repita
            errorEscritura = "No se pudo actualizar el TXT. El historial sigue disponible para exportar: "
                    + e.getMessage();
            System.err.println(errorEscritura);
        }
    }

    /** @return el último error al escribir el TXT, o null si todo salió bien */
    public synchronized String getErrorEscritura() {
        return errorEscritura;
    }

    private String encabezado() {
        return "PARTIDA: " + codigo + System.lineSeparator() + "Moneda: colones (CRC)"
                + System.lineSeparator() + COLUMNAS + System.lineSeparator();
    }
}
