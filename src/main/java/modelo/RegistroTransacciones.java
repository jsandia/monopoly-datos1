package modelo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Historial persistente e independiente para cada partida. */
public final class RegistroTransacciones {
    private final Path archivo;

    public RegistroTransacciones(String codigo) {
        try {
            Path carpeta = Path.of("transacciones");
            Files.createDirectories(carpeta);
            String fecha = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            archivo = carpeta.resolve("partida_" + codigo.replaceAll("[^a-zA-Z0-9_-]", "_")
                    + "_" + fecha + "_" + UUID.randomUUID() + ".txt");
            Files.writeString(archivo, "PARTIDA: " + limpiar(codigo) + System.lineSeparator()
                    + "Fecha y hora | Jugador | Operacion | Detalle | Movimiento | Saldo anterior | Saldo posterior"
                    + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el historial de la partida", e);
        }
    }

    public Path getArchivo() {
        return archivo.toAbsolutePath();
    }

    public void registrar(Jugador jugador, String operacion, String detalle, int saldoAnterior) {
        escribir(limpiar(jugador.getId()) + " - " + limpiar(jugador.getNombre()) + " | "
                + limpiar(operacion) + " | " + limpiar(detalle) + " | "
                + ((long) jugador.getSaldo() - saldoAnterior) + " | " + saldoAnterior
                + " | " + jugador.getSaldo());
    }

    public synchronized void escribir(String texto) {
        try {
            Files.writeString(archivo, ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
                    + " | " + texto + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el historial en " + archivo, e);
        }
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.replace('\n', ' ').replace('\r', ' ').replace('|', '/');
    }
}
