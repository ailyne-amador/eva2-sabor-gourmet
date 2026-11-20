# Evidencias de pruebas manuales

- Ejecución: 2026-11-20, zona horaria `-03:00`.
- Aplicación: `mvn spring-boot:run`, `http://localhost:8080`.
- Se recorrieron las 42 solicitudes de la colección v2.1 en orden con un cliente HTTP basado en `urllib` (Python estándar); se comprobaron los estados esperados declarados en los tests de cada solicitud y las respuestas de disponibilidad. No había Postman/Newman instalado, por lo que este es el recorrido HTTP equivalente, no una ejecución del runner de Postman.
- Se recorrieron las 42 solicitudes de la colección v2.1 en orden con un cliente HTTP basado en `urllib` (Python estándar); se comprobaron los estados esperados declarados en los tests de cada solicitud y las respuestas de disponibilidad. `newman` no estaba disponible en `PATH`, así que no se ejecutó el runner de Postman.
- Resultado: 42/42 estados esperados; las respuestas de error observadas no contienen trazas.
- Después se detuvo y reinició la aplicación. `GET /api/reservas` respondió 200 y conservó las tres reservas, incluida la reserva 3 con estado `CANCELADA`.
- Copia del archivo persistido tras las pruebas: [`evidencias/datos_despues_de_pruebas.json`](evidencias/datos_despues_de_pruebas.json).

## CRUD, disponibilidad y reservas

| # | Método y URL | Cuerpo enviado | HTTP | Respuesta observada |
|---:|---|---|---:|---|
| 1 | `GET http://localhost:8080/api/clientes` | — | 200 | `[ {"id":1,"nombre":"Camila Rojas","email":"camila.rojas@example.com","telefono":"+56912345678"}, {"id":2,"nombre":"Diego Pérez","email":"diego.perez@example.com","telefono":"+56987654321"} ]` |
| 2 | `GET http://localhost:8080/api/clientes/1` | — | 200 | `{"id":1,"nombre":"Camila Rojas","email":"camila.rojas@example.com","telefono":"+56912345678"}` |
| 3 | `POST http://localhost:8080/api/clientes` | `{"nombre":"Paula Postman","email":"paula.postman@example.com","telefono":"+56911112222"}` | 201 | `Location: /api/clientes/3`; `{"id":3,"nombre":"Paula Postman","email":"paula.postman@example.com","telefono":"+56911112222"}` |
| 4 | `PUT http://localhost:8080/api/clientes/3` | `{"nombre":"Paula Postman Actualizada","email":"paula.actualizada@example.com","telefono":"+56911112222"}` | 200 | `{"id":3,"nombre":"Paula Postman Actualizada","email":"paula.actualizada@example.com","telefono":"+56911112222"}` |
| 5 | `DELETE http://localhost:8080/api/clientes/3` | — | 204 | Sin cuerpo. |
| 6 | `GET http://localhost:8080/api/mesas` | — | 200 | `[ {"id":1,"numero":1,"capacidad":2,"ubicacion":"Terraza"}, {"id":2,"numero":2,"capacidad":4,"ubicacion":"Salón principal"}, {"id":3,"numero":3,"capacidad":6,"ubicacion":"Patio"} ]` |
| 7 | `GET http://localhost:8080/api/mesas/1` | — | 200 | `{"id":1,"numero":1,"capacidad":2,"ubicacion":"Terraza"}` |
| 8 | `POST http://localhost:8080/api/mesas` | `{"numero":4,"capacidad":2,"ubicacion":"Postman"}` | 201 | `Location: /api/mesas/4`; `{"id":4,"numero":4,"capacidad":2,"ubicacion":"Postman"}` |
| 9 | `PUT http://localhost:8080/api/mesas/4` | `{"numero":4,"capacidad":3,"ubicacion":"Salón de pruebas"}` | 200 | `{"id":4,"numero":4,"capacidad":3,"ubicacion":"Salón de pruebas"}` |
| 10 | `DELETE http://localhost:8080/api/mesas/4` | — | 204 | Sin cuerpo. |
| 11 | `GET http://localhost:8080/api/mesas/disponibles?fecha=2026-12-07&hora=19:00&personas=2` | — | 200 | `[ {"id":1,"numero":1,"capacidad":2,"ubicacion":"Terraza"}, {"id":2,"numero":2,"capacidad":4,"ubicacion":"Salón principal"}, {"id":3,"numero":3,"capacidad":6,"ubicacion":"Patio"} ]` |
| 12 | `GET http://localhost:8080/api/mesas/disponibles?fecha=2026-12-05&hora=20:00&personas=4` | — | 200 | `[ {"id":3,"numero":3,"capacidad":6,"ubicacion":"Patio"} ]`; la mesa 2 está ocupada. |
| 13 | `GET http://localhost:8080/api/reservas` | — | 200 | `[ {"id":1,"clienteId":1,"mesaId":2,"fecha":"2026-12-05","hora":"20:00","duracionMinutos":120,"cantidadPersonas":4,"estado":"ACTIVA"}, {"id":2,"clienteId":2,"mesaId":3,"fecha":"2026-12-06","hora":"19:00","duracionMinutos":120,"cantidadPersonas":5,"estado":"ACTIVA"} ]` |
| 14 | `GET http://localhost:8080/api/reservas?fecha=2026-12-05&estado=ACTIVA&clienteId=1&mesaId=2` | — | 200 | `[ {"id":1,"clienteId":1,"mesaId":2,"fecha":"2026-12-05","hora":"20:00","duracionMinutos":120,"cantidadPersonas":4,"estado":"ACTIVA"} ]` |
| 15 | `GET http://localhost:8080/api/reservas/1` | — | 200 | `{"id":1,"clienteId":1,"mesaId":2,"fecha":"2026-12-05","hora":"20:00","duracionMinutos":120,"cantidadPersonas":4,"estado":"ACTIVA"}` |
| 16 | `POST http://localhost:8080/api/reservas` | `{"clienteId":1,"mesaId":1,"fecha":"2026-12-07","hora":"19:00","cantidadPersonas":2}` | 201 | `Location: /api/reservas/3`; `{"id":3,"clienteId":1,"mesaId":1,"fecha":"2026-12-07","hora":"19:00","duracionMinutos":120,"cantidadPersonas":2,"estado":"ACTIVA"}` |
| 17 | `GET http://localhost:8080/api/mesas/disponibles?fecha=2026-12-07&hora=19:00&personas=2` | — | 200 | `[ {"id":2,"numero":2,"capacidad":4,"ubicacion":"Salón principal"}, {"id":3,"numero":3,"capacidad":6,"ubicacion":"Patio"} ]`; la mesa 1 dejó de estar disponible. |
| 18 | `PUT http://localhost:8080/api/reservas/3` | `{"clienteId":1,"mesaId":1,"fecha":"2026-12-08","hora":"19:00","cantidadPersonas":2}` | 200 | `{"id":3,"clienteId":1,"mesaId":1,"fecha":"2026-12-08","hora":"19:00","duracionMinutos":120,"cantidadPersonas":2,"estado":"ACTIVA"}` |
| 19 | `GET http://localhost:8080/api/reservas/3` | — | 200 | `{"id":3,"clienteId":1,"mesaId":1,"fecha":"2026-12-08","hora":"19:00","duracionMinutos":120,"cantidadPersonas":2,"estado":"ACTIVA"}` |
| 20 | `GET http://localhost:8080/api/mesas/disponibles?fecha=2026-12-08&hora=19:00&personas=2` | — | 200 | `[ {"id":2,"numero":2,"capacidad":4,"ubicacion":"Salón principal"}, {"id":3,"numero":3,"capacidad":6,"ubicacion":"Patio"} ]`; la mesa 1 sigue ocupada. |
| 21 | `PATCH http://localhost:8080/api/reservas/3/cancelar` | — | 200 | `{"id":3,"clienteId":1,"mesaId":1,"fecha":"2026-12-08","hora":"19:00","duracionMinutos":120,"cantidadPersonas":2,"estado":"CANCELADA"}` |
| 22 | `GET http://localhost:8080/api/mesas/disponibles?fecha=2026-12-08&hora=19:00&personas=2` | — | 200 | `[ {"id":1,"numero":1,"capacidad":2,"ubicacion":"Terraza"}, {"id":2,"numero":2,"capacidad":4,"ubicacion":"Salón principal"}, {"id":3,"numero":3,"capacidad":6,"ubicacion":"Patio"} ]`; cancelar liberó la mesa 1. |
| 23 | `GET http://localhost:8080/api/reservas/3` | — | 200 | `{"id":3,"clienteId":1,"mesaId":1,"fecha":"2026-12-08","hora":"19:00","duracionMinutos":120,"cantidadPersonas":2,"estado":"CANCELADA"}` |

[CAPTURA POSTMAN: CRUD de clientes y mesas]
[CAPTURA POSTMAN: disponibilidad antes y después de reservar/cancelar]

## Errores

| # | Método y URL | Cuerpo enviado | HTTP | Respuesta observada |
|---:|---|---|---:|---|
| 24 | `POST http://localhost:8080/api/clientes` | `{"nombre":"","email":"correo-invalido","telefono":""}` | 400 | `{"timestamp":"2026-11-20T11:29:11.5099028","status":400,"error":"Bad Request","mensaje":"La solicitud contiene campos inválidos","ruta":"/api/clientes","detalles":["email: El correo electrónico debe tener un formato válido","nombre: El nombre es obligatorio","telefono: El teléfono es obligatorio"]}` |
| 25 | `POST http://localhost:8080/api/mesas` | `{"numero":0,"capacidad":0}` | 400 | `{"timestamp":"2026-11-20T11:29:11.5340316","status":400,"error":"Bad Request","mensaje":"La solicitud contiene campos inválidos","ruta":"/api/mesas","detalles":["capacidad: La capacidad debe ser positiva","numero: El número de mesa debe ser positivo"]}` |
| 26 | `POST http://localhost:8080/api/reservas` | `{"clienteId":1,"mesaId":1,"fecha":"2026-12-09","hora":"19:00","cantidadPersonas":0}` | 400 | `{"timestamp":"2026-11-20T11:29:11.5417577","status":400,"error":"Bad Request","mensaje":"La solicitud contiene campos inválidos","ruta":"/api/reservas","detalles":["cantidadPersonas: La cantidad de personas debe ser positiva"]}` |
| 27 | `GET http://localhost:8080/api/mesas/disponibles?fecha=2026-12-07&hora=19:00` | — | 400 | `{"timestamp":"2026-11-20T11:29:11.5633877","status":400,"error":"Bad Request","mensaje":"Falta el parámetro obligatorio 'personas'","ruta":"/api/mesas/disponibles","detalles":[]}` |
| 28 | `GET http://localhost:8080/api/mesas/disponibles?fecha=no-es-fecha&hora=19:00&personas=2` | — | 400 | `{"timestamp":"2026-11-20T11:29:11.5712265","status":400,"error":"Bad Request","mensaje":"El parámetro 'fecha' tiene un formato inválido","ruta":"/api/mesas/disponibles","detalles":[]}` |
| 29 | `POST http://localhost:8080/api/reservas` | `{"clienteId":1,"mesaId":1,"fecha":"2026-11-19","hora":"19:00","cantidadPersonas":2}` | 400 | `{"timestamp":"2026-11-20T11:29:11.5796058","status":400,"error":"Bad Request","mensaje":"La fecha y hora de la reserva no pueden ser anteriores a ahora","ruta":"/api/reservas","detalles":[]}` |
| 30 | `POST http://localhost:8080/api/reservas` | `{"clienteId":1,"mesaId":1,"fecha":"2026-12-09","hora":"22:00","cantidadPersonas":2}` | 400 | `{"timestamp":"2026-11-20T11:29:11.5940166","status":400,"error":"Bad Request","mensaje":"El horario solicitado está fuera del horario de atención","ruta":"/api/reservas","detalles":[]}` |
| 31 | `POST http://localhost:8080/api/reservas` | `{"clienteId":1,"mesaId":1,"fecha":"2026-12-09","hora":"19:00","cantidadPersonas":3}` | 400 | `{"timestamp":"2026-11-20T11:29:11.6031693","status":400,"error":"Bad Request","mensaje":"La cantidad de personas supera la capacidad de la mesa","ruta":"/api/reservas","detalles":[]}` |
| 32 | `GET http://localhost:8080/api/clientes/999999` | — | 404 | `{"timestamp":"2026-11-20T11:29:11.6087489","status":404,"error":"Not Found","mensaje":"No existe un cliente con ID 999999","ruta":"/api/clientes/999999","detalles":[]}` |
| 33 | `GET http://localhost:8080/api/mesas/999999` | — | 404 | `{"timestamp":"2026-11-20T11:29:11.6173711","status":404,"error":"Not Found","mensaje":"No existe una mesa con ID 999999","ruta":"/api/mesas/999999","detalles":[]}` |
| 34 | `GET http://localhost:8080/api/reservas/999999` | — | 404 | `{"timestamp":"2026-11-20T11:29:11.6276239","status":404,"error":"Not Found","mensaje":"No existe una reserva con ID 999999","ruta":"/api/reservas/999999","detalles":[]}` |
| 35 | `POST http://localhost:8080/api/reservas` | `{"clienteId":999999,"mesaId":1,"fecha":"2026-12-09","hora":"19:00","cantidadPersonas":2}` | 404 | `{"timestamp":"2026-11-20T11:29:11.6325437","status":404,"error":"Not Found","mensaje":"No existe un cliente con ID 999999","ruta":"/api/reservas","detalles":[]}` |
| 36 | `POST http://localhost:8080/api/reservas` | `{"clienteId":1,"mesaId":999999,"fecha":"2026-12-09","hora":"19:00","cantidadPersonas":2}` | 404 | `{"timestamp":"2026-11-20T11:29:11.6677485","status":404,"error":"Not Found","mensaje":"No existe una mesa con ID 999999","ruta":"/api/reservas","detalles":[]}` |
| 37 | `POST http://localhost:8080/api/mesas` | `{"numero":2,"capacidad":4,"ubicacion":"Duplicada"}` | 409 | `{"timestamp":"2026-11-20T11:29:11.6696516","status":409,"error":"Conflict","mensaje":"El número de mesa 2 ya está registrado","ruta":"/api/mesas","detalles":[]}` |
| 38 | `POST http://localhost:8080/api/reservas` | `{"clienteId":1,"mesaId":2,"fecha":"2026-12-05","hora":"21:00","cantidadPersonas":2}` | 409 | `{"timestamp":"2026-11-20T11:29:11.6832344","status":409,"error":"Conflict","mensaje":"La mesa 2 ya tiene una reserva activa que se superpone con el horario solicitado","ruta":"/api/reservas","detalles":[]}` |
| 39 | `DELETE http://localhost:8080/api/clientes/1` | — | 409 | `{"timestamp":"2026-11-20T11:29:11.6894095","status":409,"error":"Conflict","mensaje":"No se puede eliminar el cliente porque tiene reservas asociadas","ruta":"/api/clientes/1","detalles":[]}` |
| 40 | `DELETE http://localhost:8080/api/mesas/2` | — | 409 | `{"timestamp":"2026-11-20T11:29:11.7006944","status":409,"error":"Conflict","mensaje":"No se puede eliminar la mesa porque tiene reservas asociadas","ruta":"/api/mesas/2","detalles":[]}` |
| 41 | `PATCH http://localhost:8080/api/reservas/3/cancelar` | — | 409 | `{"timestamp":"2026-11-20T11:29:11.7126551","status":409,"error":"Conflict","mensaje":"La reserva ya está cancelada","ruta":"/api/reservas/3/cancelar","detalles":[]}` |
| 42 | `PUT http://localhost:8080/api/reservas/3` | `{"clienteId":1,"mesaId":1,"fecha":"2026-12-09","hora":"19:00","cantidadPersonas":2}` | 409 | `{"timestamp":"2026-11-20T11:29:11.7243604","status":409,"error":"Conflict","mensaje":"Solo se pueden modificar reservas activas","ruta":"/api/reservas/3","detalles":[]}` |

[CAPTURA POSTMAN: respuestas de error 400, 404 y 409 sin trazas]

## Persistencia tras reinicio

| Método y URL | Cuerpo enviado | HTTP | Respuesta observada |
|---|---|---:|---|
| `GET http://localhost:8080/api/reservas` (después de detener y volver a iniciar la aplicación) | — | 200 | `[ {"id":1,"clienteId":1,"mesaId":2,"fecha":"2026-12-05","hora":"20:00","duracionMinutos":120,"cantidadPersonas":4,"estado":"ACTIVA"}, {"id":2,"clienteId":2,"mesaId":3,"fecha":"2026-12-06","hora":"19:00","duracionMinutos":120,"cantidadPersonas":5,"estado":"ACTIVA"}, {"id":3,"clienteId":1,"mesaId":1,"fecha":"2026-12-08","hora":"19:00","duracionMinutos":120,"cantidadPersonas":2,"estado":"CANCELADA"} ]` |

[CAPTURA POSTMAN: GET /api/reservas después del reinicio]
