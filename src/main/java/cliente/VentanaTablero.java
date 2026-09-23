package cliente;

import modelo.Juego;
import modelo.Jugador;
import modelo.Casilla;
import modelo.CasillaEspecial;
import modelo.CasillaEvento;
import modelo.Propiedad;
import modelo.Tablero;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * La ventana (JFrame) que muestra el tablero. Solo arma las piezas:
 * el PanelTablero al centro y un panel lateral con el detalle de la
 * casilla seleccionada. El dibujo del tablero NO está aquí, está en PanelTablero.
 *
 * Esta ventana es temporal: cuando exista la partida, el PanelTablero se
 * reutiliza dentro de la ventana del juego y esta clase se descarta.
 */
public class VentanaTablero extends JFrame {

    private final JLabel etiquetaNombre = new JLabel("Hacé clic en una casilla");
    private final JLabel etiquetaTipo = new JLabel(" ");
    private final JTextArea areaDescripcion = new JTextArea();

    // --- Atributos de control agregados para el motor del juego ---
    private final JLabel etiquetaTurnoJugador = new JLabel("Turno: -");
    private final JLabel etiquetaSaldoJugador = new JLabel("Saldo: ₡0");
    private final JTextArea areaConsola = new JTextArea();

    private final JButton botonLanzarDados = new JButton("🎲 Lanzar Dados");
    private final JButton botonComprar = new JButton("🏠 Comprar Propiedad");
    private final JButton botonTerminarTurno = new JButton("➡️ Terminar Turno");

    private Juego juego;
    private PanelTablero panelTablero;
    private Tablero modeloTablero;
    private boolean yaLanzados = false;

    public VentanaTablero() {
        this("Jugador 1", "Jugador 2");
    }

    public VentanaTablero(String nombreJugador1, String nombreJugador2) {
        super("Monopoly Costa Rica — Tablero");
        setDefaultCloseOperation(EXIT_ON_CLOSE); // cerrar la ventana termina el programa

        // BorderLayout reparte la ventana en zonas: CENTER crece, EAST tiene ancho fijo
        setLayout(new BorderLayout());

        this.modeloTablero = new Tablero();
        this.juego = new Juego(100, 2);

        juego.agregarJugador(new Jugador("J1", nombreJugador1, 1500000));
        juego.agregarJugador(new Jugador("J2", nombreJugador2, 1500000));

        this.panelTablero = new PanelTablero(modeloTablero);
        add(panelTablero, BorderLayout.CENTER);
        add(crearPanelLateral(), BorderLayout.EAST);

        // Conexión panel -> ventana: cuando el panel detecta un clic en una casilla,
        // llama a mostrarCasilla(casilla). "this::mostrarCasilla" es una referencia
        // al método, equivalente a  casilla -> mostrarCasilla(casilla).
        panelTablero.setAlSeleccionar(this::mostrarCasilla);

        configurarEventosBotones();
        actualizarEstadoJugador();

        pack();                       // ajusta la ventana al tamaño preferido de sus componentes
        setLocationRelativeTo(null);  // centrada en la pantalla
    }

    private JPanel crearPanelLateral() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // apila de arriba hacia abajo
        panel.setPreferredSize(new Dimension(280, 0));
        panel.setBackground(new Color(0xFBF9F3));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));

        etiquetaNombre.setFont(new Font("SansSerif", Font.BOLD, 20));
        etiquetaTipo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        etiquetaTipo.setForeground(new Color(0x666666));

        areaDescripcion.setEditable(false);
        areaDescripcion.setLineWrap(true);
        areaDescripcion.setWrapStyleWord(true);
        areaDescripcion.setFont(new Font("SansSerif", Font.PLAIN, 14));
        areaDescripcion.setBackground(panel.getBackground());
        areaDescripcion.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        panel.add(etiquetaNombre);
        panel.add(etiquetaTipo);
        panel.add(areaDescripcion);

        // Componentes interactivos del juego integrados al panel lateral
        etiquetaTurnoJugador.setFont(new Font("SansSerif", Font.BOLD, 16));
        etiquetaTurnoJugador.setForeground(new Color(0x2C3E50));

        etiquetaSaldoJugador.setFont(new Font("SansSerif", Font.BOLD, 14));
        etiquetaSaldoJugador.setForeground(new Color(0x27AE60));

        areaConsola.setEditable(false);
        areaConsola.setLineWrap(true);
        areaConsola.setWrapStyleWord(true);
        areaConsola.setFont(new Font("Monospaced", Font.PLAIN, 11));
        areaConsola.setBackground(new Color(0xEAEDED));
        areaConsola.setBorder(BorderFactory.createTitledBorder("Registro de Partida"));

        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new BoxLayout(panelBotones, BoxLayout.Y_AXIS));
        panelBotones.setOpaque(false);

        botonLanzarDados.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        botonComprar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        botonTerminarTurno.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        botonComprar.setEnabled(false);
        botonTerminarTurno.setEnabled(false);

        panelBotones.add(botonLanzarDados);
        panelBotones.add(javax.swing.Box.createRigidArea(new Dimension(0, 5)));
        panelBotones.add(botonComprar);
        panelBotones.add(javax.swing.Box.createRigidArea(new Dimension(0, 5)));
        panelBotones.add(botonTerminarTurno);

        panel.add(etiquetaTurnoJugador, 0);
        panel.add(etiquetaSaldoJugador, 1);
        panel.add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));
        panel.add(panelBotones);
        panel.add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));
        panel.add(areaConsola);

        return panel;
    }

    private void mostrarCasilla(Casilla casilla) {
        etiquetaNombre.setText(casilla.getNombre());
        etiquetaTipo.setText("Casilla #" + casilla.getId() + " · " + tipoDe(casilla));
        // getDescripcion() ejecuta la versión de la hija real (polimorfismo)
        areaDescripcion.setText(casilla.getDescripcion());
    }

    private String tipoDe(Casilla casilla) {
        if (casilla instanceof Propiedad p) return "Propiedad · " + p.getGrupo();
        if (casilla instanceof CasillaEvento) return "Evento";
        if (casilla instanceof CasillaEspecial) return "Especial";
        return "";
    }

    private void configurarEventosBotones() {
        botonLanzarDados.addActionListener(e -> {
            if (yaLanzados) return;

            int[] dados = juego.lanzarDadosSoftware();
            int total = dados[0] + dados[1];

            Jugador actual = juego.getJugadorActual();
            juego.ejecutarMovimientoJugadorActual(total);

            registrarLog(actual.getNombre() + " sacó [" + dados[0] + "][" + dados[1] + "] = " + total);

            Casilla casillaActual = modeloTablero.obtenerCasilla(actual.getPosicionActual());
            mostrarCasilla(casillaActual);

            if (casillaActual instanceof Propiedad p && !p.tienePropietario()) {
                botonComprar.setEnabled(true);
            } else {
                botonComprar.setEnabled(false);
            }

            yaLanzados = true;
            botonLanzarDados.setEnabled(false);
            botonTerminarTurno.setEnabled(true);

            actualizarEstadoJugador();
            panelTablero.repaint();
        });

        botonComprar.addActionListener(e -> {
            Jugador actual = juego.getJugadorActual();
            Casilla casillaActual = modeloTablero.obtenerCasilla(actual.getPosicionActual());

            if (casillaActual instanceof Propiedad p) {
                boolean exito = juego.getBanco().comprarPropiedad(actual, p);
                if (exito) {
                    registrarLog("¡" + actual.getNombre() + " compró " + p.getNombre() + "!");
                    botonComprar.setEnabled(false);
                    actualizarEstadoJugador();
                } else {
                    JOptionPane.showMessageDialog(this, "Saldo insuficiente para realizar la compra.", "Fondos Insuficientes", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        botonTerminarTurno.addActionListener(e -> {
            juego.terminarTurnoActual();

            if (juego.esFinDeJuego()) {
                JOptionPane.showMessageDialog(this, "¡La partida ha finalizado!", "Fin de Juego", JOptionPane.INFORMATION_MESSAGE);
                botonLanzarDados.setEnabled(false);
                botonComprar.setEnabled(false);
                botonTerminarTurno.setEnabled(false);
                return;
            }

            yaLanzados = false;
            botonLanzarDados.setEnabled(true);
            botonComprar.setEnabled(false);
            botonTerminarTurno.setEnabled(false);

            Jugador siguiente = juego.getJugadorActual();
            registrarLog("Turno de " + siguiente.getNombre());
            actualizarEstadoJugador();
        });
    }

    private void actualizarEstadoJugador() {
        Jugador actual = juego.getJugadorActual();
        if (actual != null) {
            etiquetaTurnoJugador.setText("Turno: " + actual.getNombre());
            etiquetaSaldoJugador.setText("Saldo: ₡" + actual.getSaldo());
        }
    }

    private void registrarLog(String mensaje) {
        areaConsola.append(mensaje + "\n");
    }

    public static void main(String[] args) {
        // Swing tiene su propio hilo para la interfaz (el "Event Dispatch Thread").
        // invokeLater le pide a ese hilo que cree la ventana; nunca se crea desde main directo.
        SwingUtilities.invokeLater(() -> new VentanaTablero().setVisible(true));
    }
}