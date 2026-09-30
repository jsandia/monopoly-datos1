import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Atiende la conexion con el cajero (Raspberry Pi Pico 2W).
// La Pico manda texto, una linea por mensaje, con partes separadas por ';':
//   HOLA;CAJERO        -> saludo al conectarse
//   DADOS;d1;d2        -> tirada de dos dados
//   TARJETA;UID        -> tarjeta RFID leida (UID en hexadecimal)
// Cada dato que llega se le pasa a GameServer.
public class ManejadorCajero implements Runnable {

    private final Socket socketCajero;
    private final GameServer servidor;

    public ManejadorCajero(Socket socketCajero, GameServer servidor) {
        this.socketCajero = socketCajero;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        // try-with-resources: al salir se cierran el socket, el lector y el escritor
        try (Socket socket = socketCajero;
             BufferedReader lector = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true)) {

            String linea;
            // readLine() espera hasta que llegue una linea completa (terminada en \n).
            // Devuelve null cuando la Pico cierra la conexion.
            while ((linea = lector.readLine()) != null) {
                procesarMensaje(linea.trim(), escritor);
            }
            System.out.println("El cajero se desconecto.");

        } catch (IOException e) {
            // "Connection reset" es normal cuando la Pico se reinicia de golpe
            System.out.println("Error con el cajero: " + e.getMessage());
        }
    }

    private void procesarMensaje(String linea, PrintWriter escritor) {
        System.out.println("Cajero -> " + linea);
        String[] partes = linea.split(";");

        switch (partes[0]) {
            case "HOLA" -> escritor.println("BIENVENIDO");

            case "DADOS" -> {
                if (partes.length < 3) {
                    escritor.println("ERROR;FORMATO_DADOS");
                    return;
                }
                try {
                    int dado1 = Integer.parseInt(partes[1]);
                    int dado2 = Integer.parseInt(partes[2]);
                    // null = el servidor movió al jugador en turno con estos dados
                    String error = servidor.recibirDadosDelCajero(dado1, dado2);
                    escritor.println(error == null ? "OK_DADOS" : "ERROR;" + error);
                } catch (NumberFormatException e) {
                    // Por si llega algo como "DADOS;x;4"
                    escritor.println("ERROR;FORMATO_DADOS");
                }
            }

            case "TARJETA" -> {
                if (partes.length < 2) {
                    escritor.println("ERROR;FORMATO_TARJETA");
                    return;
                }

                String uid = partes[1].trim();

                servidor.recibirTarjetaDelCajero(uid);
                escritor.println("OK_TARJETA");
            }

            default -> System.out.println("Mensaje desconocido del cajero: " + linea);
        }
    }
}
