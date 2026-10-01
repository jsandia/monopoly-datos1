import modelo.Juego;
import modelo.Jugador;
import protocolo.Mensaje;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Registro de tarjetas RFID antes de jugar: una computadora pide registrar
 * a uno de sus dos jugadores, el cajero lee una tarjeta y el servidor le
 * contesta a esa computadora.
 *
 * Los escenarios no dependen de cómo viajan los mensajes: eso lo resuelven
 * pedirRegistro() y resultados().
 */
class ServidorTarjetasTest {

    /** Lo que la computadora se entera de un registro, sin importar el formato del mensaje. */
    private record Resultado(int jugadorLocal, boolean registrada, String uid, String mensaje) {}

    private Servidor servidor;
    private final ByteArrayOutputStream[] bytes = new ByteArrayOutputStream[2];
    private final ObjectOutputStream[] salidas = new ObjectOutputStream[2];

    // =========================================================
    // AYUDANTES: lo único que depende del formato de los mensajes
    // =========================================================

    private void pedirRegistro(int computadora, int jugadorLocal) throws Exception {
        invocar("solicitarRegistroTarjeta", new Class<?>[]{int.class, int.class, ObjectOutputStream.class},
                computadora, jugadorLocal, salidas[computadora]);
    }

    /** Todos los resultados de registro que recibió la computadora hasta ahora. */
    private List<Resultado> resultados(int computadora) throws Exception {
        List<Resultado> encontrados = new ArrayList<>();
        for (Mensaje mensaje : recibidos(computadora)) {
            if (mensaje instanceof Mensaje.ResultadoTarjeta resultado) {
                encontrados.add(new Resultado(resultado.jugadorLocal(), resultado.registrada(),
                        resultado.uid(), resultado.mensaje()));
            }
        }
        return encontrados;
    }

    // =========================================================
    // PREPARACIÓN
    // =========================================================

    private Object invocar(String nombre, Class<?>[] tipos, Object... argumentos) throws Exception {
        Method metodo = Servidor.class.getDeclaredMethod(nombre, tipos);
        metodo.setAccessible(true);
        return metodo.invoke(servidor, argumentos);
    }

    private Object campo(String nombre) throws Exception {
        Field campo = Servidor.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        return campo.get(servidor);
    }

    private List<Mensaje> recibidos(int computadora) throws Exception {
        salidas[computadora].flush();
        List<Mensaje> mensajes = new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes[computadora].toByteArray()))) {
            while (true) mensajes.add((Mensaje) in.readObject());
        } catch (EOFException fin) {
            return mensajes;
        }
    }

    @BeforeEach void preparar() throws Exception {
        servidor = new Servidor();
        ObjectOutputStream[] salidasServidor = (ObjectOutputStream[]) campo("salidas");
        for (int i = 0; i < 2; i++) {
            bytes[i] = new ByteArrayOutputStream();
            salidas[i] = new ObjectOutputStream(bytes[i]);
            salidasServidor[i] = salidas[i];
        }
        for (String nombre : new String[]{"Ana", "Beto", "Caro", "Dani"}) {
            invocar("registrarUsuario", new Class<?>[]{String.class, ObjectOutputStream.class}, nombre, salidas[0]);
        }
    }

    @AfterEach void limpiar() throws Exception {
        Juego juego = (Juego) campo("juego");
        if (juego != null) Files.deleteIfExists(juego.getArchivoTransacciones());
    }

    private void crearPartida() throws Exception {
        invocar("crearPartida", new Class<?>[]{String.class, String.class, int.class, ObjectOutputStream.class},
                "Ana", "Beto", 0, salidas[0]);
    }

    private void unirsePartida() throws Exception {
        String codigo = ((Map<?, ?>) campo("partidas")).keySet().iterator().next().toString();
        invocar("entrarPartida", new Class<?>[]{String.class, String.class, String.class, int.class, ObjectOutputStream.class},
                codigo, "Caro", "Dani", 1, salidas[1]);
    }

    private Jugador jugador(int indice) throws Exception {
        return ((Jugador[]) campo("jugadores"))[indice];
    }

    // =========================================================
    // ESCENARIOS
    // =========================================================

    @Test void elCreadorRegistraASusDosJugadoresAntesDeQueEmpiece() throws Exception {
        crearPartida();

        pedirRegistro(0, 0);
        servidor.recibirTarjetaDelCajero("aaa111");
        pedirRegistro(0, 1);
        servidor.recibirTarjetaDelCajero("BBB222");

        assertEquals(List.of(
                new Resultado(0, true, "AAA111", "Tarjeta registrada correctamente."),
                new Resultado(1, true, "BBB222", "Tarjeta registrada correctamente.")
        ), resultados(0));

        // Al arrancar la partida, los jugadores 1 y 2 ya traen su tarjeta
        unirsePartida();
        assertEquals("AAA111", jugador(0).getUidTarjeta());
        assertEquals("BBB222", jugador(1).getUidTarjeta());
    }

    @Test void elInvitadoRegistraASusJugadoresConLaPartidaYaEmpezada() throws Exception {
        crearPartida();
        unirsePartida();

        pedirRegistro(1, 0);
        servidor.recibirTarjetaDelCajero("CCC333");
        pedirRegistro(1, 1);
        servidor.recibirTarjetaDelCajero("DDD444");

        assertEquals(2, resultados(1).size());
        assertTrue(resultados(0).isEmpty()); // a la otra computadora no le llega nada
        assertEquals("CCC333", jugador(2).getUidTarjeta());
        assertEquals("DDD444", jugador(3).getUidTarjeta());
    }

    @Test void unaTarjetaRepetidaSeRechazaYSeSigueEsperandoOtra() throws Exception {
        crearPartida();
        pedirRegistro(0, 0);
        servidor.recibirTarjetaDelCajero("AAA111");

        pedirRegistro(0, 1);
        servidor.recibirTarjetaDelCajero("AAA111"); // la misma de Ana
        servidor.recibirTarjetaDelCajero("BBB222"); // sigue esperando: esta sí

        List<Resultado> lista = resultados(0);
        assertEquals(3, lista.size());
        assertEquals(new Resultado(1, false, "AAA111",
                "Esta tarjeta ya pertenece a otro jugador. Escanee otra."), lista.get(1));
        assertEquals(new Resultado(1, true, "BBB222", "Tarjeta registrada correctamente."), lista.get(2));
    }

    @Test void unaTarjetaSinSolicitudNoRegistraNada() throws Exception {
        crearPartida();
        servidor.recibirTarjetaDelCajero("AAA111");
        assertTrue(resultados(0).isEmpty());
    }

    @Test void pedirRegistroSinPartidaAvisaElErrorSinRegistrar() throws Exception {
        pedirRegistro(0, 0);

        List<Resultado> lista = resultados(0);
        assertEquals(1, lista.size());
        assertFalse(lista.get(0).registrada());
        assertNull(lista.get(0).uid());
        assertEquals("No hay una partida para registrar la tarjeta.", lista.get(0).mensaje());

        // Y una tarjeta que llegue después no se registra en ningún lado
        servidor.recibirTarjetaDelCajero("AAA111");
        assertEquals(1, resultados(0).size());
    }

    @Test void unJugadorLocalInvalidoSeRechaza() throws Exception {
        crearPartida();
        pedirRegistro(0, 2);

        List<Resultado> lista = resultados(0);
        assertEquals(1, lista.size());
        assertFalse(lista.get(0).registrada());
        assertEquals("Jugador RFID inválido.", lista.get(0).mensaje());
    }
}
