import javax.swing.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Interfaz {

    public static void main(String[] args) {

        JFrame ventana = new JFrame("MONOPOLIO CR");

        ventana.setSize(1500, 900);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setLocationRelativeTo(null);

        // ==========================
        // FONDO
        // ==========================

        PanelFondo fondo =
                new PanelFondo("imagenes/fondo.jpg");

        fondo.setLayout(new BorderLayout());


        // ==========================
        // TITULO
        // ==========================

        JLabel titulo = new JLabel("MOMNOPOLIO CR");

        titulo.setForeground(Color.WHITE);
        titulo.setFont(
                new Font("Arial", Font.BOLD, 32)
        );


        // ==========================
        // BOTONES CON IMAGEN
        // ==========================

        JButton crearPartida =
                crearBotonImagen(
                        "imagenes/crear.png",
                        150,
                        100
                );

        JButton añadirUsuario =
                crearBotonImagen(
                        "imagenes/usuario.png",
                        175,
                        90
                );

        JButton entrarPartida =
                crearBotonImagen(
                        "imagenes/entrar.png",
                        175,
                        80
                );


        // ==========================
        // PANEL SUPERIOR
        // ==========================

        JPanel panelSuperior =
                new JPanel(new BorderLayout());

        panelSuperior.setOpaque(false);

        panelSuperior.setBorder(
                BorderFactory.createEmptyBorder(
                        15,
                        20,
                        0,
                        20
                )
        );


        // ==========================
        // PANEL DE BOTONES
        // ==========================

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


        // ==========================
        // AGREGAR ELEMENTOS
        // ==========================

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


        // ==========================
        // ACCIONES DE LOS BOTONES
        // ==========================

        crearPartida.addActionListener(e -> {

            JOptionPane.showMessageDialog(
                    ventana,
                    "Crear partida"
            );

        });


        añadirUsuario.addActionListener(e -> {

            JOptionPane.showMessageDialog(
                    ventana,
                    "Añadir usuario"
            );

        });


        entrarPartida.addActionListener(e -> {

            JOptionPane.showMessageDialog(
                    ventana,
                    "Entrar a partida"
            );

        });


        // ==========================
        // MOSTRAR
        // ==========================

        ventana.add(fondo);

        ventana.setVisible(true);
    }


    // =====================================================
    // CREAR BOTON CON IMAGEN
    // =====================================================

    public static JButton crearBotonImagen(
            String ruta,
            int ancho,
            int alto
    ) {

        ImageIcon iconoOriginal =
                new ImageIcon(ruta);

        Image imagenOriginal =
                iconoOriginal.getImage();


        // Imagen normal
        Image imagenNormal =
                imagenOriginal.getScaledInstance(
                        ancho,
                        alto,
                        Image.SCALE_SMOOTH
                );


        // Imagen cuando pasamos el cursor
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


        // Crear botón
        JButton boton =
                new JButton(iconoNormal);


        // Quitar apariencia tradicional
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setContentAreaFilled(false);
        boton.setOpaque(false);


        // Quitar espacio interno
        boton.setMargin(
                new Insets(0, 0, 0, 0)
        );


        // Cursor de mano
        boton.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );


        // Tamaño normal
        boton.setPreferredSize(
                new Dimension(
                        ancho,
                        alto
                )
        );


        // =================================================
        // ANIMACION DEL MOUSE
        // =================================================

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

                        boton.getParent()
                                .revalidate();

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

                        boton.getParent()
                                .revalidate();

                        boton.repaint();
                    }
                }
        );


        return boton;
    }
}


// =========================================================
// PANEL DEL FONDO
// =========================================================

class PanelFondo extends JPanel {

    private Image imagen;


    public PanelFondo(String ruta) {

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


        // Mejor calidad al escalar
        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints
                        .VALUE_INTERPOLATION_BICUBIC
        );


        g2.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints
                        .VALUE_RENDER_QUALITY
        );


        // Dibujar fondo
        g2.drawImage(
                imagen,
                0,
                0,
                getWidth(),
                getHeight(),
                this
        );


        // Capa oscura
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