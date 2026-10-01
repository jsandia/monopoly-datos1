package cliente;

import modelo.Casilla;
import modelo.CasillaEspecial;
import modelo.CasillaEvento;
import modelo.Propiedad;
import modelo.Tablero;
import protocolo.Message;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Random;
import java.util.function.Consumer;

/**
 * La ventana (JFrame) que muestra el tablero. Solo arma las piezas:
 * el PanelTablero al centro y un panel lateral con el detalle de la
 * casilla seleccionada. El dibujo del tablero NO está aquí, está en PanelTablero.
 *
 * La partida NO vive aquí, vive en el servidor. Esta ventana:
 *  - manda intenciones (comprar, terminar turno...) con 'enviar'
 *  - recibe lo que decide el servidor en recibirMensaje(): Dados, Aviso, EstadoJuego
 * Nunca cambia saldos, posiciones ni turnos por su cuenta.
 */
public class VentanaTablero extends JFrame {

    private final JLabel etiquetaNombre = new JLabel("Hacé clic en una casilla");
    private final JLabel etiquetaTipo = new JLabel(" ");
    private final JTextArea areaDescripcion = new JTextArea();

    // --- Atributos de control agregados para el motor del juego ---
    private final JLabel etiquetaTurnoJugador = new JLabel("Turno: -");
    private final JLabel etiquetaSaldoJugador = new JLabel("Saldo: ₡0");
    private final JTextArea areaConsola = new JTextArea();

    // Los dados los tira el cajero; este botón pide al servidor unos al azar para probar sin la Pico
    private final JButton botonLanzarDados = new JButton("🎲 Dados de prueba (sin cajero)");
    private final JButton botonComprar = new JButton("🏠 Comprar Propiedad");
    private final JButton botonTerminarTurno = new JButton("➡️ Terminar Turno");
    private final JButton botonPagarMulta = new JButton("🔓 Pagar multa de la cárcel");
    private final JButton botonUsarCarta = new JButton("🃏 Usar carta \"Salga de la cárcel\"");
    private final JButton botonHistorial = new JButton("Historial y exportar TXT");
    private final JButton botonComprarCasa = new JButton("Comprar casas");
    private Message.EstadoJuego ultimoEstado;
    private final JButton botonPropiedades = new JButton("Propiedades de los jugadores");
    private VentanaPropiedades ventanaPropiedades;
    private Casilla casillaSeleccionada;
    private VentanaHistorial ventanaHistorial;
    private String[] nombresJugadores = new String[0];

    private final JLabel etiquetaIndicacion = new JLabel("Esperando a la otra computadora…");
    private final JLabel etiquetaJugadores = new JLabel(" ");

    private PanelTablero panelTablero;
    private Tablero modeloTablero; // solo para dibujar y mostrar el detalle de las casillas

    // --- Conexión con el servidor ---
    private final int miComputadora;               // 0 o 1, el playerId que dio el servidor
    private final Consumer<Message> enviar;        // manda una intención al servidor
    private boolean finMostrado = false;

    // --- Animación de los dados ---
    private static final int DURACION_ANIMACION_MS = 1200; // cuánto giran antes de mostrar el número final
    private static final int INTERVALO_ANIMACION_MS = 80;  // cada cuánto cambia la cara mientras gira
    private final Random random = new Random();
    private Timer temporizadorDados;                // != null mientras los dados están girando
    private Message.EstadoJuego estadoPendiente;    // estado que llegó mientras giraban

    /** Solo para ver el tablero sin servidor (no se puede jugar). */
    public VentanaTablero() {
        this(-1, mensaje -> { });
    }

    /**
     * @param miComputadora el playerId que el servidor le dio a esta computadora
     * @param enviar        cómo mandar una intención al servidor (GameClient::enviarAccion)
     */
    public VentanaTablero(int miComputadora, Consumer<Message> enviar) {
        super("Monopoly Costa Rica — Tablero");
        setDefaultCloseOperation(EXIT_ON_CLOSE); // cerrar la ventana termina el programa

        this.miComputadora = miComputadora;
        this.enviar = enviar;

        // BorderLayout reparte la ventana en zonas: CENTER crece, EAST tiene ancho fijo
        setLayout(new BorderLayout());

        this.modeloTablero = new Tablero();

        this.panelTablero = new PanelTablero(modeloTablero);
        add(panelTablero, BorderLayout.CENTER);
        add(crearPanelLateral(), BorderLayout.EAST);

        // Conexión panel -> ventana: cuando el panel detecta un clic en una casilla,
        // llama a mostrarCasilla(casilla). "this::mostrarCasilla" es una referencia
        // al método, equivalente a  casilla -> mostrarCasilla(casilla).
        panelTablero.setAlSeleccionar(this::mostrarCasilla);

        configurarEventosBotones();

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

        // Componentes interactivos del juego integrados al panel lateral
        etiquetaTurnoJugador.setFont(new Font("SansSerif", Font.BOLD, 16));
        etiquetaTurnoJugador.setForeground(new Color(0x2C3E50));

        etiquetaSaldoJugador.setFont(new Font("SansSerif", Font.BOLD, 14));
        etiquetaSaldoJugador.setForeground(new Color(0x27AE60));

        areaConsola.setEditable(false);
        areaConsola.setLineWrap(true);
        areaConsola.setWrapStyleWord(true);
        areaConsola.setFont(new Font("Monospaced", Font.PLAIN, 11));
        areaConsola.setBackground(new Color(0xEAEDED));

        // El registro va dentro de un JScrollPane: sin él, el JTextArea crece hacia
        // abajo y los mensajes nuevos quedan fuera de la ventana, sin forma de verlos.
        JScrollPane scrollConsola = new JScrollPane(areaConsola);
        scrollConsola.setBorder(BorderFactory.createTitledBorder("Registro de Partida"));
        scrollConsola.setPreferredSize(new Dimension(248, 260));

        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new BoxLayout(panelBotones, BoxLayout.Y_AXIS));
        panelBotones.setOpaque(false);

        botonLanzarDados.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        botonComprar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        botonComprarCasa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        botonComprarCasa.setEnabled(false);
        botonTerminarTurno.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        botonPagarMulta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        botonUsarCarta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        botonLanzarDados.setEnabled(false);
        botonPagarMulta.setEnabled(false);
        botonUsarCarta.setEnabled(false);
        botonComprar.setEnabled(false);
        botonTerminarTurno.setEnabled(false);
        botonHistorial.setEnabled(false);
        botonHistorial.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        botonPropiedades.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        botonPropiedades.setEnabled(false);

        etiquetaIndicacion.setFont(new Font("SansSerif", Font.BOLD, 13));
        etiquetaIndicacion.setForeground(new Color(0xC0392B));
        etiquetaJugadores.setFont(new Font("SansSerif", Font.PLAIN, 12));

        panelBotones.add(botonLanzarDados);
        panelBotones.add(javax.swing.Box.createRigidArea(new Dimension(0, 5)));
        panelBotones.add(botonComprar);
        panelBotones.add(javax.swing.Box.createRigidArea(new Dimension(0, 5)));
        panelBotones.add(botonComprarCasa);
        panelBotones.add(javax.swing.Box.createRigidArea(new Dimension(0, 5)));
        panelBotones.add(botonTerminarTurno);
        panelBotones.add(javax.swing.Box.createRigidArea(new Dimension(0, 5)));
        panelBotones.add(botonPagarMulta);
        panelBotones.add(javax.swing.Box.createRigidArea(new Dimension(0, 5)));
        panelBotones.add(botonUsarCarta);
        panelBotones.add(javax.swing.Box.createRigidArea(new Dimension(0, 5)));
        panelBotones.add(botonHistorial);
        panelBotones.add(javax.swing.Box.createRigidArea(new Dimension(0, 5)));
        panelBotones.add(botonPropiedades);

        panel.add(etiquetaTurnoJugador, 0);
        panel.add(etiquetaSaldoJugador, 1);
        panel.add(etiquetaIndicacion, 2);
        panel.add(etiquetaJugadores, 3);
        panel.add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));
        panel.add(panelBotones);
        panel.add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));
        panel.add(scrollConsola);

        return panel;
    }

    private void mostrarCasilla(Casilla casilla) {
        casillaSeleccionada = casilla;
        etiquetaNombre.setText(casilla.getNombre());
        etiquetaTipo.setText("Casilla #" + casilla.getId() + " · " + tipoDe(casilla));
        // getDescripcion() ejecuta la versión de la hija real (polimorfismo)
        areaDescripcion.setText(casilla.getDescripcion());
        if (casilla instanceof Propiedad p && ultimoEstado != null) {
            Message.EstadoPropiedad estado = ultimoEstado.propiedades()[p.getId()];
            if (estado != null) {
                String propietario = estado.propietario() < 0 ? "Sin dueño" : ultimoEstado.nombres()[estado.propietario()];
                areaDescripcion.setText("Grupo: " + p.getGrupo() + "\nPrecio: ₡" + p.getPrecio()
                        + "\nDueño: " + propietario + "\nRenta actual: ₡" + estado.renta()
                        + "\nCasas: " + estado.casas() + " | Hotel: " + (estado.hotel() ? "Sí" : "No")
                        + "\nHipotecada: " + (estado.hipotecada() ? "Sí" : "No")
                        + (p.esEdificable() ? "\nPrecio por casa: ₡" + estado.precioCasa() : ""));
            }
        }
    }

    private String tipoDe(Casilla casilla) {
        if (casilla instanceof Propiedad p) return "Propiedad · " + p.getGrupo();
        if (casilla instanceof CasillaEvento) return "Evento";
        if (casilla instanceof CasillaEspecial) return "Especial";
        return "";
    }

    /**
     * Punto de entrada de todo lo que manda el servidor. Lo llama el hilo
     * lector de GameClient, que NO es el hilo de Swing; por eso se pasa a
     * Swing con invokeLater antes de tocar cualquier componente.
     */
    public void recibirMensaje(Message mensaje) {
        SwingUtilities.invokeLater(() -> {
            if (mensaje instanceof Message.Dados dados) {
                animarDados(dados.dado1(), dados.dado2());
            } else if (mensaje instanceof Message.Aviso aviso) {
                registrarLog(aviso.texto());
            } else if (mensaje instanceof Message.HistorialTransacciones historial) {
                if (ventanaHistorial != null && ventanaHistorial.isDisplayable()) {
                    ventanaHistorial.recibir(historial);
                }
            } else if (mensaje instanceof Message.EstadoJuego estado) {
                if (temporizadorDados != null) {
                    estadoPendiente = estado; // se aplica cuando los dados terminen de girar
                } else {
                    aplicarEstado(estado);
                }
            }
        });
    }

    /**
     * Hace girar los dados en el tablero (caras al azar, solo visuales)
     * durante DURACION_ANIMACION_MS y los deja quietos en los valores
     * REALES, los que tiró el cajero y el servidor ya usó para mover.
     *
     * Usa javax.swing.Timer (no java.util.Timer): sus eventos se disparan
     * en el hilo de Swing, así que es seguro tocar los componentes de la
     * interfaz directamente desde adentro, sin SwingUtilities.invokeLater.
     */
    private void animarDados(int dadoFinal1, int dadoFinal2) {
        if (temporizadorDados != null) {
            temporizadorDados.stop();
        }

        long inicioAnimacion = System.currentTimeMillis();

        temporizadorDados = new Timer(INTERVALO_ANIMACION_MS, null);
        temporizadorDados.addActionListener(evento -> {
            long transcurrido = System.currentTimeMillis() - inicioAnimacion;

            if (transcurrido >= DURACION_ANIMACION_MS) {
                temporizadorDados.stop();
                temporizadorDados = null;
                panelTablero.mostrarDados(dadoFinal1, dadoFinal2); // números del cajero, ya no al azar
                panelTablero.repaint();

                if (estadoPendiente != null) {
                    aplicarEstado(estadoPendiente);
                    estadoPendiente = null;
                }
                return;
            }

            // Mientras gira: caras al azar solo para el efecto visual
            panelTablero.mostrarDados(1 + random.nextInt(6), 1 + random.nextInt(6));
            panelTablero.repaint();
        });
        temporizadorDados.start();
    }

    /** Pone la ventana igual a lo que dice el servidor. */
    private void aplicarEstado(Message.EstadoJuego estado) {
        ultimoEstado = estado;
        botonPropiedades.setEnabled(true);
        if (ventanaPropiedades != null && ventanaPropiedades.isDisplayable()) {
            ventanaPropiedades.actualizar(estado);
        }
        panelTablero.actualizarPropiedades(estado.propiedades());
        if (casillaSeleccionada != null) mostrarCasilla(casillaSeleccionada);
        nombresJugadores = estado.nombres().clone();
        botonHistorial.setEnabled(true);
        int enTurno = estado.enTurno();
        String nombreEnTurno = estado.nombres()[enTurno];
        boolean esMiTurno = estado.computadoras()[enTurno] == miComputadora && !estado.terminado();

        etiquetaTurnoJugador.setText("Turno de " + nombreEnTurno
                + " (" + estado.turnoGlobal() + "/" + estado.maxTurnos() + ")");
        etiquetaSaldoJugador.setText("Saldo: ₡" + estado.saldos()[enTurno]);

        // Los 4 jugadores. JLabel acepta un poquito de HTML para poner varias líneas.
        StringBuilder lista = new StringBuilder("<html>");
        for (int i = 0; i < estado.nombres().length; i++) {
            lista.append(i == enTurno ? "▶ " : "&nbsp;&nbsp;&nbsp;")
                    .append(estado.nombres()[i])
                    .append(" — ₡").append(estado.saldos()[i])
                    .append(" — casilla ").append(estado.posiciones()[i])
                    .append(estado.enCarcel()[i] ? " — 🔒 preso" : "")
                    .append("<br>");
        }
        etiquetaJugadores.setText(lista.append("</html>").toString());

        if (estado.terminado()) {
            etiquetaIndicacion.setText("Partida terminada");
        } else if (!estado.yaTiro()) {
            panelTablero.ocultarDados(); // turno nuevo: todavía no hay dados
            if (estado.enCarcel()[enTurno]) {
                etiquetaIndicacion.setText(esMiTurno
                        ? "🔒 En la cárcel: pagá, usá una carta o tirá buscando dobles"
                        : nombreEnTurno + " está en la cárcel");
            } else {
                etiquetaIndicacion.setText(esMiTurno
                        ? "🎲 Tirá los dados en el cajero"
                        : "Esperando a que " + nombreEnTurno + " tire los dados");
            }
        } else {
            etiquetaIndicacion.setText(esMiTurno ? "Comprá o terminá el turno" : "Juega " + nombreEnTurno);
            mostrarCasilla(modeloTablero.obtenerCasilla(estado.posiciones()[enTurno]));
            if (estado.enCarcel()[enTurno]) {
                // La casilla 10 describe la visita; si está preso hay que decirlo claro
                areaDescripcion.setText("🔒 " + nombreEnTurno + " está PRESO en la cárcel.\n"
                        + "En su próximo turno puede pagar la multa, usar una carta "
                        + "o tirar buscando dobles.");
            }
        }

        // Los botones solo se prenden si el jugador en turno es de esta computadora.
        // Igual el servidor vuelve a revisar todo: esto es solo comodidad.
        botonLanzarDados.setEnabled(esMiTurno && !estado.yaTiro());
        botonComprar.setEnabled(esMiTurno && estado.puedeComprar());
        boolean tienePropiedadEdificable = false;
        for (Message.EstadoPropiedad p : estado.propiedades()) {
            if (p != null && p.propietario() == enTurno && p.precioCasa() > 0) tienePropiedadEdificable = true;
        }
        botonComprarCasa.setEnabled(esMiTurno && tienePropiedadEdificable);
        botonComprarCasa.setToolTipText("Construye en tus propiedades durante tu turno, antes o después de tirar.");
        botonTerminarTurno.setEnabled(esMiTurno && estado.yaTiro());
        boolean presoSinTirar = esMiTurno && !estado.yaTiro() && estado.enCarcel()[enTurno];
        botonPagarMulta.setEnabled(presoSinTirar);
        botonUsarCarta.setEnabled(presoSinTirar && estado.cartasCarcel()[enTurno] > 0);

        // Fichas: si alguna cambió de casilla, el panel la hace caminar hasta ahí.
        // Como aplicarEstado se llama cuando terminan de girar los dados, las
        // fichas se mueven justo después de ver el resultado.
        panelTablero.actualizarFichas(estado.posiciones(), estado.nombres(),
                estado.terminado() ? -1 : enTurno);

        panelTablero.repaint();

        if (estado.terminado() && !finMostrado) {
            finMostrado = true;
            JOptionPane.showMessageDialog(this, "¡La partida ha finalizado!", "Fin de Juego", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void configurarEventosBotones() {
        botonPropiedades.addActionListener(evento -> {
            if (ultimoEstado == null) return;
            if (ventanaPropiedades == null || !ventanaPropiedades.isDisplayable()) {
                ventanaPropiedades = new VentanaPropiedades(this, modeloTablero);
            }
            ventanaPropiedades.actualizar(ultimoEstado);
            ventanaPropiedades.setVisible(true);
        });
        botonComprarCasa.addActionListener(evento -> elegirPropiedadParaCasa());
        botonHistorial.addActionListener(evento -> {
            if (ventanaHistorial == null || !ventanaHistorial.isDisplayable()) {
                ventanaHistorial = new VentanaHistorial(this, nombresJugadores, enviar);
            }
            ventanaHistorial.setVisible(true);
            ventanaHistorial.consultar();
        });
        // Solo mandan la intención. Lo que pase lo contesta el servidor.
        botonLanzarDados.addActionListener(e -> {
            botonLanzarDados.setEnabled(false); // evita doble clic mientras responde
            enviar.accept(new Message.TirarDadosPrueba());
        });
        botonComprar.addActionListener(e -> {
            botonComprar.setEnabled(false);
            enviar.accept(new Message.ComprarPropiedad());
        });
        botonTerminarTurno.addActionListener(e -> {
            botonTerminarTurno.setEnabled(false);
            enviar.accept(new Message.TerminarTurno());
        });
        botonPagarMulta.addActionListener(e -> {
            botonPagarMulta.setEnabled(false);
            botonUsarCarta.setEnabled(false);
            enviar.accept(new Message.PagarMultaCarcel());
        });
        botonUsarCarta.addActionListener(e -> {
            botonPagarMulta.setEnabled(false);
            botonUsarCarta.setEnabled(false);
            enviar.accept(new Message.UsarCartaCarcel());
        });
    }

    private void registrarLog(String mensaje) {
        areaConsola.append(mensaje + "\n");
        // Mueve el cursor al final: el JScrollPane baja solo hasta el último mensaje
        areaConsola.setCaretPosition(areaConsola.getDocument().getLength());
    }

    private void elegirPropiedadParaCasa() {
        if (ultimoEstado == null) return;
        javax.swing.JComboBox<OpcionCasa> opciones = new javax.swing.JComboBox<>();
        for (Message.EstadoPropiedad p : ultimoEstado.propiedades()) {
            if (p != null && p.propietario() == ultimoEstado.enTurno() && p.precioCasa() > 0) {
                opciones.addItem(new OpcionCasa(p, modeloTablero.obtenerCasilla(p.id()).getNombre()));
            }
        }
        JTextArea reglas = new JTextArea("Debes completar una vuelta desde la compra de la propiedad.\n"
                + "Máximo 4 casas por propiedad. Casas disponibles: " + ultimoEstado.casasDisponibles());
        reglas.setEditable(false);
        reglas.setOpaque(false);
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(reglas, BorderLayout.NORTH);
        panel.add(opciones, BorderLayout.CENTER);
        JTextArea disponibilidad = new JTextArea(3, 42);
        disponibilidad.setEditable(false);
        disponibilidad.setLineWrap(true);
        disponibilidad.setWrapStyleWord(true);
        disponibilidad.setOpaque(false);
        Runnable mostrarDisponibilidad = () -> {
            OpcionCasa elegida = (OpcionCasa) opciones.getSelectedItem();
            if (elegida != null) {
                String motivo = elegida.estado().motivoNoComprarCasa();
                disponibilidad.setText(motivo == null ? "Puedes comprar una casa en esta propiedad." : motivo);
            }
        };
        opciones.addActionListener(evento -> mostrarDisponibilidad.run());
        mostrarDisponibilidad.run();
        panel.add(disponibilidad, BorderLayout.SOUTH);
        if (JOptionPane.showConfirmDialog(this, panel, "Comprar una casa", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        OpcionCasa opcion = (OpcionCasa) opciones.getSelectedItem();
        if (opcion == null) return;
        if (opcion.estado().motivoNoComprarCasa() != null) {
            JOptionPane.showMessageDialog(this, opcion.estado().motivoNoComprarCasa(), "No se puede construir",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        botonComprarCasa.setEnabled(false);
        enviar.accept(new Message.ComprarCasa(opcion.estado().id()));
    }

    private record OpcionCasa(Message.EstadoPropiedad estado, String nombre) {
        @Override public String toString() {
            return nombre + " — " + estado.casas() + " casas — ₡" + estado.precioCasa()
                    + (estado.motivoNoComprarCasa() == null ? "" : " (no disponible)");
        }
    }

    public static void main(String[] args) {
        // Swing tiene su propio hilo para la interfaz (el "Event Dispatch Thread").
        // invokeLater le pide a ese hilo que cree la ventana; nunca se crea desde main directo.
        SwingUtilities.invokeLater(() -> new VentanaTablero().setVisible(true));
    }
}
