# Transacciones: puntos 11, 12 y 13

El servidor mantiene las transacciones oficiales. El cliente solicita consultas
y exportaciones por sockets; no modifica el historial ni calcula pagos.

## 11. Datos y operaciones

`Transaccion` es un registro inmutable. Contiene un número consecutivo por partida,
fecha y hora con zona horaria, turno, tipo, identificadores y nombres de origen y
destino, monto positivo y descripción. También conserva los saldos de ambos
participantes antes y después de la operación. El banco se identifica como `BANCO`.

`Banco` genera una transacción después de validar y completar cada operación.
Los pagos rechazados no generan transacciones. Un alquiler se registra una sola
vez y se puede encontrar tanto por el pagador como por el propietario.

| Tipo obligatorio | Operación del banco |
| --- | --- |
| `COMPRA_PROPIEDAD` | `comprarPropiedad` y `comprarPropiedadConTarjeta` |
| `PAGO_ALQUILER` | `cobrarAlquiler` |
| `PAGO_AL_BANCO` | `cobrarJugador` y `cobrarImpuesto` |
| `PAGO_ENTRE_JUGADORES` | `pagarEntreJugadores` |
| `GANANCIA_EVENTO` | `pagarGananciaEvento` |
| `PERDIDA_EVENTO` | `cobrarPerdidaEvento` |
| `PREMIO_INICIO` | `pagarSalida` |

También se registran casas, hoteles, hipotecas, pagos genéricos del banco y la
asignación inicial de dinero. La asignación inicial informa el saldo con el que
se creó el jugador; no descuenta dinero de la reserva inicial del banco.

Los métodos de eventos reciben la descripción de la carta y están disponibles
para el motor de cartas. Este cambio implementa su pago y registro; no implementa
el mazo ni la selección de cartas del punto 10. Del mismo modo, los métodos de
alquiler y pago entre jugadores registran la operación cuando el servidor los invoca.

`Juego` actualiza el turno utilizado por el registro cuando avanza al siguiente
jugador. Los pagos del banco y la asignación de números están sincronizados.

## 12. Estructura del historial

`HistorialTransacciones` utiliza `estructuras.ListaCircularDoble`, implementada en
el proyecto. Sus nodos conservan referencias anterior y siguiente.

- `agregar`: inserta al final en O(1).
- `desdeLaMasAntigua`: recorre desde la cabeza por los enlaces siguientes.
- `desdeLaMasReciente`: comienza en el último nodo y recorre los enlaces anteriores.
- `buscarPorJugador`: busca por identificador de origen o de destino.
- `buscarPorTipo`: busca por `TipoTransaccion`.
- `consultar`: combina jugador, tipo y orden.
- `imprimirTodas`: produce el texto completo, en orden cronológico.

Los recorridos y búsquedas son O(n). Se recorren exactamente n nodos para evitar
repeticiones al llegar nuevamente al inicio de la lista circular. Los arreglos
devueltos son copias de resultados; la lista enlazada almacena el historial.
No se utilizan colecciones de `java.util` para esta estructura.

## 13. Consulta y exportación TXT

1. Abrir el tablero desde `Interfaz` después de iniciar una partida.
2. Pulsar **Historial y exportar TXT**.
3. Elegir jugador, tipo y orden, y pulsar **Consultar**. Ambos filtros incluyen
   la opción **Todos**. Banco también es una opción de participante.
4. Pulsar **Exportar toda la partida a TXT** y elegir dónde guardar el archivo.

La consulta y exportación siguen disponibles al terminar la partida. La
exportación incluye toda la partida en orden cronológico, aunque la consulta
visible tenga filtros. El texto se prepara en el servidor y se guarda en UTF-8.

Además se crea automáticamente un TXT independiente en `transacciones/` dentro
del directorio desde el que se ejecuta el servidor. Cada operación se agrega
inmediatamente; no es necesario esperar al fin de la partida.

Las columnas son: número, fecha y hora, turno, tipo, origen, destino, monto,
descripción, saldo anterior y posterior de origen, y saldo anterior y posterior
de destino. La moneda utilizada es CRC (colones).

Si falla una escritura durante un pago, se conserva la transacción en memoria y
se informa el error. El pago no se repite y el historial se puede reexportar
mientras el servidor siga en ejecución.

Mensajes añadidos al protocolo:

| Dirección | Mensaje | Contenido |
| --- | --- | --- |
| Cliente → servidor | `ConsultarTransacciones` | Identificador del jugador, tipo y orden; filtros nulos significan todos |
| Cliente → servidor | `ExportarTransacciones` | Solicitud del historial completo |
| Servidor → cliente | `HistorialTransacciones` | Texto, nombre sugerido del TXT y si es una exportación |

El servidor valida que quien consulta participe en la partida y que los filtros
sean válidos. Las solicitudes no permiten insertar ni alterar transacciones.

## Verificación

`TransaccionesTest` verifica los campos obligatorios, siete tipos mínimos,
recorridos, filtros, pagos rechazados, compra RFID, operaciones adicionales,
turnos, archivos independientes, exportación UTF-8 y recuperación ante un error
de escritura. Se ejecuta junto con las pruebas existentes del proyecto.
