import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import cliente.VentanaTablero;

public class Interfaz {

    static final String ARCHIVO_USUARIOS = "usuarios.txt";
    static final String ARCHIVO_PARTIDAS = "partidas.txt";

    // =========================================================
    // CONEXIÓN CON EL SERVIDOR
    // =========================================================

    static GameClient cliente;

    // =========================================================
    // SERVIDOR
    // =========================================================

    static GameServer servidor;

    static boolean servidorIniciado = false;


    public static void main(String[] args) {

        JFrame ventana = new JFrame("MONOPOLY CR");
        ventana.setSize(1200, 800);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setLocationRelativeTo(null);

        PanelFondo fondo = new PanelFondo("imagenes/fondo.jpg");
        fondo.setLayout(new BorderLayout());


        // =========================================================
        // CONECTAR AL SERVIDOR
        // =========================================================

        try {

            cliente =
                    new GameClient();

            cliente.conectar();

        } catch (Exception e) {

            cliente = null;

        }


        // =========================
        // TÍTULO
        // =========================

        JLabel titulo = new JLabel("MONOPOLY CR");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Arial", Font.BOLD, 32));


        // =========================
        // BOTONES
        // =========================

        JButton crearPartida =
                crearBotonImagen(
                        "imagenes/crear.png",
                        150,
                        100
                );


        JButton añadirUsuario =
                crearBotonImagen(
                        "imagenes/usuario.png",
                        150,
                        100
                );


        JButton entrarPartida =
                crearBotonImagen(
                        "imagenes/entrar.png",
                        180,
                        80
                );


        // =========================================================
        // NUEVO BOTÓN DEL SERVIDOR
        // =========================================================

        JButton iniciarServidor =
                crearBotonImagen(
                        "imagenes/servidor.png",
                        180,
                        100
                );


        // =========================
        // PARTE SUPERIOR
        // =========================

        JPanel panelSuperior =
                new JPanel(
                        new BorderLayout()
                );

        panelSuperior.setOpaque(false);

        panelSuperior.setBorder(
                BorderFactory.createEmptyBorder(
                        15,
                        20,
                        0,
                        20
                )
        );


        JPanel botones =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                10,
                                0
                        )
                );

        botones.setOpaque(false);

        botones.add(crearPartida);
        botones.add(añadirUsuario);
        botones.add(entrarPartida);


        panelSuperior.add(
                titulo,
                BorderLayout.WEST
        );

        panelSuperior.add(
                botones,
                BorderLayout.EAST
        );


        fondo.add(
                panelSuperior,
                BorderLayout.NORTH
        );


        // =========================================================
        // BOTÓN SERVIDOR - ESQUINA INFERIOR IZQUIERDA
        // =========================================================

        JPanel panelServidor =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                20,
                                15
                        )
                );

        panelServidor.setOpaque(false);

        panelServidor.add(iniciarServidor);


        fondo.add(
                panelServidor,
                BorderLayout.SOUTH
        );


        // =========================
        // CREAR PARTIDA
        // =========================

        crearPartida.addActionListener(
                e -> crearPartida(ventana)
        );


        // =========================
        // AÑADIR USUARIO
        // =========================

        añadirUsuario.addActionListener(
                e -> registrarUsuario(ventana)
        );


        // =========================
        // ENTRAR A PARTIDA
        // =========================

        entrarPartida.addActionListener(
                e -> entrarPartida(ventana)
        );


        // =========================================================
        // INICIAR SERVIDOR
        // =========================================================

        iniciarServidor.addActionListener(
                e -> iniciarServidor(ventana)
        );


        ventana.add(fondo);

        ventana.setVisible(true);
    }


    // =========================================================
    // INICIAR SERVIDOR
    // =========================================================

    public static void iniciarServidor(
            JFrame ventanaPrincipal
    ) {

        // ---------------------------------------------------------
        // COMPROBAR SI YA ESTÁ INICIADO
        // ---------------------------------------------------------

        if (servidorIniciado) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "El servidor ya está iniciado.",
                    "Servidor",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }


        // ---------------------------------------------------------
        // INICIAR SERVIDOR EN OTRO HILO
        // ---------------------------------------------------------

        Thread hiloServidor =
                new Thread(
                        () -> {

                            try {

                                servidor =
                                        new GameServer();


                                servidorIniciado = true;


                                System.out.println(
                                        "GameServer iniciado."
                                );


                                servidor.start();


                            } catch (IOException e) {

                                servidorIniciado = false;


                                SwingUtilities.invokeLater(
                                        () -> JOptionPane.showMessageDialog(
                                                ventanaPrincipal,

                                                "No se pudo iniciar el servidor.\n\n"
                                                        + e.getMessage(),

                                                "Error del servidor",

                                                JOptionPane.ERROR_MESSAGE
                                        )
                                );
                            }
                        }
                );


        hiloServidor.start();


        // ---------------------------------------------------------
        // ESPERAR A QUE EL SERVIDOR ABRA EL PUERTO
        // ---------------------------------------------------------

        try {

            Thread.sleep(500);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }


        // ---------------------------------------------------------
        // CONECTAR CLIENTE
        // ---------------------------------------------------------

        try {

            cliente =
                    new GameClient();


            cliente.conectar();


            JOptionPane.showMessageDialog(
                    ventanaPrincipal,

                    "¡Servidor iniciado correctamente!\n\n"
                            + "El GameClient está conectado.",

                    "Servidor iniciado",

                    JOptionPane.INFORMATION_MESSAGE
            );


        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,

                    "El servidor se inició,\n"
                            + "pero no se pudo conectar el GameClient.\n\n"
                            + e.getMessage(),

                    "Error de conexión",

                    JOptionPane.ERROR_MESSAGE
            );
        }
    }


    // =========================================================
    // REGISTRAR USUARIO
    // =========================================================

    public static void registrarUsuario(
            JFrame ventanaPrincipal
    ) {

        if (cliente == null) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "No hay conexión con el servidor.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }


        JTextField campoNombre =
                new JTextField();


        Object[] mensaje = {

                "Escribe tu nombre:",
                campoNombre
        };


        int opcion =
                JOptionPane.showConfirmDialog(
                        ventanaPrincipal,
                        mensaje,
                        "Registrar usuario",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );


        if (opcion == JOptionPane.OK_OPTION) {

            String nombre =
                    campoNombre.getText().trim();


            if (nombre.isEmpty()) {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        "Debes escribir un nombre.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }


            try {

                Message.Response respuesta =
                        cliente.registrar(nombre);


                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        respuesta.mensaje(),
                        respuesta.tipo().equals("REGISTER_OK")
                                ? "Registro exitoso"
                                : "Registro",
                        respuesta.tipo().equals("REGISTER_OK")
                                ? JOptionPane.INFORMATION_MESSAGE
                                : JOptionPane.WARNING_MESSAGE
                );


            } catch (Exception e) {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        "Error comunicándose con el servidor.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }


    // =========================================================
    // CREAR PARTIDA
    // =========================================================

    public static void crearPartida(
            JFrame ventanaPrincipal
    ) {

        if (cliente == null) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "No hay conexión con el servidor.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }


        // =====================================================
        // DOS JUGADORES
        // =====================================================

        JTextField jugador1 =
                new JTextField();

        JTextField jugador2 =
                new JTextField();


        JPanel panel =
                new JPanel();

        panel.setLayout(
                new GridLayout(
                        4,
                        1,
                        5,
                        5
                )
        );


        panel.add(
                new JLabel(
                        "Nombre del Jugador 1:"
                )
        );

        panel.add(jugador1);


        panel.add(
                new JLabel(
                        "Nombre del Jugador 2:"
                )
        );

        panel.add(jugador2);


        int opcion =
                JOptionPane.showConfirmDialog(
                        ventanaPrincipal,
                        panel,
                        "Crear partida",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );


        if (opcion != JOptionPane.OK_OPTION) {

            return;
        }


        String nombreJugador1 =
                jugador1.getText().trim();

        String nombreJugador2 =
                jugador2.getText().trim();


        // =====================================================
        // COMPROBAR CAMPOS
        // =====================================================

        if (nombreJugador1.isEmpty()
                || nombreJugador2.isEmpty()) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "Debes escribir los nombres de los dos jugadores.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }


        // =====================================================
        // CREAR EN SERVIDOR
        // =====================================================

        try {

            Message.Response respuesta =
                    cliente.crearPartida(
                            nombreJugador1,
                            nombreJugador2
                    );


            if (respuesta.tipo().equals(
                    "GAME_CREATED"
            )) {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,

                        "¡Partida creada correctamente!\n\n"
                                + "Jugador 1: "
                                + nombreJugador1
                                + "\n"
                                + "Jugador 2: "
                                + nombreJugador2
                                + "\n\n"
                                + "CÓDIGO DE PARTIDA:\n"
                                + respuesta.mensaje()
                                + "\n\n"
                                + "Comparte este código con la otra computadora.",

                        "Partida creada",

                        JOptionPane.INFORMATION_MESSAGE
                );

                // =================================================
                // ABRIR TABLERO CON LOS NOMBRES INGRESADOS
                // =================================================

                String j1 = nombreJugador1;
                String j2 = nombreJugador2;

                SwingUtilities.invokeLater(
                        () -> new cliente.VentanaTablero(j1, j2).setVisible(true)
                );

                ventanaPrincipal.dispose();


            } else {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        respuesta.mensaje(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }


        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "Error comunicándose con el servidor.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }


    // =========================================================
    // ENTRAR A PARTIDA
    // =========================================================

    public static void entrarPartida(
            JFrame ventanaPrincipal
    ) {

        if (cliente == null) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "No hay conexión con el servidor.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }


        JTextField jugador1 =
                new JTextField();

        JTextField jugador2 =
                new JTextField();

        JTextField campoCodigo =
                new JTextField();


        JPanel panel =
                new JPanel();

        panel.setLayout(
                new GridLayout(
                        6,
                        1,
                        5,
                        5
                )
        );


        panel.add(
                new JLabel(
                        "Nombre del Jugador 1:"
                )
        );

        panel.add(jugador1);


        panel.add(
                new JLabel(
                        "Nombre del Jugador 2:"
                )
        );

        panel.add(jugador2);


        panel.add(
                new JLabel(
                        "Código de partida:"
                )
        );

        panel.add(campoCodigo);


        int opcion =
                JOptionPane.showConfirmDialog(
                        ventanaPrincipal,
                        panel,
                        "Entrar a partida",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );


        if (opcion != JOptionPane.OK_OPTION) {

            return;
        }


        String nombreJugador1 =
                jugador1.getText().trim();

        String nombreJugador2 =
                jugador2.getText().trim();

        String codigo =
                campoCodigo.getText().trim();


        // =====================================================
        // COMPROBAR CAMPOS
        // =====================================================

        if (nombreJugador1.isEmpty()
                || nombreJugador2.isEmpty()) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "Debes escribir los nombres de los dos jugadores.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }


        // =====================================================
        // COMPROBAR CÓDIGO
        // =====================================================

        if (!codigo.matches("\\d{4}")) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "El código debe tener exactamente 4 dígitos.",
                    "Código inválido",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }


        // =====================================================
        // ENTRAR
        // =====================================================

        try {

            Message.Response respuesta =
                    cliente.entrarPartida(
                            codigo,
                            nombreJugador1,
                            nombreJugador2
                    );


            if (respuesta.tipo().equals(
                    "GAME_JOINED"
            )) {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,

                        "¡Partida encontrada!\n\n"
                                + "Jugador 1: "
                                + nombreJugador1
                                + "\n"
                                + "Jugador 2: "
                                + nombreJugador2
                                + "\n\n"
                                + "Código: "
                                + codigo
                                + "\n\n"
                                + "Los 4 jugadores están en la misma partida.",

                        "Partida encontrada",

                        JOptionPane.INFORMATION_MESSAGE
                );

                // =================================================
                // ABRIR TABLERO CON LOS NOMBRES INGRESADOS
                // =================================================

                String j1 = nombreJugador1;
                String j2 = nombreJugador2;

                SwingUtilities.invokeLater(
                        () -> new cliente.VentanaTablero(j1, j2).setVisible(true)
                );

                ventanaPrincipal.dispose();


            } else {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        respuesta.mensaje(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }


        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "Error comunicándose con el servidor.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }


    // =========================================================
    // BOTÓN CON IMAGEN
    // =========================================================

    public static JButton crearBotonImagen(
            String ruta,
            int ancho,
            int alto
    ) {

        ImageIcon iconoOriginal =
                new ImageIcon(ruta);

        Image imagenOriginal =
                iconoOriginal.getImage();

        Image imagenNormal =
                imagenOriginal.getScaledInstance(
                        ancho,
                        alto,
                        Image.SCALE_SMOOTH
                );

        Image imagenGrande =
                imagenOriginal.getScaledInstance(
                        ancho + 12,
                        alto + 12,
                        Image.SCALE_SMOOTH
                );

        ImageIcon iconoNormal =
                new ImageIcon(imagenNormal);

        ImageIcon iconoGrande =
                new ImageIcon(imagenGrande);

        JButton boton =
                new JButton(iconoNormal);

        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);

        boton.setMargin(
                new Insets(0, 0, 0, 0)
        );

        boton.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        boton.setPreferredSize(
                new Dimension(
                        ancho,
                        alto
                )
        );

        boton.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseEntered(
                            MouseEvent e
                    ) {

                        boton.setIcon(
                                iconoGrande
                        );

                        boton.setPreferredSize(
                                new Dimension(
                                        ancho + 12,
                                        alto + 12
                                )
                        );

                        boton.getParent().revalidate();

                        boton.repaint();
                    }

                    @Override
                    public void mouseExited(
                            MouseEvent e
                    ) {

                        boton.setIcon(
                                iconoNormal
                        );

                        boton.setPreferredSize(
                                new Dimension(
                                        ancho,
                                        alto
                                )
                        );

                        boton.getParent().revalidate();

                        boton.repaint();
                    }
                }
        );

        return boton;
    }
}


// =============================================================
// FONDO
// =============================================================

class PanelFondo extends JPanel {

    private Image imagen;

    public PanelFondo(
            String ruta
    ) {

        imagen =
                new ImageIcon(ruta).getImage();
    }

    @Override
    protected void paintComponent(
            Graphics g
    ) {

        super.paintComponent(g);

        Graphics2D g2 =
                (Graphics2D) g;

        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        g2.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        g2.drawImage(
                imagen,
                0,
                0,
                getWidth(),
                getHeight(),
                this
        );

        // Oscurecer ligeramente el fondo
        g2.setColor(
                new Color(
                        0,
                        0,
                        0,
                        80
                )
        );

        g2.fillRect(
                0,
                0,
                getWidth(),
                getHeight()
        );
    }
}