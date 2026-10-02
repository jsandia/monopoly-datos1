package cliente;

import modelo.TipoTransaccion;
import protocolo.Mensaje;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Consulta el historial de transacciones del servidor, con filtros por jugador
 * y por tipo, en los dos órdenes, y lo exporta a TXT. El cliente solo muestra
 * y guarda el texto que le manda el servidor: el historial oficial vive allá.
 */
public final class VentanaHistorial extends JDialog {
    private final JComboBox<String> jugador = new JComboBox<>();
    private final JComboBox<String> tipo = new JComboBox<>();
    private final JComboBox<String> orden = new JComboBox<>(new String[]{"Más antigua primero", "Más reciente primero"});
    private final JTextArea texto = new JTextArea();
    private final Consumer<Mensaje> enviar;

    public VentanaHistorial(JFrame ventana, String[] nombres, Consumer<Mensaje> enviar) {
        super(ventana, "Historial de transacciones", false);
        this.enviar = enviar;
        jugador.addItem("Todos");
        jugador.addItem("Banco");
        for (int i = 0; i < nombres.length; i++) jugador.addItem("J" + (i + 1) + " - " + nombres[i]);
        tipo.addItem("Todos");
        for (TipoTransaccion valor : TipoTransaccion.values()) tipo.addItem(valor.name());

        JPanel controles = new JPanel();
        controles.add(new JLabel("Jugador:"));
        controles.add(jugador);
        controles.add(new JLabel("Tipo:"));
        controles.add(tipo);
        controles.add(orden);
        JButton consultar = new JButton("Consultar");
        consultar.addActionListener(evento -> consultar());
        controles.add(consultar);

        JButton exportar = new JButton("Exportar toda la partida a TXT");
        exportar.addActionListener(evento -> enviar.accept(new Mensaje.ExportarTransacciones()));
        texto.setEditable(false);
        texto.setFont(new Font("Monospaced", Font.PLAIN, 12));
        setLayout(new BorderLayout());
        add(controles, BorderLayout.NORTH);
        add(new JScrollPane(texto), BorderLayout.CENTER);
        add(exportar, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(1100, 500));
        pack();
        setLocationRelativeTo(ventana);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void consultar() {
        int indice = jugador.getSelectedIndex();
        String identificador = indice == 0 ? null : indice == 1 ? "BANCO" : "J" + (indice - 1);
        String tipoSeleccionado = tipo.getSelectedIndex() == 0 ? null : (String) tipo.getSelectedItem();
        enviar.accept(new Mensaje.ConsultarTransacciones(identificador, tipoSeleccionado,
                orden.getSelectedIndex() == 1));
    }

    public void recibir(Mensaje.HistorialTransacciones respuesta) {
        if (!respuesta.exportacion()) {
            texto.setText(respuesta.texto());
            texto.setCaretPosition(0);
            return;
        }
        JFileChooser selector = new JFileChooser();
        selector.setFileFilter(new FileNameExtensionFilter("Archivo TXT", "txt"));
        selector.setSelectedFile(new File(respuesta.nombreArchivo()));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path archivo = selector.getSelectedFile().toPath();
        if (!archivo.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt")) {
            archivo = archivo.resolveSibling(archivo.getFileName() + ".txt");
        }
        if (Files.exists(archivo) && JOptionPane.showConfirmDialog(this,
                "El archivo ya existe. ¿Desea reemplazarlo?", "Exportar TXT",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            Files.writeString(archivo, respuesta.texto(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Historial guardado en " + archivo.toAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el TXT: " + e.getMessage(),
                    "Error de exportación", JOptionPane.ERROR_MESSAGE);
        }
    }
}
