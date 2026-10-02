import config.Configuracion;
import protocolo.Mensaje;
import modelo.Casilla;
import modelo.Juego;
import modelo.Jugador;
import modelo.Propiedad;
import modelo.Tablero;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

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

    private ServerSocket socketJuego;

    private ServerSocket servidorCajero;

    private final ExecutorService hilosComputadoras =
            Executors.newFixedThreadPool(4);

    // =========================================================
    // USUARIOS
    // =========================================================

    private final Set<String> usuarios =
            ConcurrentHashMap.newKeySet();

    // =========================================================
    // PARTIDAS
    // =========================================================

    private final Map<String, Partida> partidas =
            new ConcurrentHashMap<>();

    private final Random random =
            new Random();

    // Un solo candado para todo el estado de la partida: lo usan los hilos
    // de las dos computadoras y el del cajero.
    private final Object gameLock = new Object();


    // =========================================================
    // PARTIDA DE MONOPOLY (vive SOLO aqui, los clientes solo la dibujan)
    // =========================================================

    // Salida de cada computadora (indice = computadora). Sirve para
    // mandarles mensajes sin que los pidan: dados, avisos, estado.
    private final ObjectOutputStream[] salidas =
            new ObjectOutputStream[2];


    // null mientras no se hayan unido las 2 computadoras
    private Juego juego;
    private Tablero tablero;

    // Los 4 jugadores en orden de turno. Juego los guarda en su cola,
    // pero no deja recorrerlos, y el estado necesita los 4.
    private final Jugador[] jugadores = new Jugador[Configuracion.CANTIDAD_JUGADORES];

    // jugadores[i] se juega desde la computadora computadoraDe[i]
    private final int[] computadoraDe = new int[4];

    // Fase del turno: false = esperando dados, true = ya se movio
    private boolean yaTiro = false;

    // =========================================================
    // RFID
    // =========================================================
    // indice local (0 o 1) que cada computadora está registrando.
    private final int[] jugadorLocalEsperandoTarjeta = {-1, -1};

    // Compra pendiente de autorización con RFID.
    private Jugador jugadorPendientePago;
    private Propiedad propiedadPendientePago;
    private int computadoraPendientePago = -1;
    private enum TipoCompra { PROPIEDAD, CASA, HOTEL }
    private TipoCompra tipoCompraPendiente = TipoCompra.PROPIEDAD;

    // Mientras se debe un alquiler no se puede comprar ni terminar el turno
    private static final String MENSAJE_ALQUILER_PENDIENTE =
            "Primero hay que pagar el alquiler: acerque la tarjeta al cajero.";


    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    public Servidor() {
        this(Configuracion.PUERTO_JUEGO);
    }

    public Servidor(int puerto) {
        this.puerto = puerto;
    }


    // =========================================================
    // INICIAR SERVIDOR
    // =========================================================

    public void iniciar() throws IOException {

        socketJuego =
                new ServerSocket(puerto);

        System.out.println(
                "Servidor iniciado en puerto "
                        + puerto
        );

        Descubrimiento.anunciarServidor(puerto);

        // CAJERO: se abre ANTES del ciclo de abajo, porque ese ciclo
        // se queda esperando en accept() hasta que lleguen las 2 computadoras
        iniciarServidorCajero();

        System.out.println(
                "Esperando 2 computadoras..."
        );

        int computadora = 0;

        while (computadora < 2) {

            Socket socket =
                    socketJuego.accept();

            System.out.println(
                    "Computadora "
                            + computadora
                            + " conectada: "
                            + socket.getInetAddress()
            );

            // Cada computadora se atiende en su propio hilo
            hilosComputadoras.submit(
                    new ManejadorCliente(socket, computadora, this)
            );

            computadora++;
        }

        System.out.println(
                "Ya hay 2 computadoras conectadas."
        );
    }


    // =========================================================
    // CONEXIONES DE LAS COMPUTADORAS
    // =========================================================

    // Lo llama ManejadorCliente cuando la computadora ya recibió su
    // Bienvenida: desde ahí se le pueden mandar dados, avisos y estado.
    void conectarComputadora(int computadora, ObjectOutputStream out) {
        salidas[computadora] = out;
    }

    // Lo llama ManejadorCliente al perder la conexión: ya no se le escribe.
    void desconectarComputadora(int computadora) {
        salidas[computadora] = null;
    }


    // =========================================================
    // REGISTRAR USUARIO
    // =========================================================

    void registrarUsuario(
            String nombre,
            ObjectOutputStream out
    ) throws IOException {

        nombre =
                nombre.trim();

        if (nombre.isEmpty()) {

            enviarRespuesta(
                    out,
                    "ERROR",
                    "Debes escribir un nombre."
            );

            return;
        }

        String nombreFinal =
                nombre;

        boolean existe =
                usuarios.stream()
                        .anyMatch(
                                usuario ->
                                        usuario.equalsIgnoreCase(
                                                nombreFinal
                                        )
                        );

        if (existe) {

            enviarRespuesta(
                    out,
                    "USER_EXISTS",
                    "Ese usuario ya existe."
            );

            return;
        }

        usuarios.add(nombre);

        System.out.println(
                "Usuario registrado: "
                        + nombre
        );

        enviarRespuesta(
                out,
                "REGISTER_OK",
                "Usuario registrado correctamente."
        );
    }


    // =========================================================
    // LOGIN
    // =========================================================

    void iniciarSesion(
            String nombre,
            ObjectOutputStream out
    ) throws IOException {

        nombre =
                nombre.trim();

        if (nombre.isEmpty()) {

            enviarRespuesta(
                    out,
                    "ERROR",
                    "Debes escribir un nombre."
            );

            return;
        }

        String nombreFinal =
                nombre;

        boolean existe =
                usuarios.stream()
                        .anyMatch(
                                usuario ->
                                        usuario.equalsIgnoreCase(
                                                nombreFinal
                                        )
                        );

        if (!existe) {

            enviarRespuesta(
                    out,
                    "USER_NOT_FOUND",
                    "El usuario no está registrado."
            );

            return;
        }

        enviarRespuesta(
                out,
                "LOGIN_OK",
                "Inicio de sesión correcto."
        );
    }


    // =========================================================
    // CREAR PARTIDA
    // =========================================================

    void crearPartida(
            String jugador1,
            String jugador2,
            int computadora,
            ObjectOutputStream out
    ) throws IOException {

        jugador1 =
                jugador1.trim();

        jugador2 =
                jugador2.trim();


        // -------------------------
        // CAMPOS VACÍOS
        // -------------------------

        if (jugador1.isEmpty()
                || jugador2.isEmpty()) {

            enviarRespuesta(
                    out,
                    "ERROR",
                    "Debes escribir los dos jugadores."
            );

            return;
        }


        // -------------------------
        // COMPROBAR USUARIOS
        // -------------------------

        if (!usuarioRegistrado(jugador1)) {

            enviarRespuesta(
                    out,
                    "USER_NOT_FOUND",
                    "El usuario "
                            + jugador1
                            + " no está registrado."
            );

            return;
        }

        if (!usuarioRegistrado(jugador2)) {

            enviarRespuesta(
                    out,
                    "USER_NOT_FOUND",
                    "El usuario "
                            + jugador2
                            + " no está registrado."
            );

            return;
        }


        // -------------------------
        // GENERAR CÓDIGO
        // -------------------------

        String codigo =
                generarCodigo();


        // -------------------------
        // CREAR PARTIDA
        // -------------------------

        Partida partida =
                new Partida(
                        codigo,
                        jugador1,
                        jugador2
                );

        partida.computadoraCreadora =
                computadora;

        partidas.put(
                codigo,
                partida
        );


        System.out.println(
                "Partida creada: "
                        + codigo
                        + " -> "
                        + jugador1
                        + ", "
                        + jugador2
        );


        enviarRespuesta(
                out,
                "GAME_CREATED",
                codigo
        );
    }


    // =========================================================
    // ENTRAR A PARTIDA
    // =========================================================

    void entrarPartida(
            String codigo,
            String jugador1,
            String jugador2,
            int computadora,
            ObjectOutputStream out
    ) throws IOException {

        codigo =
                codigo.trim();

        jugador1 =
                jugador1.trim();

        jugador2 =
                jugador2.trim();


        // -------------------------
        // COMPROBAR CÓDIGO
        // -------------------------

        if (!codigo.matches("\\d{4}")) {

            enviarRespuesta(
                    out,
                    "ERROR",
                    "El código debe tener 4 dígitos."
            );

            return;
        }


        // -------------------------
        // COMPROBAR USUARIOS
        // -------------------------

        if (!usuarioRegistrado(jugador1)) {

            enviarRespuesta(
                    out,
                    "USER_NOT_FOUND",
                    "El usuario "
                            + jugador1
                            + " no está registrado."
            );

            return;
        }

        if (!usuarioRegistrado(jugador2)) {

            enviarRespuesta(
                    out,
                    "USER_NOT_FOUND",
                    "El usuario "
                            + jugador2
                            + " no está registrado."
            );

            return;
        }


        // -------------------------
        // BUSCAR PARTIDA
        // -------------------------

        Partida partida =
                partidas.get(codigo);

        if (partida == null) {

            enviarRespuesta(
                    out,
                    "GAME_NOT_FOUND",
                    "La partida no existe."
            );

            return;
        }


        // -------------------------
        // COMPROBAR SI YA ESTÁ LLENA
        // -------------------------

        if (partida.jugador3 != null
                || partida.jugador4 != null) {

            enviarRespuesta(
                    out,
                    "GAME_FULL",
                    "La partida ya tiene 4 jugadores."
            );

            return;
        }


        // -------------------------
        // AGREGAR LOS DOS JUGADORES
        // -------------------------

        partida.jugador3 =
                jugador1;

        partida.jugador4 =
                jugador2;

        partida.computadoraInvitada =
                computadora;


        System.out.println(
                "Partida "
                        + codigo
                        + " completa:"
        );

        System.out.println(
                "  Jugador 1: "
                        + partida.jugador1
        );

        System.out.println(
                "  Jugador 2: "
                        + partida.jugador2
        );

        System.out.println(
                "  Jugador 3: "
                        + partida.jugador3
        );

        System.out.println(
                "  Jugador 4: "
                        + partida.jugador4
        );


        enviarRespuesta(
                out,
                "GAME_JOINED",
                "Partida unida correctamente."
        );

        // Ya estan los 4: arranca la partida en el servidor
        iniciarJuego(partida);
    }


    // =========================================================
    // COMPROBAR USUARIO
    // =========================================================

    private boolean usuarioRegistrado(
            String nombre
    ) {

        return usuarios.stream()
                .anyMatch(
                        usuario ->
                                usuario.equalsIgnoreCase(
                                        nombre
                                )
                );
    }


    // =========================================================
    // GENERAR CÓDIGO
    // =========================================================

    private String generarCodigo() {

        String codigo;

        do {

            int numero =
                    1000
                            + random.nextInt(9000);

            codigo =
                    String.valueOf(numero);

        } while (
                partidas.containsKey(codigo)
        );

        return codigo;
    }


    // =========================================================
    // RESPUESTA
    // =========================================================

    private void enviarRespuesta(
            ObjectOutputStream out,
            String tipo,
            String mensaje
    ) throws IOException {

        escribir(
                out,
                new Mensaje.Respuesta(
                        tipo,
                        mensaje
                )
        );
    }


    // =========================================================
    // ESCRIBIR A UN CLIENTE / A TODOS
    // =========================================================

    // Al mismo 'out' le pueden escribir dos hilos: el de esa computadora
    // (respuestas) y el del cajero (dados). synchronized(out) hace que
    // escriban de uno en uno; si no, los bytes se mezclan y el stream
    // se corrompe.
    void escribir(
            ObjectOutputStream out,
            Mensaje mensaje
    ) throws IOException {

        synchronized (out) {

            out.writeObject(mensaje);

            // ObjectOutputStream recuerda lo que ya mandó y a veces manda
            // solo una "referencia". reset() lo obliga a olvidar, así cada
            // EstadoJuego llega completo y con los datos nuevos.
            out.reset();

            out.flush();
        }
    }

    // Manda el mismo mensaje a las 2 computadoras
    private void difundir(
            Mensaje mensaje
    ) {

        for (ObjectOutputStream out : salidas) {

            if (out == null) {
                continue;
            }

            try {

                escribir(out, mensaje);

            } catch (IOException e) {

                // Si una computadora se cayó, la otra sigue recibiendo
                System.out.println(
                        "No se pudo avisar a una computadora: "
                                + e.getMessage()
                );
            }
        }
    }


    // =========================================================
    // PARTIDA: ARRANCAR
    // =========================================================

    // Se llama cuando la segunda computadora se une y ya hay 4 jugadores
    private void iniciarJuego(
            Partida partida
    ) {

        synchronized (gameLock) {

            juego = new Juego(Configuracion.MAX_TURNOS, Configuracion.CANTIDAD_JUGADORES, partida.codigo);

            // Un solo tablero: el mismo con el que Juego mueve y resuelve las casillas
            tablero = juego.getTablero();

            String[] nombres = {
                    partida.jugador1,
                    partida.jugador2,
                    partida.jugador3,
                    partida.jugador4
            };

            for (int i = 0; i < Configuracion.CANTIDAD_JUGADORES; i++) {

                jugadores[i] =
                        new Jugador("J" + (i + 1), nombres[i], Configuracion.SALDO_INICIAL);

                if (partida.tarjetas[i] != null) {
                    jugadores[i].registrarTarjeta(partida.tarjetas[i]);
                }

                juego.agregarJugador(jugadores[i]);

                // Jugadores 1 y 2 son de quien creó la partida, 3 y 4 de quien se unió
                computadoraDe[i] =
                        (i < 2)
                                ? partida.computadoraCreadora
                                : partida.computadoraInvitada;
            }

            yaTiro = false;
            difundir(new Mensaje.Aviso("Historial de transacciones: " + juego.getArchivoTransacciones()));

            System.out.println(
                    "Partida "
                            + partida.codigo
                            + " iniciada en el servidor."
            );

            difundir(new Mensaje.Aviso(
                    "¡Empieza la partida! Turno de "
                            + juego.getJugadorActual().getNombre()
                            + ". Tirá los dados en el cajero."
            ));

            difundir(crearEstado());
        }
    }


    // =========================================================
    // PARTIDA: DADOS (del cajero o de prueba)
    // =========================================================

    // Aplica una tirada al jugador en turno. Devuelve null si se usó,
    // o un código de error si no se podía tirar en este momento.
    // Hay que llamarlo teniendo gameLock.
    private String aplicarDados(
            int dado1,
            int dado2
    ) {

        if (juego == null) {
            return "NO_HAY_PARTIDA";
        }

        if (juego.esFinDeJuego()) {
            return "PARTIDA_TERMINADA";
        }

        if (yaTiro) {
            return "YA_TIRO";
        }

        if (dado1 < 1 || dado1 > 6
                || dado2 < 1 || dado2 > 6) {
            return "DADO_INVALIDO";
        }

        // Juego aplica todas las reglas: cárcel, movimiento, Salida,
        // cartas de Suerte / Arca Comunal y "Vaya a la cárcel".
        // Devuelve qué pasó, una línea por evento.
        Jugador quienTiro = juego.getJugadorActual();
        int indiceQuienTiro = indiceEnTurno();

        String resultado = juego.jugarTiro(dado1, dado2);

        yaTiro = true;

        // Primero los dados, para que los clientes los animen...
        difundir(new Mensaje.Dados(
                indiceQuienTiro,
                dado1,
                dado2
        ));

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

    // Cayó en una propiedad ajena: el alquiler se paga acercando la tarjeta.
    // Si el jugador no registró tarjeta, se cobra solo para no trabar la partida.
    // Hay que llamarlo teniendo gameLock.
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

    // Después de cobrar un alquiler: si el deudor quebró, se termina su turno
    // (o la partida). Hay que llamarlo teniendo gameLock.
    private void revisarSiQuebroAlPagar(Jugador deudor) {
        if (juego.esFinDeJuego()) {
            difundir(new Mensaje.Aviso(textoFinDePartida()));
        } else if (!deudor.isActivo()) {
            pasarTurno();
        }
    }

    void tirarDadosPrueba(
            int computadora,
            ObjectOutputStream out
    ) throws IOException {

        synchronized (gameLock) {

            String error = validarTurno(computadora);

            if (error != null) {
                escribir(out, new Mensaje.Aviso(error));
                return;
            }

            int[] dados = juego.lanzarDadosSoftware();

            String codigo = aplicarDados(dados[0], dados[1]);

            if (codigo != null) {
                escribir(out, new Mensaje.Aviso(
                        "No se pueden tirar los dados ahora (" + codigo + ")."
                ));
            }
        }
    }


    // =========================================================
    // PARTIDA: COMPRAR Y TERMINAR TURNO
    // =========================================================

    void comprarPropiedad(
            int computadora,
            ObjectOutputStream out
    ) throws IOException {

        synchronized (gameLock) {

            String error = validarTurno(computadora);

            if (error == null && !yaTiro) {
                error = "Primero hay que tirar los dados.";
            }
            if (error == null && juego.hayAlquilerPendiente()) {
                error = MENSAJE_ALQUILER_PENDIENTE;
            }
            if (error == null && jugadorPendientePago != null) {
                error = "Ya hay una compra esperando el pago con tarjeta.";
            }

            if (error != null) {
                escribir(out, new Mensaje.Aviso(error));
                return;
            }

            Jugador actual = juego.getJugadorActual();

            Casilla casilla =
                    tablero.obtenerCasilla(actual.getPosicionActual());

            if (!(casilla instanceof Propiedad propiedad)
                    || propiedad.tienePropietario()) {

                escribir(out, new Mensaje.Aviso(
                        "Esa casilla no se puede comprar."
                ));
                return;
            }

            if (actual.getSaldo() < propiedad.getPrecioCompra()) {

                escribir(out, new Mensaje.Aviso(
                        "Saldo insuficiente para comprar "
                                + propiedad.getNombre() + "."
                ));
                return;
            }

            if (!actual.tieneTarjetaRegistrada()) {
                escribir(out, new Mensaje.Aviso(
                        actual.getNombre() + " no tiene tarjeta RFID registrada."
                ));
                return;
            }

            jugadorPendientePago = actual;
            propiedadPendientePago = propiedad;
            computadoraPendientePago = computadora;
            tipoCompraPendiente = TipoCompra.PROPIEDAD;

            escribir(out, new Mensaje.Aviso(
                    actual.getNombre()
                            + ", acerque su tarjeta RFID para pagar ₡"
                            + propiedad.getPrecioCompra()
                            + " y comprar " + propiedad.getNombre() + "."
            ));
        }
    }

    void comprarCasa(int computadora, ObjectOutputStream out, int idPropiedad) throws IOException {
        comprarConstruccion(computadora, out, idPropiedad, false);
    }

    void comprarHotel(int computadora, ObjectOutputStream out, int idPropiedad) throws IOException {
        comprarConstruccion(computadora, out, idPropiedad, true);
    }

    private void comprarConstruccion(int computadora, ObjectOutputStream out, int idPropiedad, boolean hotel) throws IOException {
        synchronized (gameLock) {
            String error = validarTurno(computadora);
            if (error == null && juego.hayAlquilerPendiente()) error = MENSAJE_ALQUILER_PENDIENTE;
            if (error == null && jugadorPendientePago != null) error = "Ya hay una compra esperando el pago con tarjeta.";
            Propiedad propiedad = null;
            if (error == null) {
                if (idPropiedad < 0 || idPropiedad >= tablero.tamaño()
                        || !(tablero.obtenerCasilla(idPropiedad) instanceof Propiedad)) {
                    error = "Selecciona una propiedad válida.";
                } else {
                    propiedad = (Propiedad) tablero.obtenerCasilla(idPropiedad);
                    error = hotel ? juego.getBanco().motivoNoComprarHotel(juego.getJugadorActual(), propiedad)
                            : juego.getBanco().motivoNoComprarCasa(juego.getJugadorActual(), propiedad);
                }
            }
            if (error == null && !juego.getJugadorActual().tieneTarjetaRegistrada()) error = "El jugador no tiene tarjeta RFID registrada.";
            if (error != null) {
                escribir(out, new Mensaje.Aviso(error));
                if (juego != null) escribir(out, crearEstado());
                return;
            }
            jugadorPendientePago = juego.getJugadorActual();
            propiedadPendientePago = propiedad;
            computadoraPendientePago = computadora;
            tipoCompraPendiente = hotel ? TipoCompra.HOTEL : TipoCompra.CASA;
            escribir(out, new Mensaje.Aviso(jugadorPendientePago.getNombre()
                    + ", acerque su tarjeta RFID para pagar ₡" + (hotel ? propiedad.getPrecioHotel() : propiedad.getPrecioCasa())
                    + " y comprar " + (hotel ? "un hotel" : "una casa") + " en " + propiedad.getNombre() + "."));
        }
    }

    // Estando preso y antes de tirar: pagar la multa (conMulta) o usar la carta.
    // Si sale, tira normal después.
    void salirDeCarcel(
            int computadora,
            ObjectOutputStream out,
            boolean conMulta
    ) throws IOException {

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

            boolean salio = conMulta
                    ? juego.pagarMultaCarcel()
                    : juego.usarCartaSalirCarcel();

            if (!salio) {
                escribir(out, new Mensaje.Aviso(conMulta
                        ? "Saldo insuficiente para pagar la multa de ₡" + Configuracion.MULTA_CARCEL + "."
                        : actual.getNombre() + " no tiene cartas para salir de la cárcel."));
                return;
            }

            difundir(new Mensaje.Aviso(actual.getNombre()
                    + (conMulta
                        ? " paga ₡" + Configuracion.MULTA_CARCEL + " y sale de la cárcel."
                        : " usa su carta y sale de la cárcel.")
                    + " Ahora puede tirar los dados."));

            difundir(crearEstado());
        }
    }

    void terminarTurno(
            int computadora,
            ObjectOutputStream out
    ) throws IOException {

        synchronized (gameLock) {

            String error = validarTurno(computadora);

            if (error == null && !yaTiro) {
                error = "Todavía no se han tirado los dados.";
            }

            if (error == null && juego.hayAlquilerPendiente()) {
                error = MENSAJE_ALQUILER_PENDIENTE;
            }

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

        // Una compra esperando tarjeta no sobrevive al cambio de turno
        jugadorPendientePago = null;
        propiedadPendientePago = null;
        computadoraPendientePago = -1;
        tipoCompraPendiente = TipoCompra.PROPIEDAD;

        yaTiro = false;

        if (!avisos.isEmpty()) {
            difundir(new Mensaje.Aviso(avisos));
        }

        if (juego.esFinDeJuego()) {
            difundir(new Mensaje.Aviso(textoFinDePartida()));
        } else {
            difundir(new Mensaje.Aviso(
                    "Turno de "
                            + juego.getJugadorActual().getNombre()
                            + ". Tirá los dados en el cajero."
            ));
        }
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
    // PARTIDA: AYUDANTES
    // =========================================================

    // null si la computadora puede actuar; si no, el motivo
    private String validarTurno(
            int computadora
    ) {

        if (juego == null) {
            return "La partida todavía no empezó.";
        }

        if (juego.esFinDeJuego()) {
            return "La partida ya terminó.";
        }

        int enTurno = indiceEnTurno();

        if (computadoraDe[enTurno] != computadora) {
            return "No es tu turno: le toca a "
                    + jugadores[enTurno].getNombre() + ".";
        }

        return null;
    }

    // Posición (0 a 3) del jugador en turno dentro de 'jugadores'
    private int indiceEnTurno() {

        Jugador actual = juego.getJugadorActual();

        for (int i = 0; i < jugadores.length; i++) {

            if (jugadores[i] == actual) {
                return i;
            }
        }

        return 0;
    }

    // Foto de la partida que se manda a los clientes
    /** Consultar y exportar no cambian el juego y se permiten incluso al finalizar. */
    void consultarTransacciones(int computadora, ObjectOutputStream out,
            Mensaje.ConsultarTransacciones consulta, boolean exportacion) throws IOException {
        synchronized (gameLock) {
            boolean participante = false;
            for (int propietario : computadoraDe) {
                if (propietario == computadora) participante = true;
            }
            if (juego == null || !participante) {
                escribir(out, new Mensaje.Aviso("No hay una partida disponible para consultar."));
                return;
            }
            String jugador = consulta == null ? null : consulta.jugador();
            modelo.TipoTransaccion tipo = null;
            if (consulta != null && consulta.tipo() != null) {
                try {
                    tipo = modelo.TipoTransaccion.valueOf(consulta.tipo());
                } catch (IllegalArgumentException e) {
                    escribir(out, new Mensaje.Aviso("Tipo de transaccion desconocido."));
                    return;
                }
            }
            if (jugador != null && !modelo.Transaccion.ID_BANCO.equals(jugador)) {
                boolean existe = false;
                for (Jugador registrado : jugadores) {
                    if (registrado != null && registrado.getId().equals(jugador)) existe = true;
                }
                if (!existe) {
                    escribir(out, new Mensaje.Aviso("Jugador desconocido."));
                    return;
                }
            }
            modelo.RegistroTransacciones registro = juego.getRegistroTransacciones();
            String texto = exportacion ? registro.exportar()
                    : registro.consultar(jugador, tipo, consulta.recientesPrimero());
            escribir(out, new Mensaje.HistorialTransacciones(texto,
                    registro.getArchivo().getFileName().toString(), exportacion));
        }
    }

    private Mensaje.EstadoJuego crearEstado() {
        String errorHistorial = juego.getRegistroTransacciones().getErrorEscritura();
        if (errorHistorial != null) difundir(new Mensaje.Aviso(errorHistorial));

        String[] nombres = new String[Configuracion.CANTIDAD_JUGADORES];
        int[] saldos = new int[Configuracion.CANTIDAD_JUGADORES];
        int[] posiciones = new int[Configuracion.CANTIDAD_JUGADORES];
        boolean[] enCarcel = new boolean[Configuracion.CANTIDAD_JUGADORES];
        int[] cartasCarcel = new int[Configuracion.CANTIDAD_JUGADORES];
        boolean[] activos = new boolean[Configuracion.CANTIDAD_JUGADORES];
        int[] patrimonios = new int[Configuracion.CANTIDAD_JUGADORES];
        int ganador = -1;

        for (int i = 0; i < Configuracion.CANTIDAD_JUGADORES; i++) {

            nombres[i] = jugadores[i].getNombre();
            saldos[i] = jugadores[i].getSaldo();
            posiciones[i] = jugadores[i].getPosicionActual();
            enCarcel[i] = jugadores[i].isEnCarcel();
            cartasCarcel[i] = jugadores[i].getCantidadCartasSalirCarcel();
            activos[i] = jugadores[i].isActivo();
            patrimonios[i] = jugadores[i].calcularPatrimonio();
            if (jugadores[i] == juego.getGanador()) ganador = i;
        }

        Jugador actual = juego.getJugadorActual();
        Casilla casillaActual =
                tablero.obtenerCasilla(
                        actual.getPosicionActual()
                );

        boolean puedeComprar =
                yaTiro
                        && actual.isActivo()
                        && !juego.esFinDeJuego()
                        && casillaActual instanceof Propiedad propiedad
                        && !propiedad.tienePropietario();

        Mensaje.EstadoPropiedad[] propiedades = new Mensaje.EstadoPropiedad[tablero.tamaño()];
        for (int i = 0; i < tablero.tamaño(); i++) {
            if (tablero.obtenerCasilla(i) instanceof Propiedad p) {
                int propietario = -1;
                for (int j = 0; j < jugadores.length; j++) {
                    if (jugadores[j] == p.getPropietario()) propietario = j;
                }
                propiedades[i] = new Mensaje.EstadoPropiedad(i, propietario, p.getCantidadCasas(),
                        p.tieneHotel(), p.estaHipotecada(), p.calcularAlquiler(), p.getPrecioCasa(),
                        juego.getBanco().motivoNoComprarCasa(juego.getJugadorActual(), p), p.getPrecioHotel(),
                        juego.getBanco().motivoNoComprarHotel(juego.getJugadorActual(), p));
            }
        }

        return new Mensaje.EstadoJuego(
                juego.getTurnoActualGlobal(),
                juego.getMaxTurnos(),
                indiceEnTurno(),
                nombres,
                saldos,
                posiciones,
                computadoraDe.clone(),
                yaTiro,
                puedeComprar,
                juego.esFinDeJuego(),
                enCarcel,
                cartasCarcel,
                propiedades,
                juego.getBanco().getCasasDisponibles(),
                juego.getBanco().getHotelesDisponibles(),
                activos,
                patrimonios,
                ganador,
                juego.hayEmpate(),
                textoAlquilerPendiente()
        );
    }

    // "₡14 de alquiler a Beto por Liberia", o null si no se debe nada
    private String textoAlquilerPendiente() {
        Propiedad propiedad = juego.getAlquilerPendiente();
        if (propiedad == null) {
            return null;
        }
        return "₡" + propiedad.calcularAlquiler() + " de alquiler a "
                + propiedad.getPropietario().getNombre() + " por " + propiedad.getNombre();
    }


    // =========================================================
    // CAJERO (Pico 2W): SERVIDOR APARTE EN Configuracion.PUERTO_CAJERO
    // =========================================================

    private void iniciarServidorCajero() {

        // Hilo aparte: asi el accept() del cajero no bloquea
        // el accept() de las computadoras
        Thread hiloCajero = new Thread(() -> {

            try {

                servidorCajero = new ServerSocket(Configuracion.PUERTO_CAJERO);

                System.out.println(
                        "Esperando al cajero en el puerto "
                                + Configuracion.PUERTO_CAJERO
                );

                while (true) {

                    Socket socketCajero = servidorCajero.accept();

                    System.out.println(
                            "Cajero conectado desde "
                                    + socketCajero.getInetAddress()
                    );

                    // 'this' es este Servidor: el manejador lo usa
                    // para avisar cuando lleguen dados o tarjetas
                    new Thread(
                            new ManejadorCajero(socketCajero, this)
                    ).start();
                }

            } catch (IOException e) {

                // Tambien pasa al cerrar el servidor con detener(): es normal
                System.out.println(
                        "Servidor del cajero detenido: "
                                + e.getMessage()
                );
            }
        });

        hiloCajero.start();
    }


    // =========================================================
    // CAJERO: LLEGARON DADOS
    // =========================================================

    // Lo llama ManejadorCajero cada vez que la Pico manda DADOS;d1;d2.
    // Corre en el hilo del cajero, por eso se protege con gameLock.
    // Devuelve null si los dados se usaron, o un código de error
    // (NO_HAY_PARTIDA, YA_TIRO, ...) para contestarle a la Pico.
    public String recibirDadosDelCajero(
            int dado1,
            int dado2
    ) {

        synchronized (gameLock) {

            System.out.println(
                    "[CAJERO] Dados: "
                            + dado1
                            + " + "
                            + dado2
                            + " = "
                            + (dado1 + dado2)
            );

            String error = aplicarDados(dado1, dado2);

            if (error != null) {
                System.out.println("[CAJERO] Dados rechazados: " + error);
            }

            return error;
        }
    }


    // =========================================================
    // CAJERO: LLEGO UNA TARJETA
    // =========================================================

    // Lo llama ManejadorCajero cada vez que la Pico manda TARJETA;UID.
    // El UID viene en hexadecimal, ej: "43B121A1".
    public void recibirTarjetaDelCajero(
            String uid
    ) {

        synchronized (gameLock) {

            if (uid == null || uid.trim().isEmpty()) {
                return;
            }

            uid = uid.trim().toUpperCase();

            System.out.println("[CAJERO] Tarjeta: " + uid);

            // Primero: un alquiler pendiente. Es obligatorio, así que va antes
            // que cualquier compra que el jugador haya pedido.
            if (juego != null && !juego.esFinDeJuego() && juego.hayAlquilerPendiente()) {

                Jugador deudor = juego.getJugadorActual();

                if (!deudor.validarTarjeta(uid)) {
                    avisarComputadora(
                            computadoraDe[indiceEnTurno()],
                            "Tarjeta incorrecta. Debe pagar el alquiler "
                                    + deudor.getNombre() + "."
                    );
                    return;
                }

                difundir(new Mensaje.Aviso("💳 " + juego.pagarAlquilerPendiente()));
                revisarSiQuebroAlPagar(deudor);
                difundir(crearEstado());
                return;
            }

            // Segundo: si hay una compra esperando pago.
            if (jugadorPendientePago != null && propiedadPendientePago != null) {

                if (!jugadorPendientePago.validarTarjeta(uid)) {
                    avisarComputadora(
                            computadoraPendientePago,
                            "Tarjeta incorrecta. Debe pagar "
                                    + jugadorPendientePago.getNombre() + "."
                    );
                    return;
                }

                Jugador jugador = jugadorPendientePago;
                Propiedad propiedad = propiedadPendientePago;
                int computadora = computadoraPendientePago;
                TipoCompra compra = tipoCompraPendiente;

                jugadorPendientePago = null;
                propiedadPendientePago = null;
                computadoraPendientePago = -1;
                tipoCompraPendiente = TipoCompra.PROPIEDAD;

                if (juego.esFinDeJuego() || juego.getJugadorActual() != jugador
                        || !switch (compra) {
                            case CASA -> juego.getBanco().comprarCasa(jugador, propiedad);
                            case HOTEL -> juego.getBanco().comprarHotel(jugador, propiedad);
                            case PROPIEDAD -> juego.getBanco().comprarPropiedadConTarjeta(jugador, propiedad, uid);
                        }) {
                    avisarComputadora(computadora, "No se pudo realizar el pago.");
                    difundir(crearEstado());
                    return;
                }

                difundir(new Mensaje.Aviso(
                        "¡" + jugador.getNombre() + " pagó con su tarjeta y compró "
                                + switch (compra) {
                                    case CASA -> "una casa en ";
                                    case HOTEL -> "un hotel en ";
                                    case PROPIEDAD -> "";
                                } + propiedad.getNombre() + "!"
                ));
                difundir(crearEstado());
                return;
            }

            // Tercero: registro de tarjeta solicitado por una computadora.
            for (int computadora = 0; computadora < 2; computadora++) {
                int indiceLocal = jugadorLocalEsperandoTarjeta[computadora];
                if (indiceLocal == -1) continue;

                Partida partida = buscarPartidaDeComputadora(computadora);
                if (partida == null) {
                    jugadorLocalEsperandoTarjeta[computadora] = -1;
                    return;
                }

                int indiceGlobal =
                        (computadora == partida.computadoraCreadora)
                                ? indiceLocal
                                : 2 + indiceLocal;

                if (tarjetaUsadaEnPartida(partida, uid)) {
                    // Se sigue esperando: la computadora vuelve a pedir y se escanea otra
                    enviarResultadoTarjeta(computadora, indiceLocal, false, uid,
                            "Esta tarjeta ya pertenece a otro jugador. Escanee otra.");
                    return;
                }

                partida.tarjetas[indiceGlobal] = uid;

                if (jugadores[indiceGlobal] != null) {
                    jugadores[indiceGlobal].registrarTarjeta(uid);
                }

                jugadorLocalEsperandoTarjeta[computadora] = -1;

                enviarResultadoTarjeta(computadora, indiceLocal, true, uid,
                        "Tarjeta registrada correctamente.");
                return;
            }
        }
    }

    // Una computadora avisa que la próxima tarjeta que lea el cajero es de
    // su jugador local (0 o 1). Si la solicitud no se puede atender, se le
    // contesta de una vez con un ResultadoTarjeta sin uid.
    void solicitarRegistroTarjeta(
            int computadora,
            int indiceLocal,
            ObjectOutputStream out
    ) throws IOException {

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
            if (partida.computadoraCreadora == computadora
                    || partida.computadoraInvitada == computadora) {
                return partida;
            }
        }
        return null;
    }

    private boolean tarjetaUsadaEnPartida(Partida partida, String uid) {
        for (String tarjeta : partida.tarjetas) {
            if (tarjeta != null && tarjeta.equalsIgnoreCase(uid)) {
                return true;
            }
        }
        return false;
    }

    private void enviarResultadoTarjeta(
            int computadora, int indiceLocal, boolean registrada,
            String uid, String mensaje
    ) {
        if (computadora < 0 || computadora >= salidas.length || salidas[computadora] == null) return;
        try {
            escribir(salidas[computadora],
                    new Mensaje.ResultadoTarjeta(indiceLocal, registrada, uid, mensaje));
        } catch (IOException e) {
            System.out.println("No se pudo enviar confirmación RFID: " + e.getMessage());
        }
    }

    private void avisarComputadora(int computadora, String mensaje) {
        if (computadora < 0 || computadora >= salidas.length || salidas[computadora] == null) return;
        try {
            escribir(salidas[computadora], new Mensaje.Aviso(mensaje));
        } catch (IOException e) {
            System.out.println("No se pudo enviar aviso: " + e.getMessage());
        }
    }


    // =========================================================
    // CERRAR SERVIDOR
    // =========================================================

    public void detener()
            throws IOException {

        if (socketJuego != null
                && !socketJuego.isClosed()) {

            socketJuego.close();
        }

        if (servidorCajero != null
                && !servidorCajero.isClosed()) {

            servidorCajero.close();
        }

        hilosComputadoras.shutdownNow();
    }


    // =========================================================
    // PARTIDA
    // =========================================================

    private static class Partida {

        String codigo;

        String jugador1;
        String jugador2;

        String jugador3;
        String jugador4;

        // Desde que computadora se juegan jugador1-2 y jugador3-4
        int computadoraCreadora;
        int computadoraInvitada = -1;

        // UID de los jugadores 1, 2, 3 y 4.
        final String[] tarjetas = new String[4];


        Partida(
                String codigo,
                String jugador1,
                String jugador2
        ) {

            this.codigo =
                    codigo;

            this.jugador1 =
                    jugador1;

            this.jugador2 =
                    jugador2;

            this.jugador3 =
                    null;

            this.jugador4 =
                    null;
        }
    }


    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args
    ) {

        try {

            Servidor servidor =
                    new Servidor();

            servidor.iniciar();

        } catch (IOException e) {

            System.out.println(
                    "No se pudo iniciar el servidor."
            );

            e.printStackTrace();
        }
    }
}
