package cliente;

import modelo.Tablero;
import protocolo.Message;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import static org.junit.jupiter.api.Assertions.*;

class VentanaPropiedadesTest {
    private static Message.EstadoJuego estado(Message.EstadoPropiedad[] propiedades) {
        return new Message.EstadoJuego(1, 100, 0, new String[]{"Ana", "José", "Luis", "María"},
                new int[]{1500, 1500, 1500, 1500}, new int[4], new int[]{0, 0, 1, 1},
                false, false, false, new boolean[4], new int[4], propiedades, 32, 12,
                new boolean[]{true, true, true, true}, new int[4], -1, false);
    }

    private static <T extends Component> T buscar(Component componente, Class<T> tipo) {
        if (tipo.isInstance(componente)) return tipo.cast(componente);
        if (componente instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) {
                T resultado = buscar(hijo, tipo);
                if (resultado != null) return resultado;
            }
        }
        return null;
    }

    @Test void separaPropiedadesPorDuenoYMuestraJugadoresSinPropiedades() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VentanaPropiedades ventana = new VentanaPropiedades(null, new Tablero());
            try {
                Message.EstadoPropiedad[] propiedades = new Message.EstadoPropiedad[40];
                propiedades[1] = new Message.EstadoPropiedad(1, 0, 2, false, false, 30, 50, null, 50, "Necesita cuatro casas.");
                propiedades[3] = new Message.EstadoPropiedad(3, 1, 0, false, true, 0, 50, null, 50, "Propiedad hipotecada.");
                ventana.actualizar(estado(propiedades));
                JTabbedPane pestanas = buscar(ventana.getContentPane(), JTabbedPane.class);
                assertEquals(4, pestanas.getTabCount());
                assertEquals("Ana (1)", pestanas.getTitleAt(0));
                JTable ana = buscar(pestanas.getComponentAt(0), JTable.class);
                JTable jose = buscar(pestanas.getComponentAt(1), JTable.class);
                assertEquals("Sixaola", ana.getValueAt(0, 0));
                assertEquals(2, ana.getValueAt(0, 2));
                assertEquals("₡30", ana.getValueAt(0, 4));
                assertFalse(ana.isCellEditable(0, 0));
                assertEquals("Cahuita", jose.getValueAt(0, 0));
                assertEquals("Sí", jose.getValueAt(0, 5));
                assertEquals(0, buscar(pestanas.getComponentAt(2), JTable.class).getRowCount());
                assertTrue(buscar(pestanas.getComponentAt(2), JLabel.class).getText().contains("todavía no tiene"));
            } finally { ventana.dispose(); }
        });
    }

    @Test void actualizaComprasYCantidadDeCasasSinCambiarLaPestanaSeleccionada() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VentanaPropiedades ventana = new VentanaPropiedades(null, new Tablero());
            try {
                Message.EstadoPropiedad[] propiedades = new Message.EstadoPropiedad[40];
                ventana.actualizar(estado(propiedades));
                JTabbedPane pestanas = buscar(ventana.getContentPane(), JTabbedPane.class);
                pestanas.setSelectedIndex(1);
                propiedades[3] = new Message.EstadoPropiedad(3, 1, 3, false, false, 180, 50, null, 50, "Necesita cuatro casas.");
                ventana.actualizar(estado(propiedades));
                assertEquals(1, pestanas.getSelectedIndex());
                assertEquals("José (1)", pestanas.getTitleAt(1));
                JTable tabla = buscar(pestanas.getComponentAt(1), JTable.class);
                assertEquals("Cahuita", tabla.getValueAt(0, 0));
                assertEquals(3, tabla.getValueAt(0, 2));
                assertEquals("₡180", tabla.getValueAt(0, 4));
                propiedades[3] = new Message.EstadoPropiedad(3, 1, 0, true, false, 450, 50,
                        "Ya tiene hotel.", 50, "Ya tiene hotel.");
                ventana.actualizar(estado(propiedades));
                tabla = buscar(pestanas.getComponentAt(1), JTable.class);
                assertEquals(0, tabla.getValueAt(0, 2));
                assertEquals("Sí", tabla.getValueAt(0, 3));
                assertEquals("₡450", tabla.getValueAt(0, 4));
            } finally { ventana.dispose(); }
        });
    }
}
