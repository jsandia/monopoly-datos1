package cliente;

import modelo.Propiedad;
import modelo.Tablero;
import protocolo.Mensaje;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** Muestra las propiedades de cada jugador usando únicamente el estado del servidor. */
public final class VentanaPropiedades extends JDialog {
    private final JTabbedPane jugadores = new JTabbedPane();
    private final Tablero tablero;

    public VentanaPropiedades(JFrame padre, Tablero tablero) {
        super(padre, "Propiedades de los jugadores", false);
        this.tablero = tablero;
        add(jugadores, BorderLayout.CENTER);
        setPreferredSize(new Dimension(850, 420));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(padre);
    }

    /** Actualiza también una ventana abierta cuando se compra una propiedad o una casa. */
    public void actualizar(Mensaje.EstadoJuego estado) {
        int seleccion = jugadores.getSelectedIndex();
        jugadores.removeAll();
        for (int jugador = 0; jugador < estado.nombres().length; jugador++) {
            DefaultTableModel filas = new DefaultTableModel(
                    new String[]{"Propiedad", "Grupo", "Casas", "Hotel", "Renta", "Hipotecada"}, 0) {
                @Override public boolean isCellEditable(int fila, int columna) { return false; }
            };
            for (Mensaje.EstadoPropiedad p : estado.propiedades()) {
                if (p != null && p.propietario() == jugador) {
                    Propiedad propiedad = (Propiedad) tablero.obtenerCasilla(p.id());
                    filas.addRow(new Object[]{propiedad.getNombre(), propiedad.getGrupo(), p.casas(),
                            p.hotel() ? "Sí" : "No", "₡" + p.renta(), p.hipotecada() ? "Sí" : "No"});
                }
            }
            JTable tabla = new JTable(filas);
            tabla.setRowHeight(26);
            tabla.getColumnModel().getColumn(0).setPreferredWidth(250);
            tabla.setFillsViewportHeight(true);
            JPanel panel = new JPanel(new BorderLayout(0, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
            panel.add(new JLabel(filas.getRowCount() == 0 ? "Este jugador todavía no tiene propiedades."
                    : "Propiedades: " + filas.getRowCount()), BorderLayout.SOUTH);
            jugadores.addTab(estado.nombres()[jugador] + " (" + filas.getRowCount() + ")", panel);
        }
        if (seleccion >= 0 && seleccion < jugadores.getTabCount()) jugadores.setSelectedIndex(seleccion);
    }
}
