import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.Random;

public class Interfaz {

    static final String ARCHIVO_USUARIOS = "usuarios.txt";
    static final String ARCHIVO_PARTIDAS = "partidas.txt";

    public static void main(String[] args) {

        JFrame ventana = new JFrame("MONOPOLY CR");
        ventana.setSize(1200, 900);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setLocationRelativeTo(null);

        PanelFondo fondo = new PanelFondo("imagenes/fondo.jpg");
        fondo.setLayout(new BorderLayout());

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
                crearBotonImagen("imagenes/crear.png", 150, 100);

        JButton añadirUsuario =
                crearBotonImagen("imagenes/usuario.png", 150, 100);

        JButton entrarPartida =
                crearBotonImagen("imagenes/entrar.png", 180, 80);

        // =========================
        // PARTE SUPERIOR
        // =========================

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setOpaque(false);

        panelSuperior.setBorder(
                BorderFactory.createEmptyBorder(
                        15, 20, 0, 20
                )
        );

        JPanel botones = new JPanel(
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

        ventana.add(fondo);
        ventana.setVisible(true);
    }


    // =========================================================
    // REGISTRAR USUARIO
    // =========================================================

    public static void registrarUsuario(JFrame ventanaPrincipal) {

        JTextField campoNombre = new JTextField();

        Object[] mensaje = {
                "Escribe tu nombre:",
                campoNombre
        };

        int opcion = JOptionPane.showConfirmDialog(
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

            if (usuarioExiste(nombre)) {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        "Ese usuario ya existe.",
                        "Usuario existente",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            guardarUsuario(nombre);

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "Usuario registrado correctamente.",
                    "Registro exitoso",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }


    // =========================================================
    // CREAR PARTIDA
    // =========================================================

    public static void crearPartida(JFrame ventanaPrincipal) {

        // Campos para los dos jugadores
        JTextField jugador1 = new JTextField();
        JTextField jugador2 = new JTextField();

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 5, 5));

        panel.add(new JLabel("Nombre del Jugador 1:"));
        panel.add(jugador1);

        panel.add(new JLabel("Nombre del Jugador 2:"));
        panel.add(jugador2);

        int opcion = JOptionPane.showConfirmDialog(
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

        // =========================
        // COMPROBAR CAMPOS VACÍOS
        // =========================

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

        // =========================
        // COMPROBAR QUE EXISTAN
        // =========================

        if (!usuarioExiste(nombreJugador1)) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "El usuario " + nombreJugador1
                            + " no existe.\n"
                            + "Primero debes registrarlo.",
                    "Usuario no encontrado",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        if (!usuarioExiste(nombreJugador2)) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "El usuario " + nombreJugador2
                            + " no existe.\n"
                            + "Primero debes registrarlo.",
                    "Usuario no encontrado",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        // =========================
        // GENERAR CÓDIGO
        // =========================

        String codigo = generarCodigo();

        // =========================
        // GUARDAR PARTIDA
        // =========================

        guardarPartida(
                codigo,
                nombreJugador1,
                nombreJugador2
        );

        // =========================
        // MOSTRAR CÓDIGO
        // =========================

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
                        + codigo
                        + "\n\n"
                        + "Comparte este código con los demás jugadores.",
                "Partida creada",
                JOptionPane.INFORMATION_MESSAGE
        );
    }


    // =========================================================
    // GENERAR CÓDIGO DE 4 DÍGITOS
    // =========================================================

    public static String generarCodigo() {

        Random random = new Random();

        String codigo;

        do {

            int numero =
                    1000 + random.nextInt(9000);

            codigo = String.valueOf(numero);

        } while (partidaExiste(codigo));

        return codigo;
    }


    // =========================================================
    // GUARDAR PARTIDA
    // =========================================================

    public static void guardarPartida(
            String codigo,
            String jugador1,
            String jugador2
    ) {

        try {

            FileWriter escritor =
                    new FileWriter(
                            ARCHIVO_PARTIDAS,
                            true
                    );

            BufferedWriter buffer =
                    new BufferedWriter(escritor);

            /*
             * Guardamos:
             *
             * codigo | jugador1 | jugador2
             */

            buffer.write(
                    codigo
                            + "|"
                            + jugador1
                            + "|"
                            + jugador2
            );

            buffer.newLine();

            buffer.close();

        } catch (IOException e) {

            JOptionPane.showMessageDialog(
                    null,
                    "No se pudo guardar la partida.",
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

        JTextField campoCodigo =
                new JTextField();

        Object[] mensaje = {
                "Escribe el código de 4 dígitos:",
                campoCodigo
        };

        int opcion = JOptionPane.showConfirmDialog(
                ventanaPrincipal,
                mensaje,
                "Entrar a partida",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String codigo =
                campoCodigo.getText().trim();

        // =========================
        // COMPROBAR CÓDIGO
        // =========================

        if (codigo.length() != 4) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "El código debe tener exactamente 4 dígitos.",
                    "Código inválido",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        // Comprobar que sean números
        try {

            Integer.parseInt(codigo);

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "El código solo puede contener números.",
                    "Código inválido",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        // =========================
        // BUSCAR PARTIDA
        // =========================

        String[] datos =
                buscarPartida(codigo);

        if (datos == null) {

            JOptionPane.showMessageDialog(
                    ventanaPrincipal,
                    "La partida no existe.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        // =========================
        // PARTIDA ENCONTRADA
        // =========================

        JOptionPane.showMessageDialog(
                ventanaPrincipal,
                "¡Partida encontrada!\n\n"
                        + "Jugador 1: "
                        + datos[0]
                        + "\n"
                        + "Jugador 2: "
                        + datos[1]
                        + "\n\n"
                        + "Código: "
                        + codigo,
                "Partida encontrada",
                JOptionPane.INFORMATION_MESSAGE
        );
    }


    // =========================================================
    // BUSCAR PARTIDA
    // =========================================================

    public static String[] buscarPartida(
            String codigoBuscado
    ) {

        File archivo =
                new File(ARCHIVO_PARTIDAS);

        if (!archivo.exists()) {
            return null;
        }

        try {

            BufferedReader lector =
                    new BufferedReader(
                            new FileReader(archivo)
                    );

            String linea;

            while ((linea = lector.readLine()) != null) {

                String[] datos =
                        linea.split("\\|");

                if (datos.length == 3) {

                    String codigo =
                            datos[0];

                    if (codigo.equals(codigoBuscado)) {

                        lector.close();

                        return new String[]{
                                datos[1],
                                datos[2]
                        };
                    }
                }
            }

            lector.close();

        } catch (IOException e) {

            return null;
        }

        return null;
    }


    // =========================================================
    // COMPROBAR SI EXISTE PARTIDA
    // =========================================================

    public static boolean partidaExiste(
            String codigo
    ) {

        return buscarPartida(codigo) != null;
    }


    // =========================================================
    // GUARDAR USUARIO
    // =========================================================

    public static void guardarUsuario(
            String nombre
    ) {

        try {

            FileWriter escritor =
                    new FileWriter(
                            ARCHIVO_USUARIOS,
                            true
                    );

            BufferedWriter buffer =
                    new BufferedWriter(escritor);

            buffer.write(nombre);
            buffer.newLine();

            buffer.close();

        } catch (IOException e) {

            JOptionPane.showMessageDialog(
                    null,
                    "No se pudo guardar el usuario.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }


    // =========================================================
    // COMPROBAR USUARIO
    // =========================================================

    public static boolean usuarioExiste(
            String nombreBuscado
    ) {

        File archivo =
                new File(ARCHIVO_USUARIOS);

        if (!archivo.exists()) {
            return false;
        }

        try {

            BufferedReader lector =
                    new BufferedReader(
                            new FileReader(archivo)
                    );

            String nombre;

            while ((nombre = lector.readLine()) != null) {

                if (nombre.equalsIgnoreCase(nombreBuscado)) {

                    lector.close();

                    return true;
                }
            }

            lector.close();

        } catch (IOException e) {

            return false;
        }

        return false;
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
                new Dimension(ancho, alto)
        );

        boton.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseEntered(
                            MouseEvent e
                    ) {

                        boton.setIcon(iconoGrande);

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

                        boton.setIcon(iconoNormal);

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

    public PanelFondo(String ruta) {

        imagen =
                new ImageIcon(ruta).getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {

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
                new Color(0, 0, 0, 80)
        );

        g2.fillRect(
                0,
                0,
                getWidth(),
                getHeight()
        );
    }
}