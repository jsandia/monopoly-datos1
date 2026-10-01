package cliente;

import modelo.Propiedad;
import modelo.Tablero;
import protocolo.Mensaje;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Genera una imagen de demostración, sin servidor ni una partida real. */
public final class PreviewHoteles {
    public static void main(String[] args) throws Exception {
        Path destino = Path.of("docs", "imagenes", "preview-hoteles.png");
        Files.createDirectories(destino.getParent());
        SwingUtilities.invokeAndWait(() -> {
            try {
                Tablero tablero = new Tablero();
                PanelTablero panel = new PanelTablero(tablero);
                panel.setSize(1000, 1000);
                Mensaje.EstadoPropiedad[] propiedades = new Mensaje.EstadoPropiedad[tablero.tamaño()];
                for (int id : new int[]{1, 11, 21, 31, 3}) {
                    Propiedad propiedad = (Propiedad) tablero.obtenerCasilla(id);
                    for (int casa = 0; casa < 4; casa++) propiedad.agregarCasa();
                    if (id != 3) propiedad.construirHotel();
                    propiedades[id] = new Mensaje.EstadoPropiedad(id, 0, propiedad.getCantidadCasas(),
                            propiedad.tieneHotel(), false, propiedad.calcularAlquiler(), propiedad.getPrecioCasa(),
                            "Máximo de casas o hotel construido.", propiedad.getPrecioHotel(),
                            propiedad.tieneHotel() ? "Ya tiene hotel." : null);
                }
                panel.actualizarPropiedades(propiedades);
                BufferedImage imagen = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
                Graphics2D dibujo = imagen.createGraphics();
                try {
                    panel.paint(dibujo);
                } finally {
                    dibujo.dispose();
                }
                ImageIO.write(imagen, "png", destino.toFile());
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo generar el preview de hoteles.", e);
            }
        });
        System.out.println("Preview guardado en " + destino.toAbsolutePath());
    }
}
