package cr.ac.tec.monopoly.cliente;

import cr.ac.tec.monopoly.modelo.Casilla;
import cr.ac.tec.monopoly.modelo.CasillaEspecial;
import cr.ac.tec.monopoly.modelo.CasillaEvento;
import cr.ac.tec.monopoly.modelo.Propiedad;
import cr.ac.tec.monopoly.modelo.Tablero;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
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

    public VentanaTablero() {
        super("Monopoly Costa Rica — Tablero");
        setDefaultCloseOperation(EXIT_ON_CLOSE); // cerrar la ventana termina el programa

        // BorderLayout reparte la ventana en zonas: CENTER crece, EAST tiene ancho fijo
        setLayout(new BorderLayout());
        PanelTablero panelTablero = new PanelTablero(new Tablero());
        add(panelTablero, BorderLayout.CENTER);
        add(crearPanelLateral(), BorderLayout.EAST);

        // Conexión panel -> ventana: cuando el panel detecta un clic en una casilla,
        // llama a mostrarCasilla(casilla). "this::mostrarCasilla" es una referencia
        // al método, equivalente a  casilla -> mostrarCasilla(casilla).
        panelTablero.setAlSeleccionar(this::mostrarCasilla);

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

    public static void main(String[] args) {
        // Swing tiene su propio hilo para la interfaz (el "Event Dispatch Thread").
        // invokeLater le pide a ese hilo que cree la ventana; nunca se crea desde main directo.
        SwingUtilities.invokeLater(() -> new VentanaTablero().setVisible(true));
    }
}
