package modelo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Mantiene el historial enlazado y su respaldo TXT por partida. */
public final class RegistroTransacciones {
    public static final String COLUMNAS = "Numero | Fecha y hora | Turno | Tipo | Origen | Destino | Monto"
            + " | Descripcion | Saldo origen (anterior -> posterior) | Saldo destino (anterior -> posterior)";
    private final Path archivo;
    private final String codigo;
    private final HistorialTransacciones historial = new HistorialTransacciones();
    private long siguienteNumero = 1;
    private int turno = 1;
    private String errorEscritura;

    public RegistroTransacciones(String codigo) {
        this(codigo, Path.of("transacciones"));
    }

    public RegistroTransacciones(String codigo, Path carpeta) {
        if (codigo == null) throw new IllegalArgumentException("Codigo requerido");
        this.codigo = codigo.replace('\n', ' ').replace('\r', ' ').replace('|', '/');
        try {
            Files.createDirectories(carpeta);
            String fecha = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            // createTempFile crea un nombre unico sin java.util ni sobrescribir otra partida.
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

    public synchronized void setTurno(int turno) {
        if (turno <= 0) throw new IllegalArgumentException("Turno invalido");
        this.turno = turno;
    }

    public synchronized Transaccion registrar(TipoTransaccion tipo, Jugador origen, Jugador destino,
            long monto, String descripcion, int saldoOrigenAnterior, int saldoOrigenPosterior,
            int saldoDestinoAnterior, int saldoDestinoPosterior) {
        Transaccion transaccion = new Transaccion(siguienteNumero, ZonedDateTime.now(), turno, tipo,
                origen == null ? Transaccion.ID_BANCO : origen.getId(),
                origen == null ? "Banco" : origen.getNombre(),
                destino == null ? Transaccion.ID_BANCO : destino.getId(),
                destino == null ? "Banco" : destino.getNombre(), monto, descripcion,
                saldoOrigenAnterior, saldoOrigenPosterior, saldoDestinoAnterior, saldoDestinoPosterior);
        // Se conserva en memoria incluso si falla el disco; se puede reexportar despues.
        historial.agregar(transaccion);
        siguienteNumero++;
        escribir(transaccion.formatear());
        return transaccion;
    }

    public HistorialTransacciones getHistorial() {
        return historial;
    }

    private String encabezado() {
        return "PARTIDA: " + codigo + System.lineSeparator() + "Moneda: colones (CRC)"
                + System.lineSeparator() + COLUMNAS + System.lineSeparator();
    }

    public synchronized String consultar(String jugador, TipoTransaccion tipo, boolean recientesPrimero) {
        return encabezado() + historial.imprimir(jugador, tipo, recientesPrimero);
    }

    /** La exportacion siempre contiene toda la partida en orden cronologico. */
    public synchronized String exportar() {
        return consultar(null, null, false);
    }

    public synchronized void exportar(Path destino) {
        try {
            Files.writeString(destino, exportar(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo exportar el historial", e);
        }
    }

    public synchronized void escribir(String texto) {
        try {
            Files.writeString(archivo, texto + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
            errorEscritura = null;
        } catch (IOException e) {
            // El pago ya se realizo: un error de disco no debe invitar a repetirlo.
            errorEscritura = "No se pudo actualizar el TXT. El historial sigue disponible para exportar: "
                    + e.getMessage();
            System.err.println(errorEscritura);
        }
    }

    public synchronized String getErrorEscritura() {
        return errorEscritura;
    }
}
