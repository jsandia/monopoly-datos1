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
                            out
                    );
                }


                // =================================================
                // MOVIMIENTO ORIGINAL
                // =================================================

                else if (mensaje instanceof Message.PlayerMove movimiento) {

                    procesarMovimiento(
                            movimiento,
                            playerId,
                            out
                    );
                }


                // =================================================
                // PING
                // =================================================

                else if (mensaje instanceof Message.Ping ping) {

                    out.writeObject(
                            new Message.Pong(
                                    ping.timestamp()
                            )
                    );

                    out.flush();
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

        out.writeObject(
                new Message.Response(
                        tipo,
                        mensaje
                )
        );

        out.flush();
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

            out.writeObject(estado);

            out.flush();
        }
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
    public void recibirDadosDelCajero(
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

            // AQUI: llamar el metodo que ya tienen para usar los dados.
            // Ejemplo: moverJugadorEnTurno(dado1 + dado2);
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

            System.out.println(
                    "[CAJERO] Tarjeta: "
                            + uid
            );

            // AQUI: llamar el metodo que ya tienen para usar la tarjeta.
            // Ejemplo: procesarTarjeta(uid);
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