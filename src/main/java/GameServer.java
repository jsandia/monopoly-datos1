import protocolo.Message;
import modelo.Casilla;
import modelo.Juego;
import modelo.Jugador;
import modelo.Propiedad;
import modelo.Tablero;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {

    // No usar 5000: en macOS lo ocupa el Receptor AirPlay (ControlCenter)
    public static final int DEFAULT_PORT = 5100;

    // Puerto aparte para el cajero (Pico 2W). Habla texto, no objetos Java
    public static final int PUERTO_CAJERO = 5050;

    private final int port;

    private ServerSocket serverSocket;

    private ServerSocket servidorCajero;

    private final ExecutorService pool =
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

    // =========================================================
    // ESTADO DEL JUEGO ORIGINAL
    // =========================================================

    private double p0x = 0;
    private double p0y = 0;

    private double p1x = 0;
    private double p1y = 0;

    private int score0 = 0;
    private int score1 = 0;

    private final Object gameLock = new Object();


    // =========================================================
    // PARTIDA DE MONOPOLY (vive SOLO aqui, los clientes solo la dibujan)
    // =========================================================

    // Salida de cada computadora (indice = playerId). Sirve para
    // mandarles mensajes sin que los pidan: dados, avisos, estado.
    private final ObjectOutputStream[] salidas =
            new ObjectOutputStream[2];

    private static final int MAX_TURNOS = 100;
    private static final int SALDO_INICIAL = 1500; // como el Monopoly original

    // null mientras no se hayan unido las 2 computadoras
    private Juego juego;
    private Tablero tablero;

    // Los 4 jugadores en orden de turno. Juego los guarda en su cola,
    // pero no deja recorrerlos, y el estado necesita los 4.
    private final Jugador[] jugadores = new Jugador[4];

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
    private boolean pagoPendienteCasa;


    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    public GameServer() {
        this(DEFAULT_PORT);
    }

    public GameServer(int port) {
        this.port = port;
    }


    // =========================================================
    // INICIAR SERVIDOR
    // =========================================================

    public void start() throws IOException {

        serverSocket =
                new ServerSocket(port);

        System.out.println(
                "Servidor iniciado en puerto "
                        + port
        );

        Discovery.hostAnnounce(port);

        // CAJERO: se abre ANTES del ciclo de abajo, porque ese ciclo
        // se queda esperando en accept() hasta que lleguen las 2 computadoras
        iniciarServidorCajero();

        System.out.println(
                "Esperando 2 computadoras..."
        );

        int playerId = 0;

        while (playerId < 2) {

            Socket socket =
                    serverSocket.accept();

            int id =
                    playerId;

            System.out.println(
                    "Computadora "
                            + id
                            + " conectada: "
                            + socket.getInetAddress()
            );

            pool.submit(
                    () -> manejarCliente(socket, id)
            );

            playerId++;
        }

        System.out.println(
                "Ya hay 2 computadoras conectadas."
        );
    }


    // =========================================================
    // MANEJAR CLIENTE
    // =========================================================

    private void manejarCliente(
            Socket socket,
            int playerId
    ) {

        try {

            ObjectOutputStream out =
                    new ObjectOutputStream(
                            socket.getOutputStream()
                    );

            out.flush();

            ObjectInputStream in =
                    new ObjectInputStream(
                            socket.getInputStream()
                    );

            // Avisamos qué conexión recibió
            out.writeObject(
                    new Message.Welcome(playerId)
            );

            out.flush();

            // Desde aqui el servidor le puede escribir cuando quiera
            salidas[playerId] = out;


            while (true) {

                Object objeto =
                        in.readObject();

                if (!(objeto instanceof Message mensaje)) {
                    continue;
                }


                // =================================================
                // REGISTRAR
                // =================================================

                if (mensaje instanceof Message.Register registro) {

                    registrarUsuario(
                            registro.nombre(),
                            out
                    );
                }


                // =================================================
                // LOGIN
                // =================================================

                else if (mensaje instanceof Message.Login login) {

                    iniciarSesion(
                            login.nombre(),
                            out
                    );
                }


                // =================================================
                // CREAR PARTIDA
                // =================================================

                else if (mensaje instanceof Message.CreateGame crear) {

                    crearPartida(
                            crear.jugador1(),
                            crear.jugador2(),
                            playerId,
                            out
                    );
                }


                // =================================================
                // ENTRAR A PARTIDA
                // =================================================

                else if (mensaje instanceof Message.JoinGame entrar) {

                    entrarPartida(
                            entrar.codigo(),
                            entrar.jugador1(),
                            entrar.jugador2(),
                            playerId,
                            out
                    );
                }


                // =================================================
                // ACCIONES DE LA PARTIDA (el cliente solo pide)
                // =================================================

                else if (mensaje instanceof Message.TirarDadosPrueba) {

                    tirarDadosPrueba(playerId, out);
                }

                else if (mensaje instanceof Message.ComprarPropiedad) {

                    comprarPropiedad(playerId, out);
                }

                else if (mensaje instanceof Message.ComprarCasa compra) {
                    comprarCasa(playerId, out, compra.idPropiedad());
                }

                else if (mensaje instanceof Message.TerminarTurno) {

                    terminarTurno(playerId, out);
                }

                else if (mensaje instanceof Message.PagarMultaCarcel) {

                    salirDeCarcel(playerId, out, true);
                }

                else if (mensaje instanceof Message.UsarCartaCarcel) {

                    salirDeCarcel(playerId, out, false);
                }

                else if (mensaje instanceof Message.ConsultarTransacciones consulta) {
                    consultarTransacciones(playerId, out, consulta, false);
                }

                else if (mensaje instanceof Message.ExportarTransacciones) {
                    consultarTransacciones(playerId, out, null, true);
                }


                // =================================================
                // MOVIMIENTO ORIGINAL
                // =================================================

                else if (mensaje instanceof Message.PlayerMove movimiento) {

                    // Se reutiliza el mensaje existente para solicitar registro RFID
                    // sin modificar el protocolo cliente-servidor.
                    if (movimiento.action() != null
                            && movimiento.action().startsWith("RFID_REGISTRAR_")) {

                        solicitarRegistroTarjeta(
                                playerId,
                                movimiento.action(),
                                out
                        );

                    } else {

                        procesarMovimiento(
                                movimiento,
                                playerId,
                                out
                        );
                    }
                }


                // =================================================
                // PING
                // =================================================

                else if (mensaje instanceof Message.Ping ping) {

                    escribir(
                            out,
                            new Message.Pong(
                                    ping.timestamp()
                            )
                    );
                }
            }

        } catch (EOFException e) {

            System.out.println(
                    "Computadora "
                            + playerId
                            + " se desconectó."
            );

        } catch (Exception e) {

            System.out.println(
                    "Error con la computadora "
                            + playerId
            );

            e.printStackTrace();

        } finally {

            pool.submit(
                    () -> {
                        // El servidor sigue funcionando
                    }
            );
        }
    }


    // =========================================================
    // REGISTRAR USUARIO
    // =========================================================

    private void registrarUsuario(
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

    private void iniciarSesion(
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

    private void crearPartida(
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

    private void entrarPartida(
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
                new Message.Response(
                        tipo,
                        mensaje
                )
        );
    }


    // =========================================================
    // MOVIMIENTO ORIGINAL
    // =========================================================

    private void procesarMovimiento(
            Message.PlayerMove movimiento,
            int playerId,
            ObjectOutputStream out
    ) throws IOException {

        synchronized (gameLock) {

            if (playerId == 0) {

                p0x =
                        movimiento.x();

                p0y =
                        movimiento.y();

            } else {

                p1x =
                        movimiento.x();

                p1y =
                        movimiento.y();
            }

            if (movimiento.action() != null) {

                if (playerId == 0) {
                    score0++;
                } else {
                    score1++;
                }
            }


            Message.GameState estado =
                    new Message.GameState(
                            p0x,
                            p0y,
                            p1x,
                            p1y,
                            score0,
                            score1
                    );

            escribir(out, estado);
        }
    }


    // =========================================================
    // ESCRIBIR A UN CLIENTE / A TODOS
    // =========================================================

    // Al mismo 'out' le pueden escribir dos hilos: el de esa computadora
    // (respuestas) y el del cajero (dados). synchronized(out) hace que
    // escriban de uno en uno; si no, los bytes se mezclan y el stream
    // se corrompe.
    private void escribir(
            ObjectOutputStream out,
            Message mensaje
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
            Message mensaje
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

            juego = new Juego(MAX_TURNOS, 4, partida.codigo);

            // Un solo tablero: el mismo con el que Juego mueve y resuelve las casillas
            tablero = juego.getTablero();

            String[] nombres = {
                    partida.jugador1,
                    partida.jugador2,
                    partida.jugador3,
                    partida.jugador4
            };

            for (int i = 0; i < 4; i++) {

                jugadores[i] =
                        new Jugador("J" + (i + 1), nombres[i], SALDO_INICIAL);

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
            difundir(new Message.Aviso("Historial de transacciones: " + juego.getArchivoTransacciones()));

            System.out.println(
                    "Partida "
                            + partida.codigo
                            + " iniciada en el servidor."
            );

            difundir(new Message.Aviso(
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
        String resultado = juego.jugarTiro(dado1, dado2);

        yaTiro = true;

        // Primero los dados, para que los clientes los animen...
        difundir(new Message.Dados(
                indiceEnTurno(),
                dado1,
                dado2
        ));

        difundir(new Message.Aviso(resultado));

        // ...y después cómo quedó todo
        difundir(crearEstado());

        return null;
    }

    private void tirarDadosPrueba(
            int computadora,
            ObjectOutputStream out
    ) throws IOException {

        synchronized (gameLock) {

            String error = validarTurno(computadora);

            if (error != null) {
                escribir(out, new Message.Aviso(error));
                return;
            }

            int[] dados = juego.lanzarDadosSoftware();

            String codigo = aplicarDados(dados[0], dados[1]);

            if (codigo != null) {
                escribir(out, new Message.Aviso(
                        "No se pueden tirar los dados ahora (" + codigo + ")."
                ));
            }
        }
    }


    // =========================================================
    // PARTIDA: COMPRAR Y TERMINAR TURNO
    // =========================================================

    private void comprarPropiedad(
            int computadora,
            ObjectOutputStream out
    ) throws IOException {

        synchronized (gameLock) {

            String error = validarTurno(computadora);

            if (error == null && !yaTiro) {
                error = "Primero hay que tirar los dados.";
            }
            if (error == null && jugadorPendientePago != null) {
                error = "Ya hay una compra esperando el pago con tarjeta.";
            }

            if (error != null) {
                escribir(out, new Message.Aviso(error));
                return;
            }

            Jugador actual = juego.getJugadorActual();

            Casilla casilla =
                    tablero.obtenerCasilla(actual.getPosicionActual());

            if (!(casilla instanceof Propiedad propiedad)
                    || propiedad.tienePropietario()) {

                escribir(out, new Message.Aviso(
                        "Esa casilla no se puede comprar."
                ));
                return;
            }

            if (actual.getSaldo() < propiedad.getPrecioCompra()) {

                escribir(out, new Message.Aviso(
                        "Saldo insuficiente para comprar "
                                + propiedad.getNombre() + "."
                ));
                return;
            }

            if (!actual.tieneTarjetaRegistrada()) {
                escribir(out, new Message.Aviso(
                        actual.getNombre() + " no tiene tarjeta RFID registrada."
                ));
                return;
            }

            jugadorPendientePago = actual;
            propiedadPendientePago = propiedad;
            computadoraPendientePago = computadora;
            pagoPendienteCasa = false;

            escribir(out, new Message.Aviso(
                    actual.getNombre()
                            + ", acerque su tarjeta RFID para pagar ₡"
                            + propiedad.getPrecioCompra()
                            + " y comprar " + propiedad.getNombre() + "."
            ));
        }
    }

    private void comprarCasa(int computadora, ObjectOutputStream out, int idPropiedad) throws IOException {
        synchronized (gameLock) {
            String error = validarTurno(computadora);
            if (error == null && jugadorPendientePago != null) error = "Ya hay una compra esperando el pago con tarjeta.";
            Propiedad propiedad = null;
            if (error == null) {
                if (idPropiedad < 0 || idPropiedad >= tablero.tamaño()
                        || !(tablero.obtenerCasilla(idPropiedad) instanceof Propiedad)) {
                    error = "Selecciona una propiedad válida.";
                } else {
                    propiedad = (Propiedad) tablero.obtenerCasilla(idPropiedad);
                    error = juego.getBanco().motivoNoComprarCasa(juego.getJugadorActual(), propiedad);
                }
            }
            if (error == null && !juego.getJugadorActual().tieneTarjetaRegistrada()) error = "El jugador no tiene tarjeta RFID registrada.";
            if (error != null) {
                escribir(out, new Message.Aviso(error));
                if (juego != null) escribir(out, crearEstado());
                return;
            }
            jugadorPendientePago = juego.getJugadorActual();
            propiedadPendientePago = propiedad;
            computadoraPendientePago = computadora;
            pagoPendienteCasa = true;
            escribir(out, new Message.Aviso(jugadorPendientePago.getNombre()
                    + ", acerque su tarjeta RFID para pagar ₡" + propiedad.getPrecioCasa()
                    + " y comprar una casa en " + propiedad.getNombre() + "."));
        }
    }

    // Estando preso y antes de tirar: pagar la multa (conMulta) o usar la carta.
    // Si sale, tira normal después.
    private void salirDeCarcel(
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
                escribir(out, new Message.Aviso(error));
                return;
            }

            boolean salio = conMulta
                    ? juego.pagarMultaCarcel()
                    : juego.usarCartaSalirCarcel();

            if (!salio) {
                escribir(out, new Message.Aviso(conMulta
                        ? "Saldo insuficiente para pagar la multa de ₡" + Juego.MULTA_CARCEL + "."
                        : actual.getNombre() + " no tiene cartas para salir de la cárcel."));
                return;
            }

            difundir(new Message.Aviso(actual.getNombre()
                    + (conMulta
                        ? " paga ₡" + Juego.MULTA_CARCEL + " y sale de la cárcel."
                        : " usa su carta y sale de la cárcel.")
                    + " Ahora puede tirar los dados."));

            difundir(crearEstado());
        }
    }

    private void terminarTurno(
            int computadora,
            ObjectOutputStream out
    ) throws IOException {

        synchronized (gameLock) {

            String error = validarTurno(computadora);

            if (error == null && !yaTiro) {
                error = "Todavía no se han tirado los dados.";
            }

            if (error != null) {
                escribir(out, new Message.Aviso(error));
                return;
            }

            juego.terminarTurnoActual();

            jugadorPendientePago = null;
            propiedadPendientePago = null;
            computadoraPendientePago = -1;
            pagoPendienteCasa = false;

            yaTiro = false;

            if (juego.esFinDeJuego()) {

                difundir(new Message.Aviso(
                        "¡La partida ha finalizado!"
                ));

            } else {

                difundir(new Message.Aviso(
                        "Turno de "
                                + juego.getJugadorActual().getNombre()
                                + ". Tirá los dados en el cajero."
                ));
            }

            difundir(crearEstado());
        }
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
    private void consultarTransacciones(int computadora, ObjectOutputStream out,
            Message.ConsultarTransacciones consulta, boolean exportacion) throws IOException {
        synchronized (gameLock) {
            boolean participante = false;
            for (int propietario : computadoraDe) {
                if (propietario == computadora) participante = true;
            }
            if (juego == null || !participante) {
                escribir(out, new Message.Aviso("No hay una partida disponible para consultar."));
                return;
            }
            String jugador = consulta == null ? null : consulta.jugador();
            modelo.TipoTransaccion tipo = null;
            if (consulta != null && consulta.tipo() != null) {
                try {
                    tipo = modelo.TipoTransaccion.valueOf(consulta.tipo());
                } catch (IllegalArgumentException e) {
                    escribir(out, new Message.Aviso("Tipo de transaccion desconocido."));
                    return;
                }
            }
            if (jugador != null && !modelo.Transaccion.ID_BANCO.equals(jugador)) {
                boolean existe = false;
                for (Jugador registrado : jugadores) {
                    if (registrado != null && registrado.getId().equals(jugador)) existe = true;
                }
                if (!existe) {
                    escribir(out, new Message.Aviso("Jugador desconocido."));
                    return;
                }
            }
            modelo.RegistroTransacciones registro = juego.getRegistroTransacciones();
            String texto = exportacion ? registro.exportar()
                    : registro.consultar(jugador, tipo, consulta.recientesPrimero());
            escribir(out, new Message.HistorialTransacciones(texto,
                    registro.getArchivo().getFileName().toString(), exportacion));
        }
    }

    private Message.EstadoJuego crearEstado() {
        String errorHistorial = juego.getRegistroTransacciones().getErrorEscritura();
        if (errorHistorial != null) difundir(new Message.Aviso(errorHistorial));

        String[] nombres = new String[4];
        int[] saldos = new int[4];
        int[] posiciones = new int[4];
        boolean[] enCarcel = new boolean[4];
        int[] cartasCarcel = new int[4];

        for (int i = 0; i < 4; i++) {

            nombres[i] = jugadores[i].getNombre();
            saldos[i] = jugadores[i].getSaldo();
            posiciones[i] = jugadores[i].getPosicionActual();
            enCarcel[i] = jugadores[i].isEnCarcel();
            cartasCarcel[i] = jugadores[i].getCantidadCartasSalirCarcel();
        }

        Casilla casillaActual =
                tablero.obtenerCasilla(
                        juego.getJugadorActual().getPosicionActual()
                );

        boolean puedeComprar =
                yaTiro
                        && casillaActual instanceof Propiedad propiedad
                        && !propiedad.tienePropietario();

        Message.EstadoPropiedad[] propiedades = new Message.EstadoPropiedad[tablero.tamaño()];
        for (int i = 0; i < tablero.tamaño(); i++) {
            if (tablero.obtenerCasilla(i) instanceof Propiedad p) {
                int propietario = -1;
                for (int j = 0; j < jugadores.length; j++) {
                    if (jugadores[j] == p.getPropietario()) propietario = j;
                }
                propiedades[i] = new Message.EstadoPropiedad(i, propietario, p.getCantidadCasas(),
                        p.tieneHotel(), p.estaHipotecada(), p.calcularAlquiler(), p.getPrecioCasa(),
                        juego.getBanco().motivoNoComprarCasa(juego.getJugadorActual(), p));
            }
        }

        return new Message.EstadoJuego(
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
                juego.getBanco().getCasasDisponibles()
        );
    }


    // =========================================================
    // CAJERO (Pico 2W): SERVIDOR APARTE EN EL PUERTO 5050
    // =========================================================

    private void iniciarServidorCajero() {

        // Hilo aparte: asi el accept() del cajero no bloquea
        // el accept() de las computadoras
        Thread hiloCajero = new Thread(() -> {

            try {

                servidorCajero = new ServerSocket(PUERTO_CAJERO);

                System.out.println(
                        "Esperando al cajero en el puerto "
                                + PUERTO_CAJERO
                );

                while (true) {

                    Socket socketCajero = servidorCajero.accept();

                    System.out.println(
                            "Cajero conectado desde "
                                    + socketCajero.getInetAddress()
                    );

                    // 'this' es este GameServer: el manejador lo usa
                    // para avisar cuando lleguen dados o tarjetas
                    new Thread(
                            new ManejadorCajero(socketCajero, this)
                    ).start();
                }

            } catch (IOException e) {

                // Tambien pasa al cerrar el servidor con stop(): es normal
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

            // Primero: si hay una compra esperando pago.
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
                boolean compraCasa = pagoPendienteCasa;

                jugadorPendientePago = null;
                propiedadPendientePago = null;
                computadoraPendientePago = -1;
                pagoPendienteCasa = false;

                if (juego.esFinDeJuego() || juego.getJugadorActual() != jugador
                        || !(compraCasa ? juego.getBanco().comprarCasa(jugador, propiedad)
                        : juego.getBanco().comprarPropiedadConTarjeta(jugador, propiedad, uid))) {
                    avisarComputadora(computadora, "No se pudo realizar el pago.");
                    difundir(crearEstado());
                    return;
                }

                difundir(new Message.Aviso(
                        "¡" + jugador.getNombre() + " pagó con su tarjeta y compró "
                                + (compraCasa ? "una casa en " : "") + propiedad.getNombre() + "!"
                ));
                difundir(crearEstado());
                return;
            }

            // Segundo: registro de tarjeta solicitado por una computadora.
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
                    enviarAvisoRFID(computadora, indiceLocal, false, uid,
                            "Esta tarjeta ya pertenece a otro jugador. Escanee otra.");
                    return;
                }

                partida.tarjetas[indiceGlobal] = uid;

                if (jugadores[indiceGlobal] != null) {
                    jugadores[indiceGlobal].registrarTarjeta(uid);
                }

                jugadorLocalEsperandoTarjeta[computadora] = -1;

                enviarAvisoRFID(computadora, indiceLocal, true, uid,
                        "Tarjeta registrada correctamente.");
                return;
            }
        }
    }

    private void solicitarRegistroTarjeta(
            int computadora,
            String accion,
            ObjectOutputStream out
    ) throws IOException {

        int indiceLocal;
        try {
            indiceLocal = Integer.parseInt(
                    accion.substring("RFID_REGISTRAR_".length())
            );
        } catch (Exception e) {
            escribir(out, new Message.Aviso("__RFID_ERROR__;Solicitud RFID inválida."));
            return;
        }

        if (indiceLocal < 0 || indiceLocal > 1) {
            escribir(out, new Message.Aviso("__RFID_ERROR__;Jugador RFID inválido."));
            return;
        }

        if (buscarPartidaDeComputadora(computadora) == null) {
            escribir(out, new Message.Aviso("__RFID_ERROR__;No hay una partida para registrar la tarjeta."));
            return;
        }

        jugadorLocalEsperandoTarjeta[computadora] = indiceLocal;
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

    private void enviarAvisoRFID(
            int computadora, int indiceLocal, boolean correcta,
            String uid, String mensaje
    ) {
        if (computadora < 0 || computadora >= salidas.length || salidas[computadora] == null) return;
        try {
            escribir(salidas[computadora], new Message.Aviso(
                    "__RFID__;" + indiceLocal + ";" + correcta + ";" + uid + ";" + mensaje
            ));
        } catch (IOException e) {
            System.out.println("No se pudo enviar confirmación RFID: " + e.getMessage());
        }
    }

    private void avisarComputadora(int computadora, String mensaje) {
        if (computadora < 0 || computadora >= salidas.length || salidas[computadora] == null) return;
        try {
            escribir(salidas[computadora], new Message.Aviso(mensaje));
        } catch (IOException e) {
            System.out.println("No se pudo enviar aviso: " + e.getMessage());
        }
    }


    // =========================================================
    // CERRAR SERVIDOR
    // =========================================================

    public void stop()
            throws IOException {

        if (serverSocket != null
                && !serverSocket.isClosed()) {

            serverSocket.close();
        }

        if (servidorCajero != null
                && !servidorCajero.isClosed()) {

            servidorCajero.close();
        }

        pool.shutdownNow();
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

            GameServer servidor =
                    new GameServer();

            servidor.start();

        } catch (IOException e) {

            System.out.println(
                    "No se pudo iniciar el servidor."
            );

            e.printStackTrace();
        }
    }
}
