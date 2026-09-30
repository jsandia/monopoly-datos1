package cliente;

import modelo.TipoTransaccion;
import protocolo.Message;

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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Consulta el historial oficial; el cliente solo muestra y guarda el texto recibido. */
public final class VentanaHistorial extends JDialog {
    private final JComboBox<String> jugador = new JComboBox<>();
    private final JComboBox<String> tipo = new JComboBox<>();
    private final JComboBox<String> orden = new JComboBox<>(new String[]{"Mas antigua primero", "Mas reciente primero"});
    private final JTextArea texto = new JTextArea();
    private final Consumer<Message> enviar;

    public VentanaHistorial(JFrame ventana, String[] nombres, Consumer<Message> enviar) {
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
        exportar.addActionListener(evento -> enviar.accept(new Message.ExportarTransacciones()));
        texto.setEditable(false);
        texto.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
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
        enviar.accept(new Message.ConsultarTransacciones(identificador, tipoSeleccionado,
                orden.getSelectedIndex() == 1));
    }

    public void recibir(Message.HistorialTransacciones respuesta) {
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
        if (!archivo.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".txt")) {
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
                    "Error de exportacion", JOptionPane.ERROR_MESSAGE);
        }
    }
}
