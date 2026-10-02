import config.Configuracion;
import modelo.Casilla;
import modelo.Dado;
import modelo.Juego;
import modelo.Jugador;
import modelo.Propiedad;
import modelo.RegistroTransacciones;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import protocolo.Mensaje;
import protocolo.Mensaje.Respuesta;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * El banco: guarda el estado oficial de la partida y es el único que lo cambia.
 *
 * Cómo se reparte el trabajo:
 * - Servidor acepta las conexiones y valida cada acción (turno, saldo, fase).
 * - ManejadorCliente atiende a cada computadora en su propio hilo y le pasa
 *   sus mensajes a los métodos de esta clase.
 * - ManejadorCajero hace lo mismo con la Pico 2W (dados y tarjetas RFID).
 * - Juego aplica las reglas del Monopoly.
 *
 * Como hay varios hilos (dos computadoras y el cajero), todo lo que toca la
 * partida se hace dentro de synchronized (gameLock), de uno en uno.
 */
public class Servidor {

    private final int puerto;
    private final ExecutorService hilosComputadoras = Executors.newFixedThreadPool(2);

    // Un solo candado para todo el estado de la partida
    private final Object gameLock = new Object();

    // Salida de cada computadora (índice = número de computadora). Sirve para
    // mandarles mensajes sin que los pidan: dados, avisos, estado.
    private final ObjectOutputStream[] salidas = new ObjectOutputStream[2];

    // Usuarios registrados y partidas creadas (por código de 4 dígitos)
    private final Set<String> usuarios = ConcurrentHashMap.newKeySet();
    private final Map<String, Partida> partidas = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // ---- La partida de Monopoly (null hasta que se unen las 2 computadoras) ----
    private Juego juego;

    // Los 4 jugadores en orden de entrada. Juego los tiene en su cola de
    // turnos, pero el estado que se manda a los clientes los necesita fijos.
    private final Jugador[] jugadores = new Jugador[Configuracion.CANTIDAD_JUGADORES];

    // jugadores[i] se juega desde la computadora computadoraDe[i]
    private final int[] computadoraDe = new int[Configuracion.CANTIDAD_JUGADORES];

    // Fase del turno: false = esperando dados, true = ya se movió
    private boolean yaTiro = false;

    // ---- RFID ----
    // Jugador local (0 o 1) que cada computadora está registrando; -1 = ninguno
    private final int[] jugadorLocalEsperandoTarjeta = {-1, -1};

    // Compra que espera la tarjeta del jugador para pagarse
    private enum TipoCompra { PROPIEDAD, CASA, HOTEL }
    private Jugador jugadorPendientePago;
    private Propiedad propiedadPendientePago;
    private int computadoraPendientePago = -1;
    private TipoCompra tipoCompraPendiente = TipoCompra.PROPIEDAD;

    // Mientras se debe un alquiler no se puede comprar ni terminar el turno
    private static final String MENSAJE_ALQUILER_PENDIENTE =
            "Primero hay que pagar el alquiler: acerque la tarjeta al cajero.";

    public Servidor() {
        this(Configuracion.PUERTO_JUEGO);
    }

    public Servidor(int puerto) {
        this.puerto = puerto;
    }

    public static void main(String[] args) {
        try {
            new Servidor().iniciar();
        } catch (IOException e) {
            System.out.println("No se pudo iniciar el servidor.");
            e.printStackTrace();
        }
    }

    // =========================================================
    // CONEXIONES
    // =========================================================

    /**
     * Abre el puerto del juego, se anuncia en la red, arranca el servidor del
     * cajero y espera a las 2 computadoras. Cada una se atiende en su propio hilo.
     */
    public void iniciar() throws IOException {
        ServerSocket socketJuego = new ServerSocket(puerto);
        System.out.println("Servidor iniciado en puerto " + puerto);
        Descubrimiento.anunciarServidor(puerto);

        // El cajero va antes del ciclo de abajo, porque ese ciclo se queda
        // esperando en accept() hasta que lleguen las 2 computadoras
        iniciarServidorCajero();

        System.out.println("Esperando 2 computadoras...");
        for (int computadora = 0; computadora < 2; computadora++) {
            Socket socket = socketJuego.accept();
            System.out.println("Computadora " + computadora + " conectada: " + socket.getInetAddress());
            hilosComputadoras.submit(new ManejadorCliente(socket, computadora, this));
        }
        System.out.println("Ya hay 2 computadoras conectadas.");
    }

    // El cajero escucha en otro puerto y en otro hilo, para que su accept()
    // no bloquee el de las computadoras
    private void iniciarServidorCajero() {
        new Thread(() -> {
            try (ServerSocket servidorCajero = new ServerSocket(Configuracion.PUERTO_CAJERO)) {
                System.out.println("Esperando al cajero en el puerto " + Configuracion.PUERTO_CAJERO);
                while (true) {
                    Socket socketCajero = servidorCajero.accept();
                    System.out.println("Cajero conectado desde " + socketCajero.getInetAddress());
                    new Thread(new ManejadorCajero(socketCajero, this)).start();
                }
            } catch (IOException e) {
                // Por ejemplo, si el puerto del cajero ya está ocupado
                System.out.println("Servidor del cajero detenido: " + e.getMessage());
            }
        }).start();
    }

    /** Lo llama ManejadorCliente después de la Bienvenida: desde ahí se le puede escribir. */
    void conectarComputadora(int computadora, ObjectOutputStream out) {
        salidas[computadora] = out;
    }

    /** Lo llama ManejadorCliente al perder la conexión: ya no se le escribe. */
    void desconectarComputadora(int computadora) {
        salidas[computadora] = null;
    }

    // =========================================================
    // ESCRIBIR A UNA COMPUTADORA O A TODAS
    // =========================================================

    /**
     * Al mismo 'out' le pueden escribir dos hilos: el de esa computadora
     * (respuestas) y el del cajero (dados). synchronized(out) hace que escriban
     * de uno en uno; si no, los bytes se mezclan y el stream se corrompe.
     */
    void escribir(ObjectOutputStream out, Mensaje mensaje) throws IOException {
        synchronized (out) {
            out.writeObject(mensaje);
            // ObjectOutputStream recuerda lo que ya mandó y a veces manda solo una
            // "referencia". reset() lo obliga a olvidar: cada EstadoJuego llega completo.
            out.reset();
            out.flush();
        }
    }

    // Manda el mismo mensaje a las 2 computadoras
    private void difundir(Mensaje mensaje) {
        for (ObjectOutputStream out : salidas) {
            if (out == null) continue;
            try {
                escribir(out, mensaje);
            } catch (IOException e) {
                // Si una computadora se cayó, la otra sigue recibiendo
                System.out.println("No se pudo avisar a una computadora: " + e.getMessage());
            }
        }
    }

    // Manda un mensaje a una sola computadora (por ejemplo, desde el hilo del cajero)
    private void enviarA(int computadora, Mensaje mensaje) {
        if (computadora < 0 || computadora >= salidas.length || salidas[computadora] == null) return;
        try {
            escribir(salidas[computadora], mensaje);
        } catch (IOException e) {
            System.out.println("No se pudo avisar a la computadora " + computadora + ": " + e.getMessage());
        }
    }

    private void responder(ObjectOutputStream out, String tipo, String mensaje) throws IOException {
        escribir(out, new Respuesta(tipo, mensaje));
    }

    // =========================================================
    // USUARIOS Y PARTIDAS
    // =========================================================

    void registrarUsuario(String nombre, ObjectOutputStream out) throws IOException {
        nombre = nombre.trim();
        if (nombre.isEmpty()) {
            responder(out, Respuesta.ERROR, "Debes escribir un nombre.");
        } else if (usuarioRegistrado(nombre)) {
            responder(out, Respuesta.USUARIO_EXISTE, "Ese usuario ya existe.");
        } else {
            usuarios.add(nombre);
            System.out.println("Usuario registrado: " + nombre);
            responder(out, Respuesta.REGISTRO_OK, "Usuario registrado correctamente.");
        }
    }

    private boolean usuarioRegistrado(String nombre) {
        return usuarios.stream().anyMatch(usuario -> usuario.equalsIgnoreCase(nombre));
    }

    // null si los dos usuarios existen; si no, el mensaje de error
    private String validarUsuarios(String jugador1, String jugador2) {
        if (jugador1.isEmpty() || jugador2.isEmpty()) return "Debes escribir los dos jugadores.";
        if (!usuarioRegistrado(jugador1)) return "El usuario " + jugador1 + " no está registrado.";
        if (!usuarioRegistrado(jugador2)) return "El usuario " + jugador2 + " no está registrado.";
        return null;
    }

    /** La primera computadora crea la partida con los jugadores 1 y 2 y recibe el código. */
    void crearPartida(String jugador1, String jugador2, int computadora, ObjectOutputStream out)
            throws IOException {
        jugador1 = jugador1.trim();
        jugador2 = jugador2.trim();
        String error = validarUsuarios(jugador1, jugador2);
        if (error != null) {
            responder(out, jugador1.isEmpty() || jugador2.isEmpty() ? Respuesta.ERROR : Respuesta.USUARIO_NO_EXISTE, error);
            return;
        }

        String codigo;
        do {
            codigo = String.valueOf(1000 + random.nextInt(9000));
        } while (partidas.containsKey(codigo));

        partidas.put(codigo, new Partida(codigo, jugador1, jugador2, computadora));
        System.out.println("Partida creada: " + codigo + " -> " + jugador1 + ", " + jugador2);
        responder(out, Respuesta.PARTIDA_CREADA, codigo);
    }

    /** La segunda computadora se une con el código y los jugadores 3 y 4: arranca el juego. */
    void entrarPartida(String codigo, String jugador1, String jugador2, int computadora,
            ObjectOutputStream out) throws IOException {
        codigo = codigo.trim();
        jugador1 = jugador1.trim();
        jugador2 = jugador2.trim();
        if (!codigo.matches("\\d{4}")) {
            responder(out, Respuesta.ERROR, "El código debe tener 4 dígitos.");
            return;
        }
        String error = validarUsuarios(jugador1, jugador2);
        if (error != null) {
            responder(out, jugador1.isEmpty() || jugador2.isEmpty() ? Respuesta.ERROR : Respuesta.USUARIO_NO_EXISTE, error);
            return;
        }
        Partida partida = partidas.get(codigo);
        if (partida == null) {
            responder(out, Respuesta.PARTIDA_NO_EXISTE, "La partida no existe.");
            return;
        }
        if (partida.jugador3 != null) {
            responder(out, Respuesta.PARTIDA_LLENA, "La partida ya tiene 4 jugadores.");
            return;
        }

        partida.jugador3 = jugador1;
        partida.jugador4 = jugador2;
        partida.computadoraInvitada = computadora;
        System.out.println("Partida " + codigo + " completa: " + partida.jugador1 + ", "
                + partida.jugador2 + ", " + partida.jugador3 + ", " + partida.jugador4);
        responder(out, Respuesta.PARTIDA_UNIDA, "Partida unida correctamente.");
        iniciarJuego(partida);
    }

    // Ya están los 4: se crea el Juego con sus jugadores y empieza la partida
    private void iniciarJuego(Partida partida) {
        synchronized (gameLock) {
            juego = new Juego(Configuracion.MAX_TURNOS, Configuracion.CANTIDAD_JUGADORES, partida.codigo);
            String[] nombres = {partida.jugador1, partida.jugador2, partida.jugador3, partida.jugador4};

            for (int i = 0; i < jugadores.length; i++) {
                jugadores[i] = new Jugador("J" + (i + 1), nombres[i], Configuracion.SALDO_INICIAL);
                if (partida.tarjetas[i] != null) {
                    jugadores[i].registrarTarjeta(partida.tarjetas[i]);
                }
                juego.agregarJugador(jugadores[i]);
                // Jugadores 1 y 2 son de quien creó la partida; 3 y 4, de quien se unió
                computadoraDe[i] = (i < 2) ? partida.computadoraCreadora : partida.computadoraInvitada;
            }

            yaTiro = false;
            System.out.println("Partida " + partida.codigo + " iniciada en el servidor.");
            difundir(new Mensaje.Aviso("Historial de transacciones: " + juego.getArchivoTransacciones()));
            difundir(new Mensaje.Aviso("¡Empieza la partida! Turno de "
                    + juego.getJugadorActual().getNombre() + ". Tirá los dados en el cajero."));
            difundir(crearEstado());
        }
    }

    // =========================================================
    // DADOS (del cajero o de prueba)
    // =========================================================

    /**
     * Lo llama ManejadorCajero cada vez que la Pico manda DADOS;d1;d2.
     *
     * @return null si los dados se usaron, o un código de error
     *         (NO_HAY_PARTIDA, YA_TIRO...) para contestarle a la Pico
     */
    public String recibirDadosDelCajero(int dado1, int dado2) {
        synchronized (gameLock) {
            System.out.println("[CAJERO] Dados: " + dado1 + " + " + dado2 + " = " + (dado1 + dado2));
            String error = aplicarDados(dado1, dado2);
            if (error != null) {
                System.out.println("[CAJERO] Dados rechazados: " + error);
            }
            return error;
        }
    }

    /** Botón "Dados de prueba": el servidor tira por el jugador en turno. */
    void tirarDadosPrueba(int computadora, ObjectOutputStream out) throws IOException {
        synchronized (gameLock) {
            String error = validarTurno(computadora);
            if (error != null) {
                escribir(out, new Mensaje.Aviso(error));
                return;
            }
            int[] dados = juego.lanzarDados();
            String codigo = aplicarDados(dados[0], dados[1]);
            if (codigo != null) {
                escribir(out, new Mensaje.Aviso("No se pueden tirar los dados ahora (" + codigo + ")."));
            }
        }
    }

    /**
     * Aplica una tirada al jugador en turno y avisa a todos.
     * Hay que llamarlo teniendo gameLock.
     *
     * @return null si se usó, o un código de error si no se podía tirar ahora
     */
    private String aplicarDados(int dado1, int dado2) {
        if (juego == null) return "NO_HAY_PARTIDA";
        if (juego.esFinDeJuego()) return "PARTIDA_TERMINADA";
        if (yaTiro) return "YA_TIRO";
        // Lo que manda el cajero se revisa con la misma regla de la clase Dado
        if (!Dado.esValido(dado1) || !Dado.esValido(dado2)) return "DADO_INVALIDO";

        Jugador quienTiro = juego.getJugadorActual();
        int indiceQuienTiro = indiceEnTurno();

        // Juego aplica todas las reglas y devuelve qué pasó, una línea por evento
        String resultado = juego.jugarTiro(dado1, dado2);
        yaTiro = true;

        // Primero los dados, para que los clientes los animen...
        difundir(new Mensaje.Dados(indiceQuienTiro, dado1, dado2));
        difundir(new Mensaje.Aviso(resultado));

        if (juego.esFinDeJuego()) {
            difundir(new Mensaje.Aviso(textoFinDePartida()));
        } else if (!quienTiro.isActivo()) {
            // Quebró en su propio tiro: no tiene nada más que hacer en este turno
            pasarTurno();
        } else if (juego.hayAlquilerPendiente()) {
            pedirPagoDeAlquiler(quienTiro);
        }

        // ...y después cómo quedó todo
        difundir(crearEstado());
        return null;
    }

    // =========================================================
    // ACCIONES DEL JUGADOR EN TURNO
    // =========================================================

    /** Pide comprar la propiedad donde cayó. Se paga después, con la tarjeta. */
    void comprarPropiedad(int computadora, ObjectOutputStream out) throws IOException {
        synchronized (gameLock) {
            String error = validarTurno(computadora);
            if (error == null && !yaTiro) error = "Primero hay que tirar los dados.";
            if (error == null && juego.hayAlquilerPendiente()) error = MENSAJE_ALQUILER_PENDIENTE;
            if (error == null && jugadorPendientePago != null) error = "Ya hay una compra esperando el pago con tarjeta.";

            Jugador actual = (error == null) ? juego.getJugadorActual() : null;
            Casilla casilla = (error == null) ? juego.getTablero().obtenerCasilla(actual.getPosicionActual()) : null;
            if (error == null && (!(casilla instanceof Propiedad p) || p.tienePropietario())) {
                error = "Esa casilla no se puede comprar.";
            }
            if (error != null) {
                escribir(out, new Mensaje.Aviso(error));
                return;
            }

            Propiedad propiedad = (Propiedad) casilla;
            if (actual.getSaldo() < propiedad.getPrecioCompra()) {
                escribir(out, new Mensaje.Aviso("Saldo insuficiente para comprar " + propiedad.getNombre() + "."));
                return;
            }
            if (!actual.tieneTarjetaRegistrada()) {
                escribir(out, new Mensaje.Aviso(actual.getNombre() + " no tiene tarjeta RFID registrada."));
                return;
            }

            esperarPago(actual, propiedad, computadora, TipoCompra.PROPIEDAD);
            escribir(out, new Mensaje.Aviso(actual.getNombre() + ", acerque su tarjeta RFID para pagar ₡"
                    + propiedad.getPrecioCompra() + " y comprar " + propiedad.getNombre() + "."));
        }
    }

    void comprarCasa(int computadora, ObjectOutputStream out, int idPropiedad) throws IOException {
        comprarConstruccion(computadora, out, idPropiedad, false);
    }

    void comprarHotel(int computadora, ObjectOutputStream out, int idPropiedad) throws IOException {
        comprarConstruccion(computadora, out, idPropiedad, true);
    }

    // Pide comprar una casa o un hotel en una de sus propiedades. Se paga con la tarjeta.
    private void comprarConstruccion(int computadora, ObjectOutputStream out, int idPropiedad, boolean hotel)
            throws IOException {
        synchronized (gameLock) {
            String error = validarTurno(computadora);
            if (error == null && juego.hayAlquilerPendiente()) error = MENSAJE_ALQUILER_PENDIENTE;
            if (error == null && jugadorPendientePago != null) error = "Ya hay una compra esperando el pago con tarjeta.";

            Propiedad propiedad = null;
            if (error == null) {
                if (idPropiedad < 0 || idPropiedad >= juego.getTablero().tamaño()
                        || !(juego.getTablero().obtenerCasilla(idPropiedad) instanceof Propiedad p)) {
                    error = "Selecciona una propiedad válida.";
                } else {
                    propiedad = p;
                    error = hotel ? juego.getBanco().motivoNoComprarHotel(juego.getJugadorActual(), propiedad)
                            : juego.getBanco().motivoNoComprarCasa(juego.getJugadorActual(), propiedad);
                }
            }
            if (error == null && !juego.getJugadorActual().tieneTarjetaRegistrada()) {
                error = "El jugador no tiene tarjeta RFID registrada.";
            }
            if (error != null) {
                escribir(out, new Mensaje.Aviso(error));
                if (juego != null) escribir(out, crearEstado());
                return;
            }

            Jugador actual = juego.getJugadorActual();
            esperarPago(actual, propiedad, computadora, hotel ? TipoCompra.HOTEL : TipoCompra.CASA);
            escribir(out, new Mensaje.Aviso(actual.getNombre() + ", acerque su tarjeta RFID para pagar ₡"
                    + (hotel ? propiedad.getPrecioHotel() : propiedad.getPrecioCasa())
                    + " y comprar " + (hotel ? "un hotel" : "una casa") + " en " + propiedad.getNombre() + "."));
        }
    }

    // Deja una compra esperando la tarjeta del jugador
    private void esperarPago(Jugador jugador, Propiedad propiedad, int computadora, TipoCompra tipo) {
        jugadorPendientePago = jugador;
        propiedadPendientePago = propiedad;
        computadoraPendientePago = computadora;
        tipoCompraPendiente = tipo;
    }

    private void cancelarPagoPendiente() {
        esperarPago(null, null, -1, TipoCompra.PROPIEDAD);
    }

    /** Estando preso y antes de tirar: pagar la multa (conMulta) o usar la carta. */
    void salirDeCarcel(int computadora, ObjectOutputStream out, boolean conMulta) throws IOException {
        synchronized (gameLock) {
            String error = validarTurno(computadora);
            Jugador actual = (error == null) ? juego.getJugadorActual() : null;
            if (error == null && yaTiro) {
                error = "Ya tiraste los dados en este turno.";
            } else if (error == null && !actual.isEnCarcel()) {
                error = actual.getNombre() + " no está en la cárcel.";
            }
            if (error != null) {
                escribir(out, new Mensaje.Aviso(error));
                return;
            }

            boolean salio = conMulta ? juego.pagarMultaCarcel() : juego.usarCartaSalirCarcel();
            if (!salio) {
                escribir(out, new Mensaje.Aviso(conMulta
                        ? "Saldo insuficiente para pagar la multa de ₡" + Configuracion.MULTA_CARCEL + "."
                        : actual.getNombre() + " no tiene cartas para salir de la cárcel."));
                return;
            }

            difundir(new Mensaje.Aviso(actual.getNombre()
                    + (conMulta ? " paga ₡" + Configuracion.MULTA_CARCEL + " y sale de la cárcel."
                                : " usa su carta y sale de la cárcel.")
                    + " Ahora puede tirar los dados."));
            difundir(crearEstado());
        }
    }

    void terminarTurno(int computadora, ObjectOutputStream out) throws IOException {
        synchronized (gameLock) {
            String error = validarTurno(computadora);
            if (error == null && !yaTiro) error = "Todavía no se han tirado los dados.";
            if (error == null && juego.hayAlquilerPendiente()) error = MENSAJE_ALQUILER_PENDIENTE;
            if (error != null) {
                escribir(out, new Mensaje.Aviso(error));
                return;
            }
            pasarTurno();
            difundir(crearEstado());
        }
    }

    // Pasa al siguiente jugador y avisa a todos. No manda el estado: eso lo
    // hace quien llama, al final. Hay que llamarlo teniendo gameLock.
    private void pasarTurno() {
        String avisos = juego.terminarTurnoActual();
        cancelarPagoPendiente(); // una compra esperando tarjeta no sobrevive al cambio de turno
        yaTiro = false;

        if (!avisos.isEmpty()) {
            difundir(new Mensaje.Aviso(avisos));
        }
        difundir(new Mensaje.Aviso(juego.esFinDeJuego() ? textoFinDePartida()
                : "Turno de " + juego.getJugadorActual().getNombre() + ". Tirá los dados en el cajero."));
    }

    // =========================================================
    // ALQUILER CON TARJETA
    // =========================================================

    // Cayó en una propiedad ajena: el alquiler se paga acercando la tarjeta.
    // Si el jugador no registró tarjeta, se cobra solo para no trabar la partida.
    private void pedirPagoDeAlquiler(Jugador deudor) {
        if (deudor.tieneTarjetaRegistrada()) {
            difundir(new Mensaje.Aviso("💳 " + deudor.getNombre()
                    + ", acerque su tarjeta al cajero para pagar el alquiler."));
            return;
        }
        difundir(new Mensaje.Aviso(juego.pagarAlquilerPendiente()
                + " (sin tarjeta registrada: se cobró automáticamente)"));
        revisarSiQuebroAlPagar(deudor);
    }

    // Después de cobrar un alquiler: si el deudor quebró, se termina su turno (o la partida)
    private void revisarSiQuebroAlPagar(Jugador deudor) {
        if (juego.esFinDeJuego()) {
            difundir(new Mensaje.Aviso(textoFinDePartida()));
        } else if (!deudor.isActivo()) {
            pasarTurno();
        }
    }

    // =========================================================
    // TARJETAS DEL CAJERO
    // =========================================================

    /**
     * Lo llama ManejadorCajero cada vez que la Pico manda TARJETA;UID.
     * Una misma tarjeta puede servir para tres cosas, en este orden:
     * pagar un alquiler pendiente, pagar una compra pendiente o registrarse.
     */
    public void recibirTarjetaDelCajero(String uid) {
        synchronized (gameLock) {
            if (uid == null || uid.isBlank()) return;
            uid = uid.trim().toUpperCase();
            System.out.println("[CAJERO] Tarjeta: " + uid);

            // El alquiler es obligatorio, así que va antes que cualquier compra
            if (juego != null && !juego.esFinDeJuego() && juego.hayAlquilerPendiente()) {
                pagarAlquilerConTarjeta(uid);
            } else if (jugadorPendientePago != null) {
                pagarCompraConTarjeta(uid);
            } else {
                registrarTarjetaLeida(uid);
            }
        }
    }

    private void pagarAlquilerConTarjeta(String uid) {
        Jugador deudor = juego.getJugadorActual();
        if (!deudor.validarTarjeta(uid)) {
            enviarA(computadoraDe[indiceEnTurno()],
                    new Mensaje.Aviso("Tarjeta incorrecta. Debe pagar el alquiler " + deudor.getNombre() + "."));
            return;
        }
        difundir(new Mensaje.Aviso("💳 " + juego.pagarAlquilerPendiente()));
        revisarSiQuebroAlPagar(deudor);
        difundir(crearEstado());
    }

    private void pagarCompraConTarjeta(String uid) {
        if (!jugadorPendientePago.validarTarjeta(uid)) {
            enviarA(computadoraPendientePago,
                    new Mensaje.Aviso("Tarjeta incorrecta. Debe pagar " + jugadorPendientePago.getNombre() + "."));
            return;
        }

        Jugador jugador = jugadorPendientePago;
        Propiedad propiedad = propiedadPendientePago;
        int computadora = computadoraPendientePago;
        TipoCompra compra = tipoCompraPendiente;
        cancelarPagoPendiente();

        // El banco vuelve a validar todo al momento de cobrar
        boolean pagado = !juego.esFinDeJuego() && juego.getJugadorActual() == jugador && switch (compra) {
            case CASA -> juego.getBanco().comprarCasa(jugador, propiedad);
            case HOTEL -> juego.getBanco().comprarHotel(jugador, propiedad);
            case PROPIEDAD -> juego.getBanco().comprarPropiedad(jugador, propiedad);
        };
        if (!pagado) {
            enviarA(computadora, new Mensaje.Aviso("No se pudo realizar el pago."));
        } else {
            String compro = switch (compra) {
                case CASA -> "una casa en ";
                case HOTEL -> "un hotel en ";
                case PROPIEDAD -> "";
            };
            difundir(new Mensaje.Aviso("¡" + jugador.getNombre() + " pagó con su tarjeta y compró "
                    + compro + propiedad.getNombre() + "!"));
        }
        difundir(crearEstado());
    }

    // Registro de tarjeta solicitado por una computadora antes de jugar
    private void registrarTarjetaLeida(String uid) {
        for (int computadora = 0; computadora < 2; computadora++) {
            int indiceLocal = jugadorLocalEsperandoTarjeta[computadora];
            if (indiceLocal == -1) continue;

            Partida partida = buscarPartidaDeComputadora(computadora);
            if (partida == null) {
                jugadorLocalEsperandoTarjeta[computadora] = -1;
                return;
            }
            if (partida.tieneTarjeta(uid)) {
                // Se sigue esperando: la computadora vuelve a pedir y se escanea otra
                enviarA(computadora, new Mensaje.ResultadoTarjeta(indiceLocal, false, uid,
                        "Esta tarjeta ya pertenece a otro jugador. Escanee otra."));
                return;
            }

            int indiceGlobal = (computadora == partida.computadoraCreadora) ? indiceLocal : 2 + indiceLocal;
            partida.tarjetas[indiceGlobal] = uid;
            if (jugadores[indiceGlobal] != null) {
                jugadores[indiceGlobal].registrarTarjeta(uid); // la partida ya había empezado
            }
            jugadorLocalEsperandoTarjeta[computadora] = -1;
            enviarA(computadora, new Mensaje.ResultadoTarjeta(indiceLocal, true, uid,
                    "Tarjeta registrada correctamente."));
            return;
        }
    }

    /**
     * Una computadora avisa que la próxima tarjeta que lea el cajero es de su
     * jugador local (0 o 1). Si no se puede atender, se le contesta de una vez
     * con un ResultadoTarjeta sin uid.
     */
    void solicitarRegistroTarjeta(int computadora, int indiceLocal, ObjectOutputStream out) throws IOException {
        synchronized (gameLock) {
            String error = null;
            if (indiceLocal < 0 || indiceLocal > 1) {
                error = "Jugador RFID inválido.";
            } else if (buscarPartidaDeComputadora(computadora) == null) {
                error = "No hay una partida para registrar la tarjeta.";
            }
            if (error != null) {
                escribir(out, new Mensaje.ResultadoTarjeta(indiceLocal, false, null, error));
                return;
            }
            jugadorLocalEsperandoTarjeta[computadora] = indiceLocal;
        }
    }

    private Partida buscarPartidaDeComputadora(int computadora) {
        for (Partida partida : partidas.values()) {
            if (partida.computadoraCreadora == computadora || partida.computadoraInvitada == computadora) {
                return partida;
            }
        }
        return null;
    }

    // =========================================================
    // HISTORIAL DE TRANSACCIONES
    // =========================================================

    /** Consultar y exportar no cambian el juego: se permiten incluso al terminar la partida. */
    void consultarTransacciones(int computadora, ObjectOutputStream out,
            Mensaje.ConsultarTransacciones consulta, boolean exportacion) throws IOException {
        synchronized (gameLock) {
            boolean participante = false;
            for (int deQuien : computadoraDe) {
                if (deQuien == computadora) participante = true;
            }
            if (juego == null || !participante) {
                escribir(out, new Mensaje.Aviso("No hay una partida disponible para consultar."));
                return;
            }

            String jugador = (consulta == null) ? null : consulta.jugador();
            TipoTransaccion tipo = null;
            if (consulta != null && consulta.tipo() != null) {
                try {
                    tipo = TipoTransaccion.valueOf(consulta.tipo());
                } catch (IllegalArgumentException e) {
                    escribir(out, new Mensaje.Aviso("Tipo de transacción desconocido."));
                    return;
                }
            }
            if (jugador != null && !Transaccion.ID_BANCO.equals(jugador) && !esJugador(jugador)) {
                escribir(out, new Mensaje.Aviso("Jugador desconocido."));
                return;
            }

            RegistroTransacciones registro = juego.getRegistroTransacciones();
            String texto = exportacion ? registro.exportar()
                    : registro.consultar(jugador, tipo, consulta.recientesPrimero());
            escribir(out, new Mensaje.HistorialTransacciones(texto,
                    registro.getArchivo().getFileName().toString(), exportacion));
        }
    }

    private boolean esJugador(String id) {
        for (Jugador jugador : jugadores) {
            if (jugador != null && jugador.getId().equals(id)) return true;
        }
        return false;
    }

    // =========================================================
    // AYUDANTES
    // =========================================================

    // null si la computadora puede actuar; si no, el motivo
    private String validarTurno(int computadora) {
        if (juego == null) return "La partida todavía no empezó.";
        if (juego.esFinDeJuego()) return "La partida ya terminó.";
        int enTurno = indiceEnTurno();
        if (computadoraDe[enTurno] != computadora) {
            return "No es tu turno: le toca a " + jugadores[enTurno].getNombre() + ".";
        }
        return null;
    }

    // Posición (0 a 3) del jugador en turno dentro de 'jugadores'
    private int indiceEnTurno() {
        return indiceDe(juego.getJugadorActual());
    }

    // Posición (0 a 3) de un jugador dentro de 'jugadores', o -1
    private int indiceDe(Jugador jugador) {
        if (jugador == null) return -1;
        for (int i = 0; i < jugadores.length; i++) {
            if (jugadores[i] == jugador) return i;
        }
        return -1;
    }

    // "¡Ganó X con ₡N!" para el registro de partida de los clientes
    private String textoFinDePartida() {
        Jugador ganador = juego.getGanador();
        if (ganador == null) {
            return "🏁 ¡La partida ha finalizado!";
        }
        return "🏆 ¡La partida ha finalizado! Gana " + ganador.getNombre()
                + " con un patrimonio de ₡" + ganador.calcularPatrimonio()
                + (juego.hayEmpate() ? " (empate en patrimonio: gana quien va primero en el turno)." : ".");
    }

    // =========================================================
    // ESTADO QUE SE MANDA A LOS CLIENTES
    // =========================================================

    /** Foto completa de la partida: los clientes dibujan solo lo que dice aquí. */
    private Mensaje.EstadoJuego crearEstado() {
        String errorHistorial = juego.getRegistroTransacciones().getErrorEscritura();
        if (errorHistorial != null) difundir(new Mensaje.Aviso(errorHistorial));

        int cantidad = jugadores.length;
        String[] nombres = new String[cantidad];
        int[] saldos = new int[cantidad];
        int[] posiciones = new int[cantidad];
        boolean[] enCarcel = new boolean[cantidad];
        int[] cartasCarcel = new int[cantidad];
        boolean[] activos = new boolean[cantidad];
        int[] patrimonios = new int[cantidad];
        for (int i = 0; i < cantidad; i++) {
            nombres[i] = jugadores[i].getNombre();
            saldos[i] = jugadores[i].getSaldo();
            posiciones[i] = jugadores[i].getPosicionActual();
            enCarcel[i] = jugadores[i].isEnCarcel();
            cartasCarcel[i] = jugadores[i].getCantidadCartasSalirCarcel();
            activos[i] = jugadores[i].isActivo();
            patrimonios[i] = jugadores[i].calcularPatrimonio();
        }

        Jugador actual = juego.getJugadorActual();
        boolean puedeComprar = yaTiro && actual.isActivo() && !juego.esFinDeJuego()
                && juego.getTablero().obtenerCasilla(actual.getPosicionActual()) instanceof Propiedad p
                && !p.tienePropietario();

        Mensaje.EstadoPropiedad[] propiedades = new Mensaje.EstadoPropiedad[juego.getTablero().tamaño()];
        for (int i = 0; i < propiedades.length; i++) {
            if (juego.getTablero().obtenerCasilla(i) instanceof Propiedad p) {
                propiedades[i] = new Mensaje.EstadoPropiedad(i, indiceDe(p.getPropietario()),
                        p.getCantidadCasas(), p.tieneHotel(), p.calcularAlquiler(), p.getPrecioCasa(),
                        juego.getBanco().motivoNoComprarCasa(actual, p), p.getPrecioHotel(),
                        juego.getBanco().motivoNoComprarHotel(actual, p));
            }
        }

        return new Mensaje.EstadoJuego(juego.getTurnoActualGlobal(), juego.getMaxTurnos(), indiceEnTurno(),
                nombres, saldos, posiciones, computadoraDe.clone(), yaTiro, puedeComprar,
                juego.esFinDeJuego(), enCarcel, cartasCarcel, propiedades,
                juego.getBanco().getCasasDisponibles(), juego.getBanco().getHotelesDisponibles(),
                activos, patrimonios, indiceDe(juego.getGanador()), juego.hayEmpate(),
                textoAlquilerPendiente());
    }

    // "₡14 de alquiler a Beto por Liberia", o null si no se debe nada
    private String textoAlquilerPendiente() {
        Propiedad propiedad = juego.getAlquilerPendiente();
        if (propiedad == null) return null;
        return "₡" + propiedad.calcularAlquiler() + " de alquiler a "
                + propiedad.getPropietario().getNombre() + " por " + propiedad.getNombre();
    }

    // =========================================================
    // PARTIDA ANTES DE EMPEZAR
    // =========================================================

    /** Una partida mientras se arma: quiénes juegan, desde qué computadora y sus tarjetas. */
    private static class Partida {
        final String codigo;
        final String jugador1;
        final String jugador2;
        final int computadoraCreadora;
        String jugador3;                  // null hasta que se une la otra computadora
        String jugador4;
        int computadoraInvitada = -1;
        final String[] tarjetas = new String[4]; // UID de los jugadores 1 a 4

        Partida(String codigo, String jugador1, String jugador2, int computadoraCreadora) {
            this.codigo = codigo;
            this.jugador1 = jugador1;
            this.jugador2 = jugador2;
            this.computadoraCreadora = computadoraCreadora;
        }

        boolean tieneTarjeta(String uid) {
            for (String tarjeta : tarjetas) {
                if (tarjeta != null && tarjeta.equalsIgnoreCase(uid)) return true;
            }
            return false;
        }
    }
}
