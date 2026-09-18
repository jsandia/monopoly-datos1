import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;

public class Interfaz {

    // Archivo donde se guardarán los usuarios
    static final String ARCHIVO_USUARIOS = "usuarios.txt";

    public static void main(String[] args) {

        JFrame ventana = new JFrame("Mi Juego");
        ventana.setSize(1200, 900);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setLocationRelativeTo(null);

        PanelFondo fondo = new PanelFondo("imagenes/fondo.jpg");
        fondo.setLayout(new BorderLayout());

        // -------------------------
        // TÍTULO
        // -------------------------

        JLabel titulo = new JLabel("MONOPOLY CR");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Arial", Font.BOLD, 32));

        // -------------------------
        // BOTONES
        // -------------------------

        JButton crearPartida =
                crearBotonImagen("imagenes/crear.png", 150, 100);

        JButton añadirUsuario =
                crearBotonImagen("imagenes/usuario.png", 150, 100);

        JButton entrarPartida =
                crearBotonImagen("imagenes/entrar.png", 150, 100);

        // -------------------------
        // PARTE SUPERIOR
        // -------------------------

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setOpaque(false);
        panelSuperior.setBorder(
                BorderFactory.createEmptyBorder(15, 20, 0, 20)
        );

        JPanel botones = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0)
        );

        botones.setOpaque(false);

        botones.add(crearPartida);
        botones.add(añadirUsuario);
        botones.add(entrarPartida);

        panelSuperior.add(titulo, BorderLayout.WEST);
        panelSuperior.add(botones, BorderLayout.EAST);

        fondo.add(panelSuperior, BorderLayout.NORTH);

        // -------------------------
        // BOTÓN CREAR PARTIDA
        // -------------------------

        crearPartida.addActionListener(e -> {

            JOptionPane.showMessageDialog(
                    ventana,
                    "Aquí se abrirá la creación de la partida."
            );

        });

        // -------------------------
        // BOTÓN AÑADIR USUARIO
        // -------------------------

        añadirUsuario.addActionListener(e -> registrarUsuario(ventana));

        // -------------------------
        // BOTÓN ENTRAR A PARTIDA
        // -------------------------

        entrarPartida.addActionListener(e -> entrarUsuario(ventana));

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

            String nombre = campoNombre.getText().trim();

            // Verificar que no esté vacío
            if (nombre.isEmpty()) {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        "Debes escribir un nombre.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            // Verificar si ya existe
            if (usuarioExiste(nombre)) {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        "Ese usuario ya existe.",
                        "Usuario existente",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            // Guardar usuario
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
    // ENTRAR CON USUARIO
    // =========================================================

    public static void entrarUsuario(JFrame ventanaPrincipal) {

        JTextField campoNombre = new JTextField();

        Object[] mensaje = {
                "Escribe tu nombre de usuario:",
                campoNombre
        };

        int opcion = JOptionPane.showConfirmDialog(
                ventanaPrincipal,
                mensaje,
                "Iniciar sesión",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (opcion == JOptionPane.OK_OPTION) {

            String nombre = campoNombre.getText().trim();

            if (nombre.isEmpty()) {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        "Debes escribir un nombre.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            // Comprobar si existe
            if (usuarioExiste(nombre)) {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        "¡Bienvenido, " + nombre + "!",
                        "Inicio de sesión",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // Aquí posteriormente podemos abrir
                // la pantalla principal del juego.

            } else {

                JOptionPane.showMessageDialog(
                        ventanaPrincipal,
                        "El usuario no existe.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }


    // =========================================================
    // GUARDAR USUARIO
    // =========================================================

    public static void guardarUsuario(String nombre) {

        try {

            FileWriter escritor = new FileWriter(
                    ARCHIVO_USUARIOS,
                    true
            );

            BufferedWriter buffer = new BufferedWriter(escritor);

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
    // COMPROBAR SI EXISTE
    // =========================================================

    public static boolean usuarioExiste(String nombreBuscado) {

        File archivo = new File(ARCHIVO_USUARIOS);

        // Si el archivo todavía no existe,
        // significa que no hay usuarios.
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
    // CREAR BOTÓN CON IMAGEN
    // =========================================================

    public static JButton crearBotonImagen(
            String ruta,
            int ancho,
            int alto
    ) {

        ImageIcon iconoOriginal = new ImageIcon(ruta);

        Image imagenOriginal = iconoOriginal.getImage();

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

        JButton boton = new JButton(iconoNormal);

        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);
        boton.setMargin(new Insets(0, 0, 0, 0));

        boton.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        boton.setPreferredSize(
                new Dimension(ancho, alto)
        );

        // Animación al pasar el mouse
        boton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {

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
            public void mouseExited(MouseEvent e) {

                boton.setIcon(iconoNormal);

                boton.setPreferredSize(
                        new Dimension(ancho, alto)
                );

                boton.getParent().revalidate();
                boton.repaint();
            }
        });

        return boton;
    }
}


// =============================================================
// PANEL PARA EL FONDO
// =============================================================

class PanelFondo extends JPanel {

    private Image imagen;

    public PanelFondo(String ruta) {

        imagen = new ImageIcon(ruta).getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

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

        // Oscurecer un poco el fondo
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