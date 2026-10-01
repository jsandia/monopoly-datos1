package cliente;

import modelo.Casilla;
import modelo.CasillaEspecial;
import modelo.CasillaEvento;
import modelo.Propiedad;
import modelo.Tablero;
import protocolo.Message;

import javax.swing.JPanel;
import javax.swing.Timer;
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
    private Message.EstadoPropiedad[] propiedades = new Message.EstadoPropiedad[40];

    public void actualizarPropiedades(Message.EstadoPropiedad[] propiedades) {
        this.propiedades = propiedades.clone();
        repaint();
    }

    // Al pintar se guarda dónde quedó cada casilla; el clic busca en este arreglo.
    private final Rectangle[] rectangulos = new Rectangle[40];
    private Casilla seleccionada;

    // Estado de los dados dibujados sobre el tablero (ver mostrarDados/ocultarDados)
    private int dado1 = 1;
    private int dado2 = 1;
    private boolean mostrandoDados = false;

    // =========================================================
    // FICHAS DE LOS JUGADORES
    // =========================================================

    private static final int CANTIDAD_JUGADORES = 4;
    private static final int PASO_FICHA_MS = 230;   // tiempo que tarda la ficha en saltar de una casilla a la siguiente
    private static final int MAX_PASOS_ANIMADOS = 12; // más que un tiro de dados: es un salto (ej. "Vaya a la cárcel")

    private static final Color[] COLORES_FICHAS = {
            new Color(0xE53935), // jugador 1: rojo
            new Color(0x1E88E5), // jugador 2: azul
            new Color(0x8E24AA), // jugador 3: morado
            new Color(0x00897B)  // jugador 4: turquesa
    };

    // Casilla donde se DIBUJA cada ficha ahora mismo (-1 = todavía no se sabe)
    private final int[] posicionVisible = {-1, -1, -1, -1};
    // Casilla donde el servidor dice que está cada jugador
    private final int[] posicionDestino = new int[CANTIDAD_JUGADORES];
    private final String[] nombresJugadores = {"1", "2", "3", "4"};
    private int jugadorEnTurno = -1;
    private Timer temporizadorFichas; // != null mientras alguna ficha está caminando

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

    /**
     * Actualiza los números de los dados y hace que se dibujen sobre el
     * tablero (centro). Quien llama a esto todavía tiene que pedir
     * repaint() para que el cambio se vea en pantalla.
     */
    public void mostrarDados(int dado1, int dado2) {
        this.dado1 = dado1;
        this.dado2 = dado2;
        this.mostrandoDados = true;
    }

    /**
     * Le dice al panel dónde está cada jugador según el servidor. Si una ficha
     * está lejos de su destino, camina casilla por casilla (como en el juego
     * real) con un Timer. La primera vez, o si el salto es mayor que lo que
     * pueden avanzar los dados (ir a la cárcel, cartas...), aparece directo.
     */
    public void actualizarFichas(int[] posiciones, String[] nombres, int enTurno) {
        jugadorEnTurno = enTurno;

        for (int i = 0; i < CANTIDAD_JUGADORES; i++) {
            if (nombres != null && nombres[i] != null && !nombres[i].isBlank()) {
                nombresJugadores[i] = nombres[i].trim();
            }

            posicionDestino[i] = posiciones[i];

            if (posicionVisible[i] < 0) {
                posicionVisible[i] = posiciones[i]; // primera vez: sin animación
            } else {
                int pasos = Math.floorMod(posicionDestino[i] - posicionVisible[i], 40);
                if (pasos > MAX_PASOS_ANIMADOS) {
                    posicionVisible[i] = posicionDestino[i];
                }
            }
        }

        if (hayFichaCaminando() && temporizadorFichas == null) {
            temporizadorFichas = new Timer(PASO_FICHA_MS, e -> {
                for (int i = 0; i < CANTIDAD_JUGADORES; i++) {
                    if (posicionVisible[i] != posicionDestino[i]) {
                        posicionVisible[i] = (posicionVisible[i] + 1) % 40; // una casilla hacia adelante
                    }
                }
                if (!hayFichaCaminando()) {
                    temporizadorFichas.stop();
                    temporizadorFichas = null;
                }
                repaint();
            });
            temporizadorFichas.start();
        }

        repaint();
    }

    private boolean hayFichaCaminando() {
        for (int i = 0; i < CANTIDAD_JUGADORES; i++) {
            if (posicionVisible[i] != posicionDestino[i]) return true;
        }
        return false;
    }

    /** Deja de dibujar los dados (por ejemplo, al empezar el turno de otro jugador). */
    public void ocultarDados() {
        this.mostrandoDados = false;
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

        if (mostrandoDados) {
            dibujarDados(g2, x0, y0, lado, c);
        }

        for (int i = 0; i < 40; i++) {
            rectangulos[i] = rectanguloDe(i, x0, y0, w, c);
            dibujarCasilla(g2, tablero.obtenerCasilla(i), rectangulos[i], ladoDe(i), w);
        }

        g2.setColor(BORDE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(x0, y0, lado, lado);

        // Las fichas van al final para quedar encima de las casillas
        dibujarFichas(g2, w);
    }

    /**
     * Dibuja las fichas. Los jugadores que están en la misma casilla se
     * reparten en una mini-cuadrícula (máximo 2 por fila) centrada en la
     * casilla, así nunca quedan una encima de otra.
     */
    private void dibujarFichas(Graphics2D g2, double w) {
        int diametro = (int) Math.max(14, w * 0.42);
        int separacion = (int) (diametro * 1.1);

        for (int casilla = 0; casilla < 40; casilla++) {
            if (rectangulos[casilla] == null) continue;

            // Jugadores (por índice) que están dibujados en esta casilla
            int[] aqui = new int[CANTIDAD_JUGADORES];
            int cantidad = 0;
            for (int j = 0; j < CANTIDAD_JUGADORES; j++) {
                if (posicionVisible[j] == casilla) aqui[cantidad++] = j;
            }
            if (cantidad == 0) continue;

            Rectangle r = rectangulos[casilla];
            int columnas = (cantidad == 1) ? 1 : 2;
            int filas = (cantidad + columnas - 1) / columnas;
            int centroX = r.x + r.width / 2;
            int centroY = r.y + r.height / 2;

            for (int k = 0; k < cantidad; k++) {
                int fila = k / columnas;
                int columna = k % columnas;
                // La última fila puede tener menos fichas: se centra
                int enEstaFila = Math.min(columnas, cantidad - fila * columnas);

                int cx = centroX + (int) ((columna - (enEstaFila - 1) / 2.0) * separacion);
                int cy = centroY + (int) ((fila - (filas - 1) / 2.0) * separacion);

                dibujarFicha(g2, cx, cy, diametro, aqui[k]);
            }
        }
    }

    /** Una ficha: círculo de color con sombra, borde blanco y la inicial del jugador. */
    private void dibujarFicha(Graphics2D g2, int cx, int cy, int diametro, int jugador) {
        int x = cx - diametro / 2;
        int y = cy - diametro / 2;

        // Sombra
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(x + 2, y + 3, diametro, diametro);

        // Cuerpo
        g2.setColor(COLORES_FICHAS[jugador]);
        g2.fillOval(x, y, diametro, diametro);

        // Borde: dorado y más grueso si es el jugador en turno
        boolean enTurno = (jugador == jugadorEnTurno);
        g2.setStroke(new BasicStroke(enTurno ? 3f : 2f));
        g2.setColor(enTurno ? RESALTADO : Color.WHITE);
        g2.drawOval(x, y, diametro, diametro);
        g2.setColor(BORDE);
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(x - 1, y - 1, diametro + 2, diametro + 2);

        // Inicial del nombre
        String inicial = nombresJugadores[jugador].substring(0, 1).toUpperCase();
        Font fuente = new Font("SansSerif", Font.BOLD, Math.max(9, (int) (diametro * 0.55)));
        g2.setFont(fuente);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString(inicial,
                cx - fm.stringWidth(inicial) / 2,
                cy + (fm.getAscent() - fm.getDescent()) / 2);
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
            dibujarConstrucciones(g2, p, r, lado, grosor);
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

    /** Casas verdes sobre la franja de color, orientadas hacia el centro del tablero. */
    private void dibujarConstrucciones(Graphics2D g2, Propiedad propiedad, Rectangle r, Lado lado, int grosor) {
        Message.EstadoPropiedad estado = propiedades[propiedad.getId()];
        int casas = estado == null ? propiedad.getCantidadCasas() : estado.casas();
        boolean hotel = estado == null ? propiedad.tieneHotel() : estado.hotel();
        if (casas == 0 && !hotel) return;
        Graphics2D dibujo = (Graphics2D) g2.create();
        try {
            switch (lado) {
                case ABAJO -> dibujo.translate(r.getCenterX(), r.y + grosor / 2.0);
                case ARRIBA -> { dibujo.translate(r.getCenterX(), r.y + r.height - grosor / 2.0); dibujo.rotate(Math.PI); }
                case IZQUIERDA -> { dibujo.translate(r.x + r.width - grosor / 2.0, r.getCenterY()); dibujo.rotate(Math.PI / 2); }
                case DERECHA -> { dibujo.translate(r.x + grosor / 2.0, r.getCenterY()); dibujo.rotate(-Math.PI / 2); }
                default -> { return; }
            }
            int anchoFranja = lado == Lado.ABAJO || lado == Lado.ARRIBA ? r.width : r.height;
            int tamano = Math.max(3, Math.min(grosor - 3, (anchoFranja - 8) / 4 - 2));
            int cantidad = hotel ? 1 : casas;
            int ancho = hotel ? tamano * 2 : tamano;
            int total = cantidad * ancho + (cantidad - 1) * 3;
            dibujo.setStroke(new BasicStroke(1f));
            for (int i = 0; i < cantidad; i++) {
                int x = -total / 2 + i * (ancho + 3);
                int y = -tamano / 2;
                java.awt.Polygon silueta = new java.awt.Polygon(
                        new int[]{x, x + ancho / 2, x + ancho, x + ancho, x},
                        new int[]{y + tamano / 3, y, y + tamano / 3, y + tamano, y + tamano}, 5);
                dibujo.setColor(Color.WHITE);
                dibujo.draw(silueta);
                dibujo.setColor(new Color(hotel ? 0xD32F2F : 0x008F39));
                dibujo.fill(silueta);
                dibujo.setColor(new Color(0x163A22));
                dibujo.draw(silueta);
                dibujo.setColor(Color.WHITE);
                dibujo.fillRect(x + ancho / 2 - 1, y + tamano * 2 / 3, 2, Math.max(2, tamano / 3));
            }
        } finally {
            dibujo.dispose();
        }
    }

    /** Segunda línea de la casilla: precio, renta, símbolo o monto. */
    private String detalleDe(Casilla casilla) {
        if (casilla instanceof Propiedad p) {
            Message.EstadoPropiedad estado = propiedades[p.getId()];
            return estado != null && estado.propietario() >= 0 ? "Renta ₡" + estado.renta() : "₡" + p.getPrecio();
        }
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
     * Dibuja los 2 dados justo debajo del cartel "COSTA RICA", en el
     * centro del tablero. Se llama solo cuando mostrandoDados es true
     * (ver mostrarDados/ocultarDados).
     */
    private void dibujarDados(Graphics2D g2, int x0, int y0, int lado, double c) {
        int cx = x0 + lado / 2;
        int cy = y0 + lado / 2;
        int altoBanner = (int) (lado * 0.11);
        int altoSub = (int) (altoBanner * 0.55);

        int tamanoDado = (int) (altoBanner * 0.85);
        int espacio = (int) (tamanoDado * 0.35);
        int anchoTotal = tamanoDado * 2 + espacio;

        int dadosY = cy + 8 + altoSub + 12;
        int dado1X = cx - anchoTotal / 2;
        int dado2X = dado1X + tamanoDado + espacio;

        dibujarUnDado(g2, dado1X, dadosY, tamanoDado, dado1);
        dibujarUnDado(g2, dado2X, dadosY, tamanoDado, dado2);
    }

    /** Dibuja un solo dado (cuadrado blanco con puntos negros) en la posición dada. */
    private void dibujarUnDado(Graphics2D g2, int x, int y, int tamano, int valor) {
        int radioEsquina = tamano / 5;

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, tamano, tamano, radioEsquina, radioEsquina);
        g2.setColor(BORDE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, tamano, tamano, radioEsquina, radioEsquina);

        int pad = tamano / 5;
        int izq = x + pad;
        int der = x + tamano - pad;
        int arriba = y + pad;
        int abajo = y + tamano - pad;
        int medioX = x + tamano / 2;
        int medioY = y + tamano / 2;
        int radioPunto = Math.max(3, tamano / 9);

        g2.setColor(new Color(0x1B1B1B));

        // Patrón de puntos clásico de un dado, según el valor (1 a 6)
        switch (valor) {
            case 1 -> dibujarPunto(g2, medioX, medioY, radioPunto);
            case 2 -> {
                dibujarPunto(g2, izq, arriba, radioPunto);
                dibujarPunto(g2, der, abajo, radioPunto);
            }
            case 3 -> {
                dibujarPunto(g2, izq, arriba, radioPunto);
                dibujarPunto(g2, medioX, medioY, radioPunto);
                dibujarPunto(g2, der, abajo, radioPunto);
            }
            case 4 -> {
                dibujarPunto(g2, izq, arriba, radioPunto);
                dibujarPunto(g2, der, arriba, radioPunto);
                dibujarPunto(g2, izq, abajo, radioPunto);
                dibujarPunto(g2, der, abajo, radioPunto);
            }
            case 5 -> {
                dibujarPunto(g2, izq, arriba, radioPunto);
                dibujarPunto(g2, der, arriba, radioPunto);
                dibujarPunto(g2, medioX, medioY, radioPunto);
                dibujarPunto(g2, izq, abajo, radioPunto);
                dibujarPunto(g2, der, abajo, radioPunto);
            }
            case 6 -> {
                dibujarPunto(g2, izq, arriba, radioPunto);
                dibujarPunto(g2, der, arriba, radioPunto);
                dibujarPunto(g2, izq, medioY, radioPunto);
                dibujarPunto(g2, der, medioY, radioPunto);
                dibujarPunto(g2, izq, abajo, radioPunto);
                dibujarPunto(g2, der, abajo, radioPunto);
            }
            default -> { } // no debería pasar (los dados van de 1 a 6)
        }
    }

    private void dibujarPunto(Graphics2D g2, int cx, int cy, int radio) {
        g2.fillOval(cx - radio, cy - radio, radio * 2, radio * 2);
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
