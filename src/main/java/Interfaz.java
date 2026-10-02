import cliente.VentanaTablero;
import protocolo.Mensaje;
import protocolo.Mensaje.Respuesta;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Pantalla de inicio del juego. Desde aquí se inicia el servidor (en la
 * computadora del organizador), se registran usuarios y se crea o se une
 * una partida. Al final se registran las tarjetas RFID y se abre el tablero.
 */
public class Interfaz {

    // Conexión de esta computadora con el servidor (null si no hay)
    static Cliente cliente;

    static boolean servidorIniciado = false;

    // Ventana abierta mientras se registran las tarjetas (null si no hay)
    static VentanaEscaneoTarjetas ventanaEscaneo;

    public static void main(String[] args) {
        JFrame ventana = new JFrame("MONOPOLY CR");
        ventana.setSize(1200, 800);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setLocationRelativeTo(null);

        // Si ya hay un servidor en la red, esta computadora se conecta de una vez
        try {
            cliente = new Cliente();
            cliente.conectar();
        } catch (Exception e) {
            cliente = null;
        }

        JLabel titulo = new JLabel("MONOPOLY CR");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Arial", Font.BOLD, 32));

        JButton crearPartida = crearBotonImagen("imagenes/crear.png", 150, 100);
        JButton añadirUsuario = crearBotonImagen("imagenes/usuario.png", 150, 100);
        JButton entrarPartida = crearBotonImagen("imagenes/entrar.png", 180, 80);
        JButton iniciarServidor = crearBotonImagen("imagenes/servidor.png", 180, 100);

        crearPartida.addActionListener(e -> crearPartida(ventana));
        añadirUsuario.addActionListener(e -> registrarUsuario(ventana));
        entrarPartida.addActionListener(e -> entrarPartida(ventana));
        iniciarServidor.addActionListener(e -> iniciarServidor(ventana));

        // Arriba: el título a la izquierda y los botones a la derecha
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);
        botones.add(crearPartida);
        botones.add(añadirUsuario);
        botones.add(entrarPartida);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setOpaque(false);
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(15, 20, 0, 20));
        panelSuperior.add(titulo, BorderLayout.WEST);
        panelSuperior.add(botones, BorderLayout.EAST);

        // Abajo a la izquierda: el botón del servidor
        JPanel panelServidor = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        panelServidor.setOpaque(false);
        panelServidor.add(iniciarServidor);

        PanelFondo fondo = new PanelFondo("imagenes/fondo.jpg");
        fondo.setLayout(new BorderLayout());
        fondo.add(panelSuperior, BorderLayout.NORTH);
        fondo.add(panelServidor, BorderLayout.SOUTH);

        ventana.add(fondo);
        ventana.setVisible(true);
    }

    // =========================================================
    // SERVIDOR
    // =========================================================

    /** Arranca el servidor en esta computadora y conecta esta misma computadora a él. */
    static void iniciarServidor(JFrame ventana) {
        if (servidorIniciado) {
            JOptionPane.showMessageDialog(ventana, "El servidor ya está iniciado.",
                    "Servidor", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // El servidor se queda esperando conexiones: va en otro hilo para no congelar la ventana
        servidorIniciado = true;
        new Thread(() -> {
            try {
                System.out.println("Servidor iniciado.");
                new Servidor().iniciar();
            } catch (Exception e) {
                servidorIniciado = false;
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(ventana,
                        "No se pudo iniciar el servidor.\n\n" + e.getMessage(),
                        "Error del servidor", JOptionPane.ERROR_MESSAGE));
            }
        }).start();

        // Medio segundo para que el servidor alcance a abrir el puerto
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            cliente = new Cliente();
            cliente.conectar();
            JOptionPane.showMessageDialog(ventana, "¡Servidor iniciado correctamente!\n\nEl cliente está conectado.",
                    "Servidor iniciado", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ventana, "El servidor se inició,\npero no se pudo conectar el cliente.\n\n"
                    + e.getMessage(), "Error de conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // USUARIOS Y PARTIDAS
    // =========================================================

    static void registrarUsuario(JFrame ventana) {
        if (!hayConexion(ventana)) return;

        JTextField campoNombre = new JTextField();
        int opcion = JOptionPane.showConfirmDialog(ventana, new Object[]{"Escribe tu nombre:", campoNombre},
                "Registrar usuario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) return;

        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarError(ventana, "Debes escribir un nombre.");
            return;
        }

        try {
            Respuesta respuesta = cliente.registrar(nombre);
            boolean registrado = respuesta.tipo().equals(Respuesta.REGISTRO_OK);
            JOptionPane.showMessageDialog(ventana, respuesta.mensaje(),
                    registrado ? "Registro exitoso" : "Registro",
                    registrado ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            mostrarError(ventana, "Error comunicándose con el servidor.");
        }
    }

    /** La primera computadora crea la partida con sus 2 jugadores y recibe el código. */
    static void crearPartida(JFrame ventana) {
        if (!hayConexion(ventana)) return;

        String[] datos = pedirDatos(ventana, "Crear partida",
                "Nombre del Jugador 1:", "Nombre del Jugador 2:");
        if (datos == null) return;

        try {
            Respuesta respuesta = cliente.crearPartida(datos[0], datos[1]);
            if (!respuesta.tipo().equals(Respuesta.PARTIDA_CREADA)) {
                mostrarError(ventana, respuesta.mensaje());
                return;
            }
            JOptionPane.showMessageDialog(ventana, "¡Partida creada correctamente!\n\n"
                            + "Jugador 1: " + datos[0] + "\nJugador 2: " + datos[1]
                            + "\n\nCÓDIGO DE PARTIDA:\n" + respuesta.mensaje()
                            + "\n\nComparte este código con la otra computadora.",
                    "Partida creada", JOptionPane.INFORMATION_MESSAGE);
            iniciarRegistroTarjetas(ventana, datos[0], datos[1]);
        } catch (Exception e) {
            mostrarError(ventana, "Error comunicándose con el servidor.");
        }
    }

    /** La segunda computadora se une con el código y sus 2 jugadores. */
    static void entrarPartida(JFrame ventana) {
        if (!hayConexion(ventana)) return;

        String[] datos = pedirDatos(ventana, "Entrar a partida",
                "Nombre del Jugador 1:", "Nombre del Jugador 2:", "Código de partida:");
        if (datos == null) return;

        String codigo = datos[2];
        if (!codigo.matches("\\d{4}")) {
            JOptionPane.showMessageDialog(ventana, "El código debe tener exactamente 4 dígitos.",
                    "Código inválido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Respuesta respuesta = cliente.entrarPartida(codigo, datos[0], datos[1]);
            if (!respuesta.tipo().equals(Respuesta.PARTIDA_UNIDA)) {
                mostrarError(ventana, respuesta.mensaje());
                return;
            }
            JOptionPane.showMessageDialog(ventana, "¡Partida encontrada!\n\n"
                            + "Jugador 1: " + datos[0] + "\nJugador 2: " + datos[1]
                            + "\n\nCódigo: " + codigo + "\n\nLos 4 jugadores están en la misma partida.",
                    "Partida encontrada", JOptionPane.INFORMATION_MESSAGE);
            iniciarRegistroTarjetas(ventana, datos[0], datos[1]);
        } catch (Exception e) {
            mostrarError(ventana, "Error comunicándose con el servidor.");
        }
    }

    /**
     * Muestra un diálogo con un campo de texto por etiqueta.
     *
     * @return los textos escritos (sin espacios a los lados), o null si se
     *         canceló o si faltó alguno de los dos nombres
     */
    private static String[] pedirDatos(JFrame ventana, String titulo, String... etiquetas) {
        JPanel panel = new JPanel(new GridLayout(etiquetas.length * 2, 1, 5, 5));
        JTextField[] campos = new JTextField[etiquetas.length];
        for (int i = 0; i < etiquetas.length; i++) {
            campos[i] = new JTextField();
            panel.add(new JLabel(etiquetas[i]));
            panel.add(campos[i]);
        }

        int opcion = JOptionPane.showConfirmDialog(ventana, panel, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) return null;

        String[] datos = new String[campos.length];
        for (int i = 0; i < campos.length; i++) {
            datos[i] = campos[i].getText().trim();
        }
        if (datos[0].isEmpty() || datos[1].isEmpty()) {
            mostrarError(ventana, "Debes escribir los nombres de los dos jugadores.");
            return null;
        }
        return datos;
    }

    private static boolean hayConexion(JFrame ventana) {
        if (cliente == null) {
            mostrarError(ventana, "No hay conexión con el servidor.");
            return false;
        }
        return true;
    }

    private static void mostrarError(Component ventana, String mensaje) {
        JOptionPane.showMessageDialog(ventana, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // =========================================================
    // TARJETAS Y TABLERO
    // =========================================================

    /**
     * Abre la ventana que registra la tarjeta de cada uno de los 2 jugadores
     * de esta computadora. Al terminar, abre el tablero.
     */
    static void iniciarRegistroTarjetas(JFrame ventana, String nombreJugador1, String nombreJugador2) {
        ventanaEscaneo = new VentanaEscaneoTarjetas(ventana, nombreJugador1, nombreJugador2, () -> {
            ventanaEscaneo = null;
            abrirTablero();
            ventana.dispose();
        });

        // Mientras se registran las tarjetas, el oyente del Cliente solo atiende
        // ResultadoTarjeta. Al abrir el tablero, abrirTablero() lo reemplaza.
        cliente.setOyente(mensaje -> {
            if (!(mensaje instanceof Mensaje.ResultadoTarjeta resultado)) return;
            // El oyente corre en el hilo lector del Cliente; Swing solo se toca desde su hilo
            SwingUtilities.invokeLater(() -> {
                if (ventanaEscaneo != null) {
                    ventanaEscaneo.tarjetaEscaneada(resultado);
                }
            });
        });

        ventanaEscaneo.setVisible(true);
        solicitarTarjetaRFID(0);
    }

    // Le avisa al servidor que la próxima tarjeta leída es del jugador local indicado
    private static void solicitarTarjetaRFID(int indiceLocal) {
        if (cliente != null) {
            cliente.enviarAccion(new Mensaje.RegistrarTarjeta(indiceLocal));
        }
    }

    /** Abre el tablero. Los nombres no hacen falta: llegan en el primer EstadoJuego. */
    static void abrirTablero() {
        SwingUtilities.invokeLater(() -> {
            VentanaTablero ventana = new VentanaTablero(cliente.getNumeroComputadora(), cliente::enviarAccion);
            // Lo que mande el servidor (dados, avisos, estado) le llega a la ventana
            cliente.setOyente(ventana::recibirMensaje);
            ventana.setVisible(true);
        });
    }

    // =========================================================
    // BOTÓN CON IMAGEN
    // =========================================================

    /** Un botón que es solo una imagen y crece un poco cuando el mouse pasa encima. */
    static JButton crearBotonImagen(String ruta, int ancho, int alto) {
        Image imagen = new ImageIcon(ruta).getImage();
        ImageIcon iconoNormal = new ImageIcon(imagen.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH));
        ImageIcon iconoGrande = new ImageIcon(imagen.getScaledInstance(ancho + 12, alto + 12, Image.SCALE_SMOOTH));

        JButton boton = new JButton(iconoNormal);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setMargin(new Insets(0, 0, 0, 0));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setPreferredSize(new Dimension(ancho, alto));

        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cambiarTamaño(boton, iconoGrande, ancho + 12, alto + 12);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cambiarTamaño(boton, iconoNormal, ancho, alto);
            }
        });
        return boton;
    }

    private static void cambiarTamaño(JButton boton, ImageIcon icono, int ancho, int alto) {
        boton.setIcon(icono);
        boton.setPreferredSize(new Dimension(ancho, alto));
        boton.getParent().revalidate();
        boton.repaint();
    }

    // =========================================================
    // VENTANA PARA ESCANEAR TARJETAS
    // =========================================================

    /**
     * Pide la tarjeta del jugador 1 y después la del jugador 2 de esta
     * computadora. El servidor confirma cada una con un ResultadoTarjeta.
     */
    static class VentanaEscaneoTarjetas extends JDialog {

        private final String nombreJugador1;
        private final String nombreJugador2;
        private final Runnable alTerminar;
        private int jugadorActual = 1; // 1 o 2; 3 = ya terminó

        private final JLabel etiquetaJugador = new JLabel("", SwingConstants.CENTER);
        private final JLabel etiquetaEstado = new JLabel("Acerque la tarjeta al lector RFID", SwingConstants.CENTER);

        VentanaEscaneoTarjetas(JFrame padre, String nombreJugador1, String nombreJugador2, Runnable alTerminar) {
            super(padre, "Registro de tarjetas", false);
            this.nombreJugador1 = nombreJugador1;
            this.nombreJugador2 = nombreJugador2;
            this.alTerminar = alTerminar;

            setSize(500, 300);
            setLocationRelativeTo(padre);
            setResizable(false);
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            JLabel titulo = new JLabel("REGISTRO DE TARJETAS", SwingConstants.CENTER);
            titulo.setFont(new Font("Arial", Font.BOLD, 24));
            etiquetaJugador.setFont(new Font("Arial", Font.BOLD, 20));
            etiquetaJugador.setAlignmentX(Component.CENTER_ALIGNMENT);
            etiquetaEstado.setFont(new Font("Arial", Font.PLAIN, 16));
            etiquetaEstado.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel centro = new JPanel();
            centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
            centro.setBorder(BorderFactory.createEmptyBorder(40, 10, 10, 10));
            centro.add(etiquetaJugador);
            centro.add(Box.createVerticalStrut(25));
            centro.add(etiquetaEstado);

            JPanel panelPrincipal = new JPanel(new BorderLayout());
            panelPrincipal.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
            panelPrincipal.add(titulo, BorderLayout.NORTH);
            panelPrincipal.add(centro, BorderLayout.CENTER);
            add(panelPrincipal);

            actualizarJugador();
        }

        private void actualizarJugador() {
            String nombre = (jugadorActual == 1) ? nombreJugador1 : nombreJugador2;
            etiquetaJugador.setText("Jugador " + jugadorActual + " - " + nombre + ": escanee su tarjeta");
            etiquetaEstado.setText("Acerque la tarjeta al lector RFID");
        }

        /** Llega la respuesta del servidor a la tarjeta que se acercó. */
        void tarjetaEscaneada(Mensaje.ResultadoTarjeta resultado) {
            if (!resultado.registrada()) {
                etiquetaEstado.setText(resultado.mensaje());
                // Con uid: se leyó una tarjeta que no sirve, se pide otra.
                // Sin uid: la solicitud falló; volver a pedir daría el mismo error.
                if (resultado.uid() != null) {
                    solicitarTarjetaRFID(jugadorActual - 1);
                }
                return;
            }

            // Solo cuenta si es la tarjeta del jugador que se está registrando ahora
            if (resultado.jugadorLocal() != jugadorActual - 1) return;

            etiquetaEstado.setText("Tarjeta registrada correctamente.");
            jugadorActual++;
            Timer pausa = new Timer(800, e -> {
                if (jugadorActual == 2) {
                    actualizarJugador();
                    solicitarTarjetaRFID(1);
                } else {
                    dispose();
                    alTerminar.run();
                }
            });
            pausa.setRepeats(false);
            pausa.start();
        }
    }
}

/** Panel que dibuja una imagen de fondo, estirada y un poco oscurecida. */
class PanelFondo extends JPanel {

    private final Image imagen;

    PanelFondo(String ruta) {
        imagen = new ImageIcon(ruta).getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(imagen, 0, 0, getWidth(), getHeight(), this);

        // Oscurece un poco el fondo para que se lean los botones
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}
