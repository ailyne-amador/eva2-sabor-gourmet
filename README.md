# Sabor Gourmet — API REST de Reservas

API sin interfaz gráfica para administrar clientes, mesas y reservas. Usa Java 21, Spring Boot 4.1.2, Maven y Jackson para persistir en `data/datos.json`, con separación Controller → Service → Repository.

## Requisitos

- JDK 21.
- Maven.

## Compilar, probar y ejecutar

Desde la raíz del proyecto:

```bash
mvn clean verify
mvn spring-boot:run
```

`mvn clean verify` ejecuta las pruebas unitarias, MVC y de integración. La API queda disponible en `http://localhost:8080`.

## Persistencia y configuración

La aplicación lee `data/datos.json` del disco en cada operación; las modificaciones se guardan en ese archivo. Si no existe, crea directorios y un JSON vacío con esta estructura:

```json
{"clientes": [], "mesas": [], "reservas": []}
```

Las referencias de una reserva se guardan mediante `clienteId` y `mesaId`. Las escrituras usan un archivo temporal y movimiento atómico cuando el sistema de archivos lo permite. Un JSON existente pero corrupto produce un error de persistencia y no se sobrescribe.

| Propiedad | Valor predeterminado | Uso |
|---|---:|---|
| `app.datos.ruta` | `data/datos.json` | Ruta relativa del archivo de datos. |
| `app.reservas.duracion-minutos` | `120` | Duración fija de cada reserva. |
| `app.reservas.hora-apertura` | `12:00` | Hora mínima de inicio. |
| `app.reservas.hora-cierre` | `23:00` | Hora límite de término. |
| `server.port` | `8080` | Puerto HTTP. |

Las propiedades se pueden cambiar en `src/main/resources/application.properties` o mediante configuración externa de Spring Boot.

## Endpoints

Las fechas usan `YYYY-MM-DD` y las horas `HH:mm`.

| Método | Ruta | Éxito |
|---|---|---|
| GET | `/api/clientes` | 200 |
| GET | `/api/clientes/{id}` | 200 |
| POST | `/api/clientes` | 201 + `Location` |
| PUT | `/api/clientes/{id}` | 200 |
| DELETE | `/api/clientes/{id}` | 204 |
| GET | `/api/mesas` | 200 |
| GET | `/api/mesas/{id}` | 200 |
| POST | `/api/mesas` | 201 + `Location` |
| PUT | `/api/mesas/{id}` | 200 |
| DELETE | `/api/mesas/{id}` | 204 |
| GET | `/api/mesas/disponibles?fecha=YYYY-MM-DD&hora=HH:mm&personas=N` | 200 |
| GET | `/api/reservas` | 200; filtros opcionales `fecha`, `estado`, `clienteId`, `mesaId` |
| GET | `/api/reservas/{id}` | 200 |
| POST | `/api/reservas` | 201 + `Location` |
| PUT | `/api/reservas/{id}` | 200 |
| PATCH | `/api/reservas/{id}/cancelar` | 200 |

Una reserva debe estar en el futuro, dentro del horario de atención y no superar la capacidad de la mesa. Dos reservas activas de la misma mesa y fecha no pueden solaparse; los turnos contiguos sí se permiten. Cancelar cambia el estado a `CANCELADA`, conserva el historial y libera la mesa. Los errores usan `400` (solicitud inválida), `404` (recurso inexistente), `409` (conflicto) o `500` genérico; las respuestas no exponen trazas.

## Probar la API manualmente

- **Postman:** importa `docs/postman/SaborGourmet.postman_collection.json`. La colección define `baseUrl` como `http://localhost:8080` y agrupa clientes, mesas, disponibilidad, reservas, cancelación y errores.
- **REST Client:** abre `docs/requests.http` en VS Code con la extensión REST Client o IntelliJ IDEA y ejecuta las solicitudes en orden.
- **Evidencias:** `docs/evidencias.md` registra los cuerpos y respuestas observadas; `data/datos.json` contiene datos de ejemplo.

## Decisiones y limitaciones

- Persistencia en un solo archivo JSON; no se utiliza una base de datos.
- La aplicación está diseñada para una sola instancia. El bloqueo de lectura/escritura y `synchronized` coordinan operaciones dentro de esa instancia, no entre procesos o réplicas.
- Los IDs se calculan como `max(id) + 1` por colección; al eliminar el registro con el ID más alto, ese ID puede reutilizarse.
- Las reservas duran 120 minutos por defecto; cada reserva guarda su duración para conservar el historial aunque cambie la configuración.
- La cancelación no elimina reservas: cambia su estado y mantiene las relaciones históricas.

Las decisiones adicionales están registradas en [`docs/DECISIONES.md`](docs/DECISIONES.md).
