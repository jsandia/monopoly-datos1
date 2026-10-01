# Compra de casas

La ventana abierta desde `Interfaz.java` muestra el botón **Comprar casas**
cuando el jugador en turno posee una propiedad edificable. Puede elegir cualquiera
de sus propiedades, sin tener que caer en ella, antes o después de tirar los dados.

Para construir debe ser dueño de la propiedad y haber recorrido una vuelta
completa (40 casillas) desde su compra. Cada propiedad tiene su propio contador:
los recorridos anteriores a la compra no cuentan y pasar por Salida no basta
si aún no ha recorrido las 40 casillas desde que la adquirió. Los movimientos
de cartas hacia adelante cuentan; los retrocesos descuentan recorrido. Ir directo
a la cárcel no cuenta. Una vez cumplida la vuelta, la propiedad sigue habilitada.
Cambiar el dueño reinicia la espera para el nuevo propietario.

No se exige completar el grupo de color ni repartir las casas uniformemente.
Las casas se compran de una en una, hasta cuatro por propiedad. Solo la propiedad
donde se construye debe estar libre de hipoteca. No se construye en transportes ni servicios.
El banco dispone inicialmente de 32 casas y comprueba saldo y existencias.

Cada compra se confirma al acercar la tarjeta RFID del jugador en turno, igual
que la compra de propiedades. Una tarjeta incorrecta no realiza el cobro. Si se
termina el turno antes del pago, la compra pendiente se cancela.

| Grupo | Precio por casa |
|---|---:|
| Café y Celeste | ₡50 |
| Rosado y Naranja | ₡100 |
| Rojo y Amarillo | ₡150 |
| Verde y Azul | ₡200 |

Las rentas usan los valores de las escrituras del Monopoly clásico, según la
cantidad de casas. Por ejemplo, Sixaola cobra ₡2 de base, ₡4 con el grupo completo
sin casas y ₡10, ₡30, ₡90 o ₡160 con una, dos, tres o cuatro casas. La propiedad
hipotecada cobra cero. Cada compra y cada pago de renta se registra en el historial.

El cliente envía `Message.ComprarCasa(idPropiedad)`. El servidor valida el turno
y las reglas antes de solicitar el pago y vuelve a validarlas al leer la tarjeta.
`EstadoJuego` incluye las propiedades con dueño, casas, hotel, hipoteca, renta,
precio por casa y motivo que impide construir, además de las existencias del banco.
Ambas computadoras dibujan las casas verdes sobre la franja de color y muestran
la renta oficial. El cliente no cambia las propiedades del modelo por su cuenta.

El botón **Propiedades de los jugadores** abre una ventana con una pestaña por
jugador. Muestra sus propiedades, grupo, casas, hotel, renta e hipoteca, e indica
cuando alguien todavía no tiene propiedades. Se actualiza con cada estado del
servidor y se puede consultar incluso fuera del turno o al finalizar la partida.

Se debe recompilar y reiniciar el servidor y ambos clientes porque cambió el
mensaje de estado compartido. Esta opción compra casas; la compra de hoteles
todavía no se ofrece en la ventana.

Pruebas: `CasasTest` verifica reglas y cobros, y `GameServerCasasTest` verifica
el pago con RFID, el turno y la serialización del estado.

Referencia de reglas: [manual clásico de Hasbro](https://www.hasbro.com/common/instruct/Monopoly.pdf).
