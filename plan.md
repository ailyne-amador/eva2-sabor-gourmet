# PLAN DE DESARROLLO — API REST "Sabor Gourmet" (Reservas)

> **Asignatura:** Desarrollo de Software Web II — IPSS
> **Evaluación:** Sumativa Unidad 2 (grupal, máx. 3 integrantes, 20 % de la nota final)
> **Stack:** Java 21 · Spring Boot 4.1.2 · Maven · `application.properties` · persistencia en archivo JSON (Jackson)

---

## 0. Instrucciones para el agente (LEER PRIMERO)

Este documento es la fuente de verdad del proyecto. Trabaja así:

1. **Lee el documento completo** antes de escribir código.
2. **Ejecuta UNA fase a la vez**, en orden. No empieces la fase N+1 hasta que todos los criterios de aceptación de la fase N estén cumplidos y `mvn test` (o `./mvnw test` si existe el wrapper) pase en verde.
3. Al terminar cada tarea, **marca su checkbox** (`- [x]`) en este archivo. Al terminar una fase, actualiza la tabla de la sección 9 (Estado).
4. Haz **un commit por fase** con el formato `feat(faseN): descripción corta` (o `test(...)`, `docs(...)`, `fix(...)`).
5. Si una decisión no está definida aquí, elige la opción **más simple que cumpla la consigna**, y regístrala en `docs/DECISIONES.md` (una línea por decisión).
6. **Idioma:** nombres de clases, métodos, variables, comentarios, Javadoc y mensajes de error en **español** (el código se evalúa por su explicación).
7. **Comentarios detallados** en el código: cada clase lleva Javadoc que explique su responsabilidad en la arquitectura CSR; cada método público lleva Javadoc con reglas de negocio, parámetros, retorno y excepciones. Esto es parte de lo evaluado.
8. Si un criterio falla y no puedes resolverlo, **detente y reporta** el problema en lugar de saltarte la fase.

### Prohibiciones (no negociables)

- ❌ No usar Spring Data JPA, Hibernate, H2, ni ninguna base de datos.
- ❌ No usar Thymeleaf ni ninguna vista / interfaz gráfica.
- ❌ No poner lógica de negocio en los Controllers.
- ❌ El Controller **nunca** accede directamente a un Repository (solo al Service).
- ❌ No devolver entidades/objetos "sueltos" desde un Controller: **siempre `ResponseEntity<T>`**.
- ❌ Nunca exponer trazas (stack traces) ni mensajes internos de excepciones en las respuestas HTTP.
- ❌ No agregar dependencias fuera de las listadas en la sección 2 sin registrar la razón en `docs/DECISIONES.md`.

---

## 1. Contexto y objetivo

El restaurante **"Sabor Gourmet"** quiere modernizar su sistema manual de reservas con una API REST que permita:

- Consultar **disponibilidad de mesas** (por fecha, hora y cantidad de personas).
- **Crear, listar, consultar, modificar y cancelar** reservas.
- Que el personal **gestione clientes y mesas** (CRUD) y consulte las reservas actuales.
- **Persistir** todo en un archivo JSON que sobreviva al reinicio de la aplicación.

La API se probará con **Postman** (o equivalente). No hay interfaz gráfica.

### Entregables

| # | Entregable | Ubicación / formato |
|---|------------|---------------------|
| 1 | Código fuente ejecutable completo (config, modelos, controllers, services, repositories, JSON, recursos) | Repositorio del proyecto |
| 2 | Archivo `datos.json` con datos de ejemplo + estructura documentada | `data/datos.json` |
| 3 | Colección Postman o solicitudes reproducibles | `docs/postman/` y `docs/requests.http` |
| 4 | Informe (Word/PDF) con explicación del código por archivo | `docs/informe/` (borrador en Markdown; el formato final lo arma el grupo) |
| 5 | README con instrucciones de ejecución | `README.md` |

> **Nombre del archivo del informe:** `EVU2_GRUPO_APELLIDO_NOMBRE` (.docx o .pdf).
> La rúbrica oficial está en la plataforma; el grupo debe contrastarla con este plan antes de entregar.

---

## 2. Stack y restricciones técnicas

| Elemento | Decisión                                                                                |
|----------|-----------------------------------------------------------------------------------------|
| Lenguaje | Java 21                                                                                 |
| Framework | Spring Boot 4.1.2                                                                    |
| Build | **Maven**                                                                               |
| Configuración | **`src/main/resources/application.properties`** (no YAML)                               |
| Persistencia | Archivo JSON vía Jackson (`ObjectMapper`)                                               |
| Group / Artifact | `com.saborgourmet` / `reservas`                                                         |
| Paquete base | `com.saborgourmet.reservas`                                                             |

### Dependencias (únicamente estas)

```xml
<dependencies>
    <!-- Spring MVC: controllers REST, ResponseEntity y Jackson -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>

    <!-- Bean Validation: @Valid, @NotBlank, @Positive, etc. -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Lombok: reduce boilerplate (getters, builders, constructores) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Test MVC: JUnit, Mockito, MockMvc, AssertJ y soporte de Spring MVC -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

En `spring-boot-maven-plugin`, **excluir Lombok** del empaquetado:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```

### Uso de Lombok (convenciones)

- Modelos: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` (Jackson necesita constructor vacío para deserializar).
- Services / Repositories / Controllers: `@RequiredArgsConstructor` para inyección por constructor (campos `private final`).
- Logging: `@Slf4j`.
- DTOs de entrada: pueden ser `record` de Java con anotaciones de validación (no requieren Lombok).

### `application.properties` (base)

```properties
spring.application.name=sabor-gourmet-reservas
server.port=8080

# --- Persistencia JSON ---
app.datos.ruta=data/datos.json

# --- Reglas de reserva ---
app.reservas.duracion-minutos=120
app.reservas.hora-apertura=12:00
app.reservas.hora-cierre=23:00

# --- Jackson ---
spring.jackson.datatype.datetime.write-dates-as-timestamps=false
spring.jackson.default-property-inclusion=non_null

# --- Seguridad de respuestas: nunca exponer trazas ---
server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-binding-errors=never

# --- Logging ---
logging.level.com.saborgourmet=INFO
```

---

## 3. Decisiones de diseño (cerradas)

| Tema | Decisión |
|------|----------|
| IDs | `Long`, autoincrementales calculados como `max(id) + 1` dentro de cada colección (vacía → 1). |
| Duración / turnos | **Duración fija de 2 horas** por reserva (`app.reservas.duracion-minutos`). Se guarda `duracionMinutos` en cada reserva para no alterar el historial si cambia la configuración. |
| Solapamiento | Dos reservas se superponen si son de la **misma mesa y fecha**, ambas `ACTIVA`, y `inicioA < finB && inicioB < finA`. Reservas contiguas (una termina justo cuando otra empieza) **no** se superponen. |
| Horario de atención | Hora de inicio ≥ apertura y `hora + duración ≤ cierre` (configurable). |
| Estados de reserva | `ACTIVA`, `CANCELADA`. Cancelar **no borra** la reserva: cambia su estado (conserva historial y libera la mesa). |
| Cancelación | `PATCH /api/reservas/{id}/cancelar` → **200** con la reserva actualizada. |
| Fecha pasada | Reservar o modificar a una fecha/hora anterior a "ahora" → **400**. Se inyecta un `Clock` para poder probarlo. |
| Capacidad excedida | `cantidadPersonas > capacidad` de la mesa → **400**. |
| Cliente/mesa referenciado inexistente (en el body de una reserva) | **404**. |
| Eliminar cliente o mesa con reservas asociadas (de cualquier estado) | **409** (se respeta la integridad referencial). |
| Reducir capacidad de mesa por debajo de la de una reserva `ACTIVA` futura | **409**. |
| Duplicados | El ID identifica a cada cliente; el `numero` de mesa es único → **409**. |
| Lectura/escritura | Cada operación **lee el JSON del disco**, aplica el cambio y **guarda** (bajo `ReentrantReadWriteLock`). Escritura **atómica**: archivo temporal + `Files.move`. |
| Concurrencia | Aplicación de una sola instancia; los métodos que crean/modifican reservas son `synchronized` en el Service. Documentar la limitación. |
| Archivo corrupto | Si el JSON existe pero no se puede leer → `PersistenciaException` (500 genérico) y **no se sobrescribe** el archivo. |
| Entradas | DTOs de request separados del modelo; campos numéricos obligatorios como `Integer`/`Long` (wrappers) con `@NotNull`, para que `null` dé 400 y no un `0` silencioso. |

---

## 4. Estructura del proyecto

```
sabor-gourmet-reservas/
├── pom.xml
├── README.md
├── plan.md
├── .gitignore
├── data/
│   └── datos.json                      # datos de ejemplo (se crea vacío si no existe)
├── docs/
│   ├── DECISIONES.md
│   ├── requests.http
│   ├── evidencias.md                   # request/response reales de cada prueba
│   ├── postman/
│   │   └── SaborGourmet.postman_collection.json
│   └── informe/
│       └── borrador_informe.md
└── src/
    ├── main/
    │   ├── java/com/saborgourmet/reservas/
    │   │   ├── ReservasApplication.java
    │   │   ├── config/
    │   │   │   ├── ClockConfig.java
    │   │   │   └── ReservasProperties.java
    │   │   ├── controller/
    │   │   │   ├── ClienteController.java
    │   │   │   ├── MesaController.java
    │   │   │   └── ReservaController.java
    │   │   ├── service/
    │   │   │   ├── ClienteService.java
    │   │   │   ├── MesaService.java
    │   │   │   └── ReservaService.java
    │   │   ├── repository/
    │   │   │   ├── JsonFileStore.java          # E/S del archivo (lock, atómico)
    │   │   │   ├── ClienteRepository.java
    │   │   │   ├── MesaRepository.java
    │   │   │   └── ReservaRepository.java
    │   │   ├── model/
    │   │   │   ├── Cliente.java
    │   │   │   ├── Mesa.java
    │   │   │   ├── Reserva.java
    │   │   │   ├── EstadoReserva.java
    │   │   │   └── DatosRestaurante.java       # contenedor raíz del JSON
    │   │   ├── dto/
    │   │   │   ├── ClienteRequest.java
    │   │   │   ├── MesaRequest.java
    │   │   │   ├── ReservaRequest.java
    │   │   │   └── ErrorResponse.java
    │   │   └── exception/
    │   │       ├── RecursoNoEncontradoException.java   # 404
    │   │       ├── SolicitudInvalidaException.java     # 400
    │   │       ├── ConflictoException.java             # 409
    │   │       ├── PersistenciaException.java          # 500 genérico
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/saborgourmet/reservas/
            ├── repository/   (JsonFileStoreTest, *RepositoryTest)
            ├── service/      (*ServiceTest)
            ├── controller/   (*ControllerTest con @WebMvcTest)
            └── integracion/  (FlujoCompletoIT con @SpringBootTest)
```

---

## 5. Modelo de datos y estructura del JSON

### Clases del modelo

| Clase | Campos |
|-------|--------|
| `Cliente` | `Long id`, `String nombre`, `String email`, `String telefono` |
| `Mesa` | `Long id`, `Integer numero`, `Integer capacidad`, `String ubicacion` (opcional) |
| `Reserva` | `Long id`, `Long clienteId`, `Long mesaId`, `LocalDate fecha`, `LocalTime hora`, `Integer duracionMinutos`, `Integer cantidadPersonas`, `EstadoReserva estado` |
| `EstadoReserva` | enum `ACTIVA`, `CANCELADA` |
| `DatosRestaurante` | `List<Cliente> clientes`, `List<Mesa> mesas`, `List<Reserva> reservas` (listas inicializadas vacías, nunca `null`) |

Las referencias entre registros son **solo por ID** (`clienteId`, `mesaId`), no objetos anidados.

### `data/datos.json` (estructura y ejemplo)

```json
{
  "clientes": [
    {
      "id": 1,
      "nombre": "Camila Rojas",
      "email": "camila.rojas@example.com",
      "telefono": "+56912345678"
    }
  ],
  "mesas": [
    { "id": 1, "numero": 1, "capacidad": 2, "ubicacion": "Terraza" },
    { "id": 2, "numero": 2, "capacidad": 4, "ubicacion": "Salón principal" },
    { "id": 3, "numero": 3, "capacidad": 6, "ubicacion": "Salón principal" }
  ],
  "reservas": [
    {
      "id": 1,
      "clienteId": 1,
      "mesaId": 2,
      "fecha": "2026-12-05",
      "hora": "20:00",
      "duracionMinutos": 120,
      "cantidadPersonas": 4,
      "estado": "ACTIVA"
    }
  ]
}
```

Estructura mínima válida cuando el archivo no existe: `{"clientes":[],"mesas":[],"reservas":[]}`.

---

## 6. Contrato de la API

### Endpoints

| Método | Ruta | Descripción | Éxito |
|--------|------|-------------|-------|
| GET | `/api/clientes` | Listar clientes | 200 |
| GET | `/api/clientes/{id}` | Consultar cliente | 200 |
| POST | `/api/clientes` | Crear cliente | 201 + `Location` |
| PUT | `/api/clientes/{id}` | Actualizar cliente | 200 |
| DELETE | `/api/clientes/{id}` | Eliminar cliente | 204 |
| GET | `/api/mesas` | Listar mesas | 200 |
| GET | `/api/mesas/{id}` | Consultar mesa | 200 |
| POST | `/api/mesas` | Crear mesa | 201 + `Location` |
| PUT | `/api/mesas/{id}` | Actualizar configuración de mesa | 200 |
| DELETE | `/api/mesas/{id}` | Eliminar mesa | 204 |
| GET | `/api/mesas/disponibles?fecha=YYYY-MM-DD&hora=HH:mm&personas=N` | Mesas disponibles | 200 (lista, puede ser `[]`) |
| GET | `/api/reservas` | Listar reservas (filtros opcionales: `fecha`, `estado`, `clienteId`, `mesaId`) | 200 |
| GET | `/api/reservas/{id}` | Consultar reserva | 200 |
| POST | `/api/reservas` | Crear reserva | 201 + `Location` |
| PUT | `/api/reservas/{id}` | Modificar reserva | 200 |
| **PATCH** | **`/api/reservas/{id}/cancelar`** | **Cancelar reserva** (estado → `CANCELADA`) | 200 |

> Nota de routing: `/api/mesas/disponibles` debe declararse de forma que no choque con `/api/mesas/{id}` (Spring prioriza la ruta literal; verificarlo con un test).

### Códigos de error

| Código | Cuándo |
|--------|--------|
| 400 | Campos obligatorios faltantes/vacíos, cantidades ≤ 0, formato inválido (fecha/hora/JSON/tipo), fecha pasada, fuera de horario, capacidad excedida, parámetros de consulta faltantes. |
| 404 | Cliente, mesa o reserva inexistente (en la ruta o referenciada en el body). |
| 409 | Reserva superpuesta, número de mesa duplicado, eliminar recurso con reservas asociadas, cancelar una reserva ya cancelada, modificar una reserva cancelada. |
| 500 | Error de persistencia / no controlado → mensaje genérico, **sin traza**. |

### Formato de error (`ErrorResponse`)

```json
{
  "timestamp": "2026-11-18T10:15:30",
  "status": 409,
  "error": "Conflict",
  "mensaje": "La mesa 2 ya tiene una reserva activa que se superpone con el horario solicitado",
  "ruta": "/api/reservas",
  "detalles": []
}
```

`detalles` lista los errores por campo cuando la validación falla (ej. `"email: debe ser un correo válido"`).

### Ejemplo — crear reserva

```http
POST /api/reservas
Content-Type: application/json

{
  "clienteId": 1,
  "mesaId": 2,
  "fecha": "2026-12-05",
  "hora": "20:00",
  "cantidadPersonas": 4
}
```

Respuesta `201 Created` (`Location: /api/reservas/2`):

```json
{
  "id": 2,
  "clienteId": 1,
  "mesaId": 2,
  "fecha": "2026-12-05",
  "hora": "20:00",
  "duracionMinutos": 120,
  "cantidadPersonas": 4,
  "estado": "ACTIVA"
}
```

---

## 7. Reglas de negocio y validaciones

**Validación de entrada (Bean Validation en DTOs, activada con `@Valid` en el Controller):**

- `ClienteRequest`: `@NotBlank nombre`, `@NotBlank @Email email`, `@NotBlank telefono`.
- `MesaRequest`: `@NotNull @Positive numero`, `@NotNull @Positive capacidad`, `ubicacion` opcional.
- `ReservaRequest`: `@NotNull @Positive clienteId`, `@NotNull @Positive mesaId`, `@NotNull fecha`, `@NotNull hora`, `@NotNull @Positive cantidadPersonas`.

**Reglas de negocio (en Service):**

1. Crear reserva — en este orden: cliente existe (404) → mesa existe (404) → fecha/hora no pasada (400) → dentro del horario (400) → `personas ≤ capacidad` (400) → sin solapamiento activo en la mesa (409) → asignar `duracionMinutos`, `estado = ACTIVA`, ID → guardar.
2. Modificar reserva — la reserva existe (404) y está `ACTIVA` (409) → repetir validaciones de creación **excluyendo la propia reserva** del chequeo de solapamiento.
3. Cancelar — existe (404) → si ya está `CANCELADA` → 409 → `estado = CANCELADA` → guardar. Desde ese momento la mesa queda disponible en ese horario.
4. Disponibilidad — mesas con `capacidad ≥ personas` y sin reserva `ACTIVA` superpuesta en `[hora, hora + duración)`; valida horario y parámetros.
5. Eliminar cliente/mesa — 404 si no existe; 409 si tiene reservas asociadas.
6. Crear/actualizar mesa — 409 si `numero` ya existe en **otra** mesa.
7. Actualizar mesa — 409 si la nueva capacidad es menor que `cantidadPersonas` de alguna reserva `ACTIVA` de esa mesa.

---

## 8. FASES DE DESARROLLO

### FASE 1 — Bootstrap del proyecto

**Objetivo:** proyecto Maven que compila y arranca vacío.

- [x] Crear proyecto Maven con `pom.xml` según sección 2 (Java 21, parent `spring-boot-starter-parent`, solo las 4 dependencias).
- [x] Crear la estructura de paquetes de la sección 4 (con `package-info.java` o clases vacías según corresponda).
- [x] Crear `ReservasApplication.java` con `@SpringBootApplication` (Javadoc explicando la arquitectura CSR del proyecto).
- [x] Crear `application.properties` según sección 2.
- [x] Crear `ReservasProperties` (`@ConfigurationProperties(prefix = "app.reservas")`, con `@Validated`): `duracionMinutos`, `horaApertura`, `horaCierre`. Registrarla con `@EnableConfigurationProperties` o `@ConfigurationPropertiesScan`.
- [x] Crear `ClockConfig` exponiendo un `@Bean Clock` (`Clock.systemDefaultZone()`).
- [x] Crear `.gitignore` (target/, .idea/, *.iml, .vscode/, `data/*.tmp`, `data/*.bak`). **No** ignorar `data/datos.json`.
- [x] Crear `README.md` inicial (descripción + cómo compilar/ejecutar) y `docs/DECISIONES.md`.
- [x] Test de humo: `ReservasApplicationTests` con `@SpringBootTest` y `contextLoads()`.

**Criterios de aceptación**
- `mvn clean test` → verde.
- `mvn spring-boot:run` arranca en el puerto 8080 sin errores.

**Commit:** `feat(fase1): bootstrap del proyecto Spring Boot con Maven`

---

### FASE 2 — Modelo de datos y DTOs

**Objetivo:** clases del dominio y de entrada/salida, con Jackson funcionando para fechas.

- [x] `EstadoReserva` (enum `ACTIVA`, `CANCELADA`).
- [x] `Cliente`, `Mesa`, `Reserva` con Lombok (`@Data @Builder @NoArgsConstructor @AllArgsConstructor`) y Javadoc por campo.
- [x] `DatosRestaurante` con las tres listas inicializadas a `new ArrayList<>()` (usar `@Builder.Default`).
- [x] DTOs `ClienteRequest`, `MesaRequest`, `ReservaRequest` con las anotaciones de validación de la sección 7 y mensajes en español.
- [x] `ErrorResponse` (`timestamp`, `status`, `error`, `mensaje`, `ruta`, `detalles`).
- [x] Test de serialización: `DatosRestaurante` → JSON → `DatosRestaurante` conserva `LocalDate`/`LocalTime` como `"2026-12-05"` / `"20:00"` (no timestamps numéricos).

**Criterios de aceptación**
- Ida y vuelta JSON sin pérdida de datos.
- Los DTOs rechazan `null`, vacíos y valores ≤ 0 (verificable con `Validator` en un test unitario).

**Commit:** `feat(fase2): modelo de datos y DTOs con validaciones`

---

### FASE 3 — Persistencia en JSON (capa Repository)

**Objetivo:** leer/escribir `datos.json` de forma segura y exponer repositories por entidad.

**`JsonFileStore` (E/S del archivo)**
- [x] `@Component` con `@Value("${app.datos.ruta}")`, `ObjectMapper` inyectado, y `ReentrantReadWriteLock`.
- [x] Al iniciar (`@PostConstruct`): si el archivo **no existe**, crear directorios y escribir la estructura vacía válida; si existe, validar que se pueda leer (si no, lanzar `PersistenciaException` **sin tocar el archivo**).
- [x] Método `<T> T leer(Function<DatosRestaurante, T> consulta)` (bajo lock de lectura; lee del disco en cada llamada).
- [x] Método `<T> T modificar(Function<DatosRestaurante, T> cambio)` (bajo lock de escritura: leer → aplicar → guardar).
- [x] Escritura **atómica**: escribir a `datos.json.tmp` en el mismo directorio y luego `Files.move(tmp, destino, REPLACE_EXISTING)` (intentar `ATOMIC_MOVE` con fallback). Opcional: copiar a `.bak` antes de sobrescribir.
- [x] Capturar `IOException`/`JsonProcessingException` y lanzar `PersistenciaException` con mensaje genérico (el detalle va a `log.error`, no a la respuesta).

**Repositories**
- [x] `ClienteRepository`: `findAll()`, `findById(Long)`, `save(Cliente)` (si `id == null` asigna `max+1`; si existe, reemplaza), `deleteById(Long)`, `existsById(Long)`.
- [x] `MesaRepository`: lo mismo + `findByNumero(Integer)`.
- [x] `ReservaRepository`: `findAll()`, `findById`, `save`, `findByMesaIdAndFecha(Long, LocalDate)`, `existsByClienteId`, `existsByMesaId`. (No hay borrado de reservas: se cancelan.)
- [x] Todos con `@Repository` y Javadoc que indique que solo hablan con `JsonFileStore`.

**Tests (con `@TempDir`, sin Spring cuando sea posible)**
- [x] Archivo inexistente → se crea con estructura válida.
- [x] Guardar con una instancia y leer con **otra nueva** (simula reinicio) → datos intactos.
- [x] IDs autoincrementales por colección.
- [x] JSON corrupto → `PersistenciaException` y el contenido original **no cambia**.
- [x] Tras guardar no queda archivo `.tmp` huérfano.

**Criterios de aceptación**
- Todos los tests de persistencia en verde.
- Ningún Repository usa Spring Data ni JPA.

**Commit:** `feat(fase3): persistencia en JSON con lectura/escritura atómica y repositories`

---

### FASE 4 — Excepciones y manejo global de errores

**Objetivo:** respuestas de error coherentes (400/404/409/500) sin trazas.

- [x] `RecursoNoEncontradoException` (404), `SolicitudInvalidaException` (400), `ConflictoException` (409), `PersistenciaException` (500). Todas `RuntimeException`, con Javadoc.
- [x] `GlobalExceptionHandler` con `@RestControllerAdvice` que devuelva `ResponseEntity<ErrorResponse>`:
  - `RecursoNoEncontradoException` → 404
  - `SolicitudInvalidaException` → 400
  - `ConflictoException` → 409
  - `MethodArgumentNotValidException` → 400 con `detalles` por campo
  - `HttpMessageNotReadableException` (JSON mal formado, fecha/hora inválida, enum inválido) → 400
  - `MethodArgumentTypeMismatchException` (ej. `/api/clientes/abc`) → 400
  - `MissingServletRequestParameterException` → 400
  - `PersistenciaException` y `Exception` genérica → 500 con mensaje genérico ("Error interno del servidor"); el detalle solo se loguea.
- [x] Test del handler (con un controller de prueba o `@WebMvcTest`) verificando status y que **el cuerpo no contenga** `Exception`, `at com.`, ni rutas de clases.

**Criterios de aceptación**
- Cada tipo de error produce el status y el formato `ErrorResponse` de la sección 6.

**Commit:** `feat(fase4): manejo global de excepciones con ResponseEntity`

---

### FASE 5 — Services: Clientes y Mesas (CRUD)

**Objetivo:** reglas de negocio de clientes y mesas.

- [x] `ClienteService` (`@Service`, `@RequiredArgsConstructor`): `listar()`, `obtener(id)`, `crear(request)`, `actualizar(id, request)`, `eliminar(id)`. Reglas 5 y 6 de la sección 7.
- [x] `MesaService`: mismo CRUD + reglas 5, 6 y 7. Incluir (stub o implementado ya) `disponibles(fecha, hora, personas)` — se completa en la Fase 6.
- [x] Mapeo `Request → modelo` dentro del Service (método privado o clase `Mapper` simple; sin librerías externas).
- [x] Tests unitarios con Mockito (`@ExtendWith(MockitoExtension.class)`):
  - crear cliente OK; crear mesa con número duplicado → `ConflictoException`
  - obtener/actualizar/eliminar inexistente → `RecursoNoEncontradoException`
  - eliminar con reservas asociadas → `ConflictoException`
  - actualizar manteniendo su propio número de mesa → OK (no se considera duplicado)
  - reducir capacidad bajo reserva activa → `ConflictoException`

**Criterios de aceptación**
- Services sin ninguna dependencia de `ResponseEntity` ni de clases web.
- Tests verdes.

**Commit:** `feat(fase5): servicios de clientes y mesas con reglas de negocio`

---

### FASE 6 — Service de Reservas y disponibilidad

**Objetivo:** el núcleo del sistema.

- [x] `ReservaService`: inyectar `ReservaRepository`, `ClienteRepository`, `MesaRepository`, `ReservasProperties`, `Clock`.
- [x] Método privado `haySolapamiento(mesaId, fecha, hora, duracion, idAExcluir)` aplicando la regla de la sección 3.
- [x] `crear(request)`, `modificar(id, request)`, `cancelar(id)` — **`synchronized`** (comentar por qué).
- [x] `listar(fecha, estado, clienteId, mesaId)` (filtros opcionales, combinables), `obtener(id)`.
- [x] `MesaService.disponibles(fecha, hora, personas)`: validar parámetros (`personas > 0`, horario) y devolver mesas libres con capacidad suficiente.
- [x] Tests unitarios (Mockito + `Clock.fixed(...)`):
  - crear OK
  - cliente inexistente → 404; mesa inexistente → 404
  - fecha/hora pasada → 400; fuera de horario → 400; personas > capacidad → 400; personas ≤ 0 → 400
  - **solapamiento**: exacto, parcial al inicio, parcial al final → 409
  - **contiguo permitido** (ej. 20:00–22:00 y 22:00–24:00 según horario) y **otra mesa permitida**
  - una reserva `CANCELADA` **no** bloquea (cancelar libera disponibilidad)
  - modificar: OK, cambia de mesa/hora, no choca consigo misma, reserva inexistente → 404, reserva cancelada → 409, nuevo horario solapado → 409
  - cancelar: OK, ya cancelada → 409, inexistente → 404
  - disponibilidad: excluye mesas ocupadas y mesas pequeñas; tras cancelar vuelve a aparecer

**Criterios de aceptación**
- [x] Todas las reglas de la sección 7 cubiertas por al menos un test.
- [x] La cancelación demostrablemente libera la mesa (test).

**Commit:** `feat(fase6): servicio de reservas, disponibilidad y cancelación`

---

### FASE 7 — Controllers REST

**Objetivo:** exponer la API con `ResponseEntity` y códigos HTTP correctos.

- [x] `ClienteController` (`@RestController`, `@RequestMapping("/api/clientes")`): GET lista, GET por id, POST (`201` + `Location` vía `ResponseEntity.created(URI)`), PUT, DELETE (`ResponseEntity.noContent().build()` → 204).
- [x] `MesaController` (`/api/mesas`): mismo CRUD + `GET /disponibles` con `@RequestParam` (`fecha` y `hora` con `@DateTimeFormat`).
- [x] `ReservaController` (`/api/reservas`): GET lista con filtros opcionales, GET por id, POST (201), PUT (200), **`PATCH /{id}/cancelar`** (200).
- [x] Todos los métodos devuelven `ResponseEntity<...>`; usan `@Valid @RequestBody`; **solo** llaman al Service.
- [x] Javadoc en cada endpoint: método, ruta, códigos posibles.
- [x] Tests `@WebMvcTest` por controller (con `@MockitoBean` de los services; si la versión de Boot es < 3.4 usar `@MockBean`):
  - códigos 200/201/204 en caso feliz, header `Location` en 201
  - body inválido → 400 con `detalles`
  - service lanza 404/409 → el handler responde 404/409
  - `/api/mesas/disponibles` no es capturado por `/api/mesas/{id}`
  - `/api/clientes/abc` → 400

**Criterios de aceptación**
- Ningún Controller importa un Repository.
- Todos los endpoints de la sección 6 existen y responden con el status indicado.

**Commit:** `feat(fase7): controladores REST con ResponseEntity`

---

### FASE 8 — Pruebas de integración end-to-end

**Objetivo:** verificar la API completa y la persistencia real.

- [ ] `FlujoCompletoIT` con `@SpringBootTest` + `@AutoConfigureMockMvc`; usar `@TempDir` y `@DynamicPropertySource` para apuntar `app.datos.ruta` a un archivo temporal (los tests **no** deben tocar `data/datos.json`).
- [ ] Escenario 1 — **CRUD completo** de clientes y mesas (crear, listar, obtener, actualizar, eliminar, 404 posterior).
- [ ] Escenario 2 — **Reserva completa**: crear → listar → obtener → modificar → cancelar.
- [ ] Escenario 3 — **Disponibilidad**: mesa aparece libre → se reserva → deja de aparecer → se cancela → vuelve a aparecer.
- [ ] Escenario 4 — **Persistencia tras reinicio**: crear datos, construir un **nuevo** contexto/`JsonFileStore` sobre el mismo archivo y comprobar que los datos siguen ahí (incluida la reserva cancelada con su estado).
- [ ] Escenario 5 — **Errores**: 400 (body inválido, fecha pasada, capacidad), 404 (ids inexistentes, cliente/mesa inexistente en reserva), 409 (solapamiento, duplicados, eliminar con reservas, cancelar dos veces). Verificar que ningún cuerpo de error contiene trazas.
- [ ] Verificar que el contenido de `datos.json` en disco refleja cada cambio (incluida la cancelación).

**Criterios de aceptación**
- `mvn clean verify` → verde.
- Los tests no dejan basura en `data/`.

**Commit:** `test(fase8): pruebas de integración end-to-end y persistencia`

---

### FASE 9 — Pruebas manuales reproducibles (Postman / HTTP)

**Objetivo:** evidencia reproducible para la evaluación.

- [ ] Dejar `data/datos.json` con datos de ejemplo (3+ mesas de distintas capacidades, 2+ clientes, 1–2 reservas con fechas futuras a 2026-11-18).
- [ ] `docs/requests.http` (formato IntelliJ/VS Code REST Client) con **todos** los endpoints y los casos de error, usando `@baseUrl = http://localhost:8080`.
- [ ] `docs/postman/SaborGourmet.postman_collection.json` (v2.1) con variable `{{baseUrl}}` y carpetas: `01 Clientes`, `02 Mesas`, `03 Disponibilidad`, `04 Reservas`, `05 Cancelación`, `06 Errores`. Cada request con su body de ejemplo y **tests de Postman** que verifiquen el código de estado esperado.
- [ ] Ejecutar la app (`mvn spring-boot:run`) y recorrer la colección. Registrar en `docs/evidencias.md`, para cada prueba: **método, URL, cuerpo, estado HTTP y respuesta**. Cubrir:
  - CRUD de clientes y mesas
  - crear / listar / consultar / modificar / cancelar reserva
  - disponibilidad antes y después de reservar y de cancelar
  - **reinicio**: detener la app, volver a arrancar, `GET /api/reservas` → datos intactos
  - errores 400, 404 y 409
- [ ] Copia del `datos.json` resultante como anexo (`docs/evidencias/datos_despues_de_pruebas.json`).

> Nota: el agente no puede tomar capturas de pantalla de Postman; el grupo las agrega al informe. En `evidencias.md`, dejar marcadores `[CAPTURA POSTMAN: descripción]` donde corresponda.

**Criterios de aceptación**
- Cualquier persona puede importar la colección y reproducir todo con la app levantada.

**Commit:** `docs(fase9): colección Postman, requests.http y evidencias`

---

### FASE 10 — Documentación y borrador del informe

**Objetivo:** README completo, comentarios revisados y material para el informe.

- [ ] Revisar que **toda** clase y método público tengan Javadoc/comentarios detallados (requisito explícito de la entrega).
- [ ] Completar `README.md`: requisitos (JDK 21, Maven), cómo compilar, ejecutar y correr tests, ruta y estructura del `datos.json`, tabla de endpoints, cómo importar la colección Postman, decisiones de diseño y limitaciones conocidas (instancia única, reutilización de IDs al borrar el último).
- [ ] Crear `docs/informe/borrador_informe.md` con esta estructura (el grupo lo traslada a Word/PDF):
  1. **Portada** — marcadores: título, asignatura (Desarrollo de Software Web II), logo IPSS, profesor, integrantes. *No inventar nombres.*
  2. **Índice**
  3. **Introducción** — contexto del restaurante y objetivo.
  4. **Desarrollo**
     - 4.1 Arquitectura CSR (diagrama simple en texto/Mermaid + responsabilidades)
     - 4.2 Modelo de datos (clases, relaciones por ID, estructura del JSON)
     - 4.3 Persistencia en JSON (lectura, escritura atómica, manejo de errores)
     - 4.4 **Explicación del código por archivo** (una subsección por archivo: propósito, fragmentos clave, decisiones)
     - 4.5 Endpoints REST con ejemplos de request/response y códigos HTTP (incluir método y ruta de cancelación)
     - 4.6 Validaciones y manejo de errores (tabla 400/404/409)
     - 4.7 Pruebas y evidencias (CRUD, cancelación, disponibilidad, reinicio, errores)
  5. **Conclusión**
  6. **Bibliografía** (documentación de Spring Boot, Jackson, RFC 9110 sobre semántica HTTP, etc., en el formato que pida el curso)
- [ ] Anexar al borrador el listado de archivos del proyecto con una línea de descripción cada uno.

**Formato final del informe (lo aplica el grupo en Word/PDF):** Arial 12, interlineado 1,15, texto justificado, páginas numeradas, portada con logo IPSS, nombre `EVU2_GRUPO_APELLIDO_NOMBRE`.

**Commit:** `docs(fase10): README, Javadoc y borrador del informe`

---

### FASE 11 — Revisión final y empaquetado

**Objetivo:** verificar contra la consigna y entregar.

- [ ] `mvn clean verify` en verde desde cero (clonar o limpiar `target/`).
- [ ] Arrancar la app con `data/datos.json` ausente → se crea con estructura válida.
- [ ] Recorrer la **matriz de trazabilidad** (sección 10) y marcar cada fila.
- [ ] Buscar y eliminar: `System.out.println`, código comentado, TODOs, imports sin usar.
- [ ] Verificar que el `pom.xml` solo tiene las 4 dependencias permitidas y que no hay JPA/H2/Thymeleaf.
- [ ] Verificar que ningún Controller usa un Repository y que ningún Service importa clases web.
- [ ] Verificar que ninguna respuesta de error filtra trazas (revisar `evidencias.md`).
- [ ] Contrastar el proyecto con la **rúbrica de la plataforma** (el grupo debe cargarla/pegarla en `docs/RUBRICA.md` si quiere que el agente la use como checklist adicional).
- [ ] Generar el `.zip` del proyecto sin `target/` ni `.idea/`.

**Commit:** `chore(fase11): revisión final y empaquetado`

---

## 9. Estado de avance (el agente actualiza esta tabla)

| Fase | Descripción | Estado |
|------|-------------|--------|
| 1 | Bootstrap del proyecto | ✅ Completada |
| 2 | Modelo de datos y DTOs | ✅ Completada |
| 3 | Persistencia JSON + Repositories | ✅ Completada |
| 4 | Excepciones y manejo global de errores | ✅ Completada |
| 5 | Services: Clientes y Mesas | ✅ Completada |
| 6 | Service de Reservas y disponibilidad | ✅ Completada |
| 7 | Controllers REST | ✅ Completada |
| 8 | Pruebas de integración | ⬜ Pendiente |
| 9 | Postman / HTTP / evidencias | ⬜ Pendiente |
| 10 | Documentación e informe (borrador) | ⬜ Pendiente |
| 11 | Revisión final y empaquetado | ⬜ Pendiente |

Leyenda: ⬜ Pendiente · 🟨 En curso · ✅ Completada

---

## 10. Matriz de trazabilidad (consigna → fase)

| Requisito de la evaluación | Fase(s) | Verificación |
|----------------------------|---------|--------------|
| 2.1 Controllers y Services que gestionan solicitudes y coordinan la persistencia en JSON | 5, 6, 7 | Tests de Service y Controller |
| 2.2 Endpoints REST con JSON y `ResponseEntity` | 7 | `@WebMvcTest`, Postman |
| 2.3 CRUD con lectura/escritura de archivo JSON | 3, 5, 8 | Tests de persistencia e integración |
| 2.4 Separación CSR + pruebas de solicitudes/respuestas | 1–8, 9 | Revisión de dependencias entre capas, `evidencias.md` |
| Modelo con IDs únicos y referencias por ID | 2, 3 | Tests de repositories |
| Duración/turnos para disponibilidad | 3 (decisiones), 6 | Tests de solapamiento |
| CRUD de clientes y mesas respetando relaciones | 5 | Tests 409 al eliminar con reservas |
| Reservas: crear, listar, consultar, modificar, cancelar | 6, 7 | Integración escenario 2 |
| Consulta de mesas disponibles (fecha, hora, personas) | 6, 7 | Integración escenario 3 |
| Cancelación libera disponibilidad | 6, 8 | Test unitario + integración |
| Datos se conservan al reiniciar | 3, 8, 9 | Escenario 4 + evidencia manual |
| Archivo `datos.json` con `clientes`, `mesas`, `reservas`; se crea si no existe | 3, 9 | Test de inicialización |
| Errores de E/S sin borrar datos | 3 | Test de JSON corrupto |
| Validaciones (obligatorios, positivos, existencia, capacidad, solapamiento) | 2, 5, 6 | Tests unitarios |
| Códigos 200/201/204/400/404/409 sin trazas | 4, 7, 8 | Tests del handler e integración |
| Documentar método y ruta de cancelación | 7, 10 | README e informe (`PATCH /api/reservas/{id}/cancelar`) |
| Comentarios detallados en el código | Todas, 10 | Revisión Javadoc |
| Colección Postman / solicitudes reproducibles | 9 | Archivos en `docs/` |
| Informe: código por archivo, CSR, modelo, JSON, endpoints con ejemplos, evidencias | 10 | `borrador_informe.md` |

---

## 11. Definition of Done (proyecto completo)

- [ ] `mvn clean verify` pasa en verde.
- [ ] La app arranca con `mvn spring-boot:run` y atiende en `http://localhost:8080`.
- [ ] Los 16 endpoints de la sección 6 funcionan con los códigos indicados.
- [ ] Los datos sobreviven a un reinicio (evidenciado).
- [ ] Ningún error HTTP expone trazas.
- [ ] Arquitectura CSR respetada (Controller → Service → Repository, sin saltos).
- [ ] `datos.json`, colección Postman, `requests.http`, README y borrador del informe presentes.
- [ ] Código comentado en español con Javadoc completo.
- [ ] Todas las filas de la matriz de trazabilidad marcadas.
