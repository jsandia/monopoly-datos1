package cliente;

import modelo.Casilla;
import modelo.CasillaEspecial;
import modelo.CasillaEvento;
import modelo.Propiedad;
import modelo.Tablero;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Dibuja el tablero. Es un JPanel: un rectángulo que se pinta a sí mismo y
 * recibe clics. No sabe nada de la ventana que lo contiene.
 *
 * Cómo se dibuja: Swing llama a paintComponent() cada vez que hay que
 * repintar (al abrir, al cambiar de tamaño, al llamar repaint()). Ahí se
 * calcula la geometría a partir del tamaño actual del panel, por eso el
 * tablero se reescala con la ventana.
 *
 * Las 40 casillas van en el borde de una cuadrícula 11x11: 4 esquinas
 * cuadradas grandes y 9 rectángulos por lado, recorridas contra reloj como
 * en el Monopoly real (Salida abajo a la derecha).
 */
public class PanelTablero extends JPanel {

    private static final Color FONDO = new Color(0xCDE6D0);   // verde clásico del centro
    private static final Color BORDE = new Color(0x1B1B1B);
    private static final Color RESALTADO = new Color(0xFFC107);

    private final Tablero tablero;

    // Al pintar se guarda dónde quedó cada casilla; el clic busca en este arreglo.
    private final Rectangle[] rectangulos = new Rectangle[40];
    private Casilla seleccionada;

    // "Callback": quien use este panel registra aquí qué hacer cuando hay clic
    // en una casilla. Por defecto no hace nada (c -> { }) para evitar un null.
    private Consumer<Casilla> alSeleccionar = c -> { };

    public PanelTablero(Tablero tablero) {
        this.tablero = tablero;
        setPreferredSize(new Dimension(820, 820));
        setBackground(new Color(0xF4F1EA));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = 0; i < 40; i++) {
                    if (rectangulos[i] != null && rectangulos[i].contains(e.getPoint())) {
                        seleccionada = tablero.obtenerCasilla(i);
                        alSeleccionar.accept(seleccionada); // avisar a la ventana
                        repaint();                          // pedir a Swing que repinte
                        return;
                    }
                }
            }
        });
    }

    public void setAlSeleccionar(Consumer<Casilla> alSeleccionar) {
        this.alSeleccionar = alSeleccionar;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // limpia el fondo; sin esto quedan restos del dibujo anterior
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Geometría: el lado del tablero se divide en 2 esquinas + 9 casillas.
        // Una esquina mide 1.5 casillas, así que 2*1.5 + 9 = 12 "anchos".
        int lado = Math.min(getWidth(), getHeight()) - 20;
        double w = lado / 12.0;   // ancho de una casilla de lado
        double c = w * 1.5;       // lado de una esquina
        int x0 = (getWidth() - lado) / 2;
        int y0 = (getHeight() - lado) / 2;

        g2.setColor(FONDO);
        g2.fillRect(x0, y0, lado, lado);
        dibujarCentro(g2, x0, y0, lado, c);

        for (int i = 0; i < 40; i++) {
            rectangulos[i] = rectanguloDe(i, x0, y0, w, c);
            dibujarCasilla(g2, tablero.obtenerCasilla(i), rectangulos[i], ladoDe(i), w);
        }

        g2.setColor(BORDE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(x0, y0, lado, lado);
    }

    /** En qué lado del tablero está la casilla i. Define dónde va la franja de color. */
    private enum Lado { ABAJO, IZQUIERDA, ARRIBA, DERECHA, ESQUINA }

    private Lado ladoDe(int i) {
        if (i % 10 == 0) return Lado.ESQUINA;   // 0, 10, 20, 30
        if (i < 10) return Lado.ABAJO;
        if (i < 20) return Lado.IZQUIERDA;
        if (i < 30) return Lado.ARRIBA;
        return Lado.DERECHA;
    }

    /**
     * Calcula el rectángulo (x, y, ancho, alto) de la casilla i.
     * En pantalla el eje Y crece hacia abajo: y0 es el borde superior.
     */
    private Rectangle rectanguloDe(int i, int x0, int y0, double w, double c) {
        double lejos = c + 9 * w; // distancia desde x0/y0 hasta la esquina opuesta
        double x, y, ancho, alto;
        switch (ladoDe(i)) {
            case ESQUINA -> {
                x = (i == 0 || i == 30) ? x0 + lejos : x0;   // 0 y 30 están a la derecha
                y = (i == 0 || i == 10) ? y0 + lejos : y0;   // 0 y 10 están abajo
                ancho = c; alto = c;
            }
            case ABAJO -> { x = x0 + c + (9 - i) * w; y = y0 + lejos; ancho = w; alto = c; }
            case IZQUIERDA -> { x = x0; y = y0 + c + (19 - i) * w; ancho = c; alto = w; }
            case ARRIBA -> { x = x0 + c + (i - 21) * w; y = y0; ancho = w; alto = c; }
            default -> { x = x0 + lejos; y = y0 + c + (i - 31) * w; ancho = c; alto = w; } // DERECHA
        }
        return new Rectangle((int) Math.round(x), (int) Math.round(y),
                (int) Math.round(ancho), (int) Math.round(alto));
    }

    private void dibujarCasilla(Graphics2D g2, Casilla casilla, Rectangle r, Lado lado, double w) {
        Rectangle areaTexto = new Rectangle(r); // copia: se va recortando conforme se dibuja

        g2.setColor(new Color(0xFBF9F3));
        g2.fill(r);

        // "instanceof con variable": si casilla es Propiedad, queda en "p" ya convertida.
        // Es la forma de preguntar por el tipo real de una Casilla.
        if (casilla instanceof Propiedad p) {
            // Franja de color en el borde interior (el que mira al centro del tablero)
            int grosor = (int) (w * 0.28);
            g2.setColor(colorDeGrupo(p.getGrupo()));
            switch (lado) {
                case ABAJO -> { g2.fillRect(r.x, r.y, r.width, grosor); areaTexto.y += grosor; areaTexto.height -= grosor; }
                case ARRIBA -> { g2.fillRect(r.x, r.y + r.height - grosor, r.width, grosor); areaTexto.height -= grosor; }
                case IZQUIERDA -> { g2.fillRect(r.x + r.width - grosor, r.y, grosor, r.height); areaTexto.width -= grosor; }
                case DERECHA -> { g2.fillRect(r.x, r.y, grosor, r.height); areaTexto.x += grosor; areaTexto.width -= grosor; }
                default -> { }
            }
        } else if (casilla instanceof CasillaEvento ev) {
            g2.setColor(ev.getTipo() == CasillaEvento.Tipo.SUERTE ? new Color(0xFFE8C2) : new Color(0xD6ECFA));
            g2.fill(r);
        } else if (casilla instanceof CasillaEspecial esp && esp.getTipo() == CasillaEspecial.Tipo.SALIDA) {
            g2.setColor(new Color(0xFFD9D9));
            g2.fill(r);
        }

        g2.setColor(BORDE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(r);

        if (casilla == seleccionada) {
            g2.setColor(RESALTADO);
            g2.setStroke(new BasicStroke(4));
            g2.drawRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
        }

        // Textos: el tamaño de letra es proporcional a la casilla para que escale
        float base = (float) (lado == Lado.ESQUINA ? w * 0.22 : w * 0.16);
        Font fuenteNombre = new Font("SansSerif", Font.BOLD, Math.round(base));
        Font fuenteDetalle = new Font("SansSerif", Font.PLAIN, Math.round(base * 0.9f));
        if (casilla instanceof CasillaEvento) {
            fuenteDetalle = new Font("SansSerif", Font.BOLD, Math.round(base * 2.2f)); // "?" grande
        }
        String detalle = detalleDe(casilla);

        areaTexto.grow(-4, -4);
        if (detalle.isEmpty()) {
            dibujarTexto(g2, casilla.getNombre().toUpperCase(), areaTexto, fuenteNombre, Color.BLACK);
        } else {
            // Nombre arriba, detalle abajo
            int altoNombre = (casilla instanceof CasillaEvento) ? areaTexto.height / 2 : areaTexto.height * 2 / 3;
            Rectangle arriba = new Rectangle(areaTexto.x, areaTexto.y, areaTexto.width, altoNombre);
            Rectangle abajo = new Rectangle(areaTexto.x, areaTexto.y + altoNombre, areaTexto.width, areaTexto.height - altoNombre);
            dibujarTexto(g2, casilla.getNombre().toUpperCase(), arriba, fuenteNombre, Color.BLACK);
            dibujarTexto(g2, detalle, abajo, fuenteDetalle, new Color(0x444444));
        }
    }

    /** Segunda línea de la casilla: precio, símbolo o monto. Vacío si no aplica. */
    private String detalleDe(Casilla casilla) {
        if (casilla instanceof Propiedad p) return "₡" + p.getPrecio();
        if (casilla instanceof CasillaEvento ev) return ev.getTipo() == CasillaEvento.Tipo.SUERTE ? "?" : "▣";
        if (casilla instanceof CasillaEspecial esp) {
            return switch (esp.getTipo()) {
                case IMPUESTO -> "Pagá ₡" + esp.getMonto();
                case SALIDA -> "Cobrá ₡200";
                default -> "";
            };
        }
        return "";
    }

    /**
     * Dibuja texto centrado dentro del rectángulo. Parte el texto en líneas
     * por palabras para que quepa a lo ancho, y si una palabra sola no cabe
     * (ej. "AEROPUERTO" en una casilla angosta) achica la fuente hasta que quepa.
     */
    private void dibujarTexto(Graphics2D g2, String texto, Rectangle area, Font fuente, Color color) {
        String[] palabras = texto.split(" ");

        FontMetrics fm = g2.getFontMetrics(fuente); // FontMetrics mide cuántos píxeles ocupa un texto
        while (fuente.getSize() > 7 && palabraMasAncha(fm, palabras) > area.width) {
            fuente = fuente.deriveFont((float) fuente.getSize() - 1);
            fm = g2.getFontMetrics(fuente);
        }
        g2.setFont(fuente);
        g2.setColor(color);

        // Armar líneas: se agregan palabras mientras quepan en area.width
        StringBuilder lineas = new StringBuilder();
        StringBuilder actual = new StringBuilder();
        for (String palabra : palabras) {
            String prueba = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (fm.stringWidth(prueba) <= area.width || actual.isEmpty()) {
                actual = new StringBuilder(prueba);
            } else {
                lineas.append(actual).append('\n');
                actual = new StringBuilder(palabra);
            }
        }
        lineas.append(actual);

        // Centrar el bloque de líneas vertical y horizontalmente.
        // drawString usa la "línea base" del texto, por eso se suma el ascent.
        String[] filas = lineas.toString().split("\n");
        int altoTotal = filas.length * fm.getHeight();
        int y = area.y + (area.height - altoTotal) / 2 + fm.getAscent();
        for (String fila : filas) {
            int x = area.x + (area.width - fm.stringWidth(fila)) / 2;
            g2.drawString(fila, x, y);
            y += fm.getHeight();
        }
    }

    private int palabraMasAncha(FontMetrics fm, String[] palabras) {
        int max = 0;
        for (String palabra : palabras) {
            max = Math.max(max, fm.stringWidth(palabra));
        }
        return max;
    }

    /** Letrero central: "MONOPOLY" en rojo y "COSTA RICA" en café. */
    private void dibujarCentro(Graphics2D g2, int x0, int y0, int lado, double c) {
        int cx = x0 + lado / 2;
        int cy = y0 + lado / 2;
        int anchoBanner = (int) (lado - 2 * c - 40);
        int altoBanner = (int) (lado * 0.11);

        g2.setColor(new Color(0xE0201B));
        g2.fillRoundRect(cx - anchoBanner / 2, cy - altoBanner, anchoBanner, altoBanner, 18, 18);
        dibujarTexto(g2, "MONOPOLY",
                new Rectangle(cx - anchoBanner / 2, cy - altoBanner, anchoBanner, altoBanner),
                new Font("SansSerif", Font.BOLD, (int) (altoBanner * 0.62)), Color.WHITE);

        int altoSub = (int) (altoBanner * 0.55);
        int anchoSub = anchoBanner / 2;
        g2.setColor(new Color(0x7A4B1F));
        g2.fillRoundRect(cx - anchoSub / 2, cy + 8, anchoSub, altoSub, 12, 12);
        dibujarTexto(g2, "COSTA RICA",
                new Rectangle(cx - anchoSub / 2, cy + 8, anchoSub, altoSub),
                new Font("SansSerif", Font.BOLD, (int) (altoSub * 0.55)), new Color(0xFFF3D6));
    }

    /**
     * Color de la franja según el grupo. Vive aquí y no en Propiedad porque
     * el color es presentación: el modelo no debe saber de colores ni de Swing.
     */
    private static Color colorDeGrupo(String grupo) {
        return switch (grupo) {
            case "Café" -> new Color(0x8B4513);
            case "Celeste" -> new Color(0xAAE0FA);
            case "Rosado" -> new Color(0xD93A96);
            case "Naranja" -> new Color(0xF7941D);
            case "Rojo" -> new Color(0xED1B24);
            case "Amarillo" -> new Color(0xFEF200);
            case "Verde" -> new Color(0x1FB25A);
            case "Azul" -> new Color(0x0072BB);
            case "Transporte" -> new Color(0x333333);
            case "Servicios" -> new Color(0xBDBDBD);
            default -> Color.LIGHT_GRAY;
        };
    }
}
