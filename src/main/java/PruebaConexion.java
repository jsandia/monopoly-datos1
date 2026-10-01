import config.Configuracion;
import protocolo.Mensaje;
import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Prueba de conexión independiente del juego. Sirve para confirmar,
 * antes de meterte con la lógica del juego, que las 2 computadoras
 * se encuentran solas en la red y que la conexión funciona bien.
 *
 * Uso:
 *   En UNA computadora:  java PruebaConexion servidor
 *   En la OTRA:          java PruebaConexion cliente
 *
 * También se puede correr SIN argumentos (por ejemplo, apretando ▶️
 * directo en IntelliJ sin configurar nada): en ese caso pregunta por
 * consola si es servidor o cliente.
 *
 * El cliente descubre al servidor solo (mismo mecanismo de Descubrimiento.java
 * que usa el juego), se conecta, y hace 5 "pings" midiendo cuánto tarda
 * cada ida y vuelta. Si ves los 5 tiempos y el promedio final, la
 * conexión está funcionando correctamente.
 */
public class PruebaConexion {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        String modo = (args.length > 0) ? args[0] : preguntarModo();

        switch (modo) {
            case "servidor" -> ejecutarServidor();
            case "cliente"  -> ejecutarCliente();
            default         -> System.out.println("Argumento inválido. Usá 'servidor' o 'cliente'.");
        }
    }

    /** Pregunta por consola si esta computadora es el servidor o el cliente (cuando no se pasó como argumento). */
    private static String preguntarModo() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("¿Esta computadora es el servidor o el cliente? (servidor/cliente): ");
            String respuesta = scanner.nextLine().trim().toLowerCase();
            if (respuesta.equals("servidor") || respuesta.equals("cliente")) {
                return respuesta;
            }
            System.out.println("Escribí exactamente 'servidor' o 'cliente'.");
        }
    }

    private static void ejecutarServidor() throws IOException, ClassNotFoundException {
        try (ServerSocket serverSocket = new ServerSocket(Configuracion.PUERTO_PRUEBA_CONEXION)) {
            System.out.println("Anunciándose en la red y esperando a la otra computadora...");
            Descubrimiento.anunciarServidor(Configuracion.PUERTO_PRUEBA_CONEXION);

            try (Socket socket = serverSocket.accept()) {
                System.out.println("¡Conexión recibida desde " + socket.getInetAddress() + "!");
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out.flush();

                System.out.println("Respondiendo pings de prueba...");
                while (true) {
                    Mensaje mensaje = (Mensaje) in.readObject();
                    if (mensaje instanceof Mensaje.Ping ping) {
                        out.writeObject(new Mensaje.Pong(ping.enviadoMs()));
                        out.flush();
                    }
                }
            } catch (EOFException e) {
                System.out.println("La otra computadora cerró la conexión. Prueba terminada correctamente.");
            }
        }
    }

    private static void ejecutarCliente() throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("Buscando la otra computadora en la red...");

        Descubrimiento.InfoServidor infoServidor;
        try {
            infoServidor = Descubrimiento.buscarServidor(Configuracion.TIEMPO_DESCUBRIMIENTO_MS);
        } catch (IOException e) {
            System.out.println("No se encontró ninguna computadora anunciándose.");
            System.out.println("¿Corriste 'java PruebaConexion servidor' en la otra máquina? ¿Están en la misma red WiFi?");
            return;
        }

        System.out.println("Encontrada en " + infoServidor.direccion().getHostAddress() + ":" + infoServidor.puerto());

        try (Socket socket = new Socket(infoServidor.direccion(), infoServidor.puerto())) {
            System.out.println("Conexión TCP establecida correctamente.");
            System.out.println("Midiendo latencia con 5 pings...\n");

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out.flush();

            long totalMs = 0;
            int cantidad = 5;
            for (int i = 1; i <= cantidad; i++) {
                long inicio = System.currentTimeMillis();
                out.writeObject(new Mensaje.Ping(inicio));
                out.flush();

                Mensaje.Pong pong = (Mensaje.Pong) in.readObject();
                long idaYVuelta = System.currentTimeMillis() - pong.enviadoMs();
                totalMs += idaYVuelta;
                System.out.println("Ping " + i + ": " + idaYVuelta + " ms");
                Thread.sleep(300);
            }

            System.out.println("\n--- Resultado ---");
            System.out.println("Conexión OK. Latencia promedio: " + (totalMs / cantidad) + " ms");
            System.out.println("(por WiFi local, algo entre 1-20 ms es normal; más de eso puede indicar interferencia)");
        }
    }
}