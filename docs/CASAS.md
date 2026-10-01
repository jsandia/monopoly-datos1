# Compra de casas

La ventana abierta desde `Interfaz.java` muestra el botón **Comprar casas**
cuando el jugador en turno posee una propiedad edificable. Puede elegir cualquiera
de sus propiedades, sin tener que caer en ella, antes o después de tirar los dados.

Para construir debe poseer todo el grupo de color. Las casas se compran de una
en una y uniformemente: no se permite una segunda casa hasta que cada propiedad
del grupo tenga una. El máximo es cuatro casas por propiedad. Ninguna propiedad
del grupo puede estar hipotecada. No se construye en transportes ni servicios.
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

Se debe recompilar y reiniciar el servidor y ambos clientes porque cambió el
mensaje de estado compartido. Esta opción compra casas; la compra de hoteles
todavía no se ofrece en la ventana.

Pruebas: `CasasTest` verifica reglas y cobros, y `GameServerCasasTest` verifica
el pago con RFID, el turno y la serialización del estado.

Referencia de reglas: [manual clásico de Hasbro](https://www.hasbro.com/common/instruct/Monopoly.pdf).
