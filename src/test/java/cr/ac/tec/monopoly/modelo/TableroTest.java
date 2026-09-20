package cr.ac.tec.monopoly.modelo;

import cr.ac.tec.monopoly.estructuras.Nodo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableroTest {

    private final Tablero tablero = new Tablero();

    @Test
    void tiene40Casillas() {
        assertEquals(40, tablero.tamaño());
    }

    @Test
    void losIdsCoincidenConLaPosicion() {
        for (int i = 0; i < 40; i++) {
            assertEquals(i, tablero.obtenerCasilla(i).getId());
        }
    }

    @Test
    void laPrimeraEsSalida() {
        Casilla c = tablero.obtenerCasilla(0);
        assertInstanceOf(CasillaEspecial.class, c);
        assertEquals(CasillaEspecial.Tipo.SALIDA, ((CasillaEspecial) c).getTipo());
    }

    @Test
    void lasCuatroEsquinasSonEspeciales() {
        assertEquals(CasillaEspecial.Tipo.SALIDA, tipoEspecial(0));
        assertEquals(CasillaEspecial.Tipo.CARCEL, tipoEspecial(10));
        assertEquals(CasillaEspecial.Tipo.PARQUEO_LIBRE, tipoEspecial(20));
        assertEquals(CasillaEspecial.Tipo.IR_A_CARCEL, tipoEspecial(30));
    }

    @Test
    void laUltimaEsSantaAnaYVuelveASalida() {
        Nodo<Casilla> primero = tablero.getPrimerNodo();
        Casilla ultima = primero.getAnterior().getDato();
        assertEquals("Santa Ana", ultima.getNombre());
        assertEquals(400, ((Propiedad) ultima).getPrecio());
        assertSame(primero, primero.getAnterior().getSiguiente());
    }

    @Test
    void hay28Propiedades6Eventos6Especiales() {
        int propiedades = 0, eventos = 0, especiales = 0;
        for (int i = 0; i < 40; i++) {
            Casilla c = tablero.obtenerCasilla(i);
            if (c instanceof Propiedad) propiedades++;
            else if (c instanceof CasillaEvento) eventos++;
            else if (c instanceof CasillaEspecial) especiales++;
        }
        assertEquals(28, propiedades);
        assertEquals(6, eventos);
        assertEquals(6, especiales);
    }

    private CasillaEspecial.Tipo tipoEspecial(int indice) {
        return ((CasillaEspecial) tablero.obtenerCasilla(indice)).getTipo();
    }
}
