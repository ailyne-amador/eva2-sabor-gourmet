package com.saborgourmet.reservas.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saborgourmet.reservas.model.DatosRestaurante;
import com.saborgourmet.reservas.model.EstadoReserva;
import com.saborgourmet.reservas.repository.JsonFileStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import tools.jackson.databind.ObjectMapper;

/** Verifica la API completa y el almacenamiento JSON aislado de los datos reales del proyecto. */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoCompletoIT {

    @TempDir
    static Path directorioTemporal;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonFileStore almacenamiento;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registrarRutaTemporal(DynamicPropertyRegistry registro) {
        registro.add("app.datos.ruta", () -> archivoDatos().toString());
    }

    /** Aísla cada escenario y restablece sus colecciones sin usar data/datos.json. */
    @BeforeEach
    void limpiarDatos() {
        almacenamiento.modificar(datos -> {
            datos.getClientes().clear();
            datos.getMesas().clear();
            datos.getReservas().clear();
            return null;
        });
    }

    /** Comprueba que la escritura atómica no deje archivos temporales huérfanos. */
    @AfterEach
    void comprobarSinArchivoTemporal() {
        assertFalse(Files.exists(archivoDatos().resolveSibling("datos.json.tmp")));
    }

    /** Recorre el CRUD de clientes y mesas, incluidos los 404 posteriores a eliminar. */
    @Test
    void completaCrudDeClientesYMesas() throws Exception {
        long clienteId = crearCliente("Camila Rojas", "camila@example.com");
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(clienteId));
        mockMvc.perform(get("/api/clientes/{id}", clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Camila Rojas"));

        mockMvc.perform(put("/api/clientes/{id}", clienteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clienteJson("Camila Soto", "camila.soto@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Camila Soto"));
        assertEquals("Camila Soto", leerDatosPersistidos().getClientes().get(0).getNombre());

        mockMvc.perform(delete("/api/clientes/{id}", clienteId)).andExpect(status().isNoContent());
        assertTrue(leerDatosPersistidos().getClientes().isEmpty());
        esperarError(get("/api/clientes/{id}", clienteId), 404);

        long mesaId = crearMesa(11, 4, "Terraza");
        mockMvc.perform(get("/api/mesas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(mesaId));
        mockMvc.perform(get("/api/mesas/{id}", mesaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(11));

        mockMvc.perform(put("/api/mesas/{id}", mesaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mesaJson(12, 6, "Salón principal")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(12));
        assertEquals(12, leerDatosPersistidos().getMesas().get(0).getNumero());

        mockMvc.perform(delete("/api/mesas/{id}", mesaId)).andExpect(status().isNoContent());
        assertTrue(leerDatosPersistidos().getMesas().isEmpty());
        esperarError(get("/api/mesas/{id}", mesaId), 404);
    }

    /** Comprueba crear, listar, consultar, modificar y cancelar una reserva por HTTP. */
    @Test
    void completaFlujoDeReserva() throws Exception {
        long clienteId = crearCliente("Diego Pérez", "diego@example.com");
        long mesaId = crearMesa(21, 4, "Salón principal");
        LocalDate fecha = LocalDate.now().plusDays(7);
        long reservaId = crearReserva(clienteId, mesaId, fecha, "18:00", 2);

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reservaId));
        mockMvc.perform(get("/api/reservas/{id}", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        mockMvc.perform(put("/api/reservas/{id}", reservaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservaJson(clienteId, mesaId, fecha, "19:00", 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadPersonas").value(3));
        DatosRestaurante datosModificados = leerDatosPersistidos();
        assertEquals(LocalTime.of(19, 0), datosModificados.getReservas().get(0).getHora());
        assertEquals(3, datosModificados.getReservas().get(0).getCantidadPersonas());

        mockMvc.perform(patch("/api/reservas/{id}/cancelar", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        assertEquals(EstadoReserva.CANCELADA, leerDatosPersistidos().getReservas().get(0).getEstado());
    }

    /** Comprueba que reservar ocupa una mesa y cancelar vuelve a liberarla. */
    @Test
    void disponibilidadCambiaAlReservarYCancelar() throws Exception {
        long clienteId = crearCliente("Elena Díaz", "elena@example.com");
        long mesaId = crearMesa(31, 2, "Ventana");
        LocalDate fecha = LocalDate.now().plusDays(8);
        String disponibilidad = "/api/mesas/disponibles?fecha=" + fecha + "&hora=18:00&personas=2";

        mockMvc.perform(get(disponibilidad))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(mesaId));
        long reservaId = crearReserva(clienteId, mesaId, fecha, "18:00", 2);
        mockMvc.perform(get(disponibilidad)).andExpect(status().isOk()).andExpect(content().json("[]"));

        mockMvc.perform(patch("/api/reservas/{id}/cancelar", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        assertEquals(EstadoReserva.CANCELADA, leerDatosPersistidos().getReservas().get(0).getEstado());
        mockMvc.perform(get(disponibilidad))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(mesaId));
    }

    /** Lee el mismo archivo con un JsonFileStore nuevo para simular el reinicio de la aplicación. */
    @Test
    void conservaDatosTrasReiniciarElAlmacenamiento() throws Exception {
        long clienteId = crearCliente("Francisca León", "francisca@example.com");
        long mesaId = crearMesa(41, 4, "Patio");
        long reservaId = crearReserva(clienteId, mesaId, LocalDate.now().plusDays(9), "20:00", 4);
        mockMvc.perform(patch("/api/reservas/{id}/cancelar", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        JsonFileStore almacenamientoReiniciado = new JsonFileStore(archivoDatos().toString(), objectMapper);
        almacenamientoReiniciado.inicializar();
        DatosRestaurante datosReiniciados = almacenamientoReiniciado.leer(datos -> datos);

        assertEquals(clienteId, datosReiniciados.getClientes().get(0).getId());
        assertEquals(mesaId, datosReiniciados.getMesas().get(0).getId());
        assertEquals(reservaId, datosReiniciados.getReservas().get(0).getId());
        assertEquals(EstadoReserva.CANCELADA, datosReiniciados.getReservas().get(0).getEstado());
    }

    /** Cubre respuestas 400/404/409 del flujo real y comprueba que no filtran trazas. */
    @Test
    void respondeErroresDeNegocioSinTrazas() throws Exception {
        long clienteId = crearCliente("Gabriela Núñez", "gabriela@example.com");
        long mesaId = crearMesa(51, 2, "Interior");
        LocalDate fecha = LocalDate.now().plusDays(10);

        MvcResult cuerpoInvalido = esperarError(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"), 400);
        assertTrue(cuerpoInvalido.getResponse().getContentAsString().contains("detalles"));

        esperarError(post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservaJson(clienteId, mesaId, LocalDate.now().minusDays(1), "18:00", 2)), 400);
        esperarError(post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservaJson(clienteId, mesaId, fecha, "18:00", 3)), 400);

        esperarError(get("/api/clientes/999"), 404);
        esperarError(get("/api/mesas/999"), 404);
        esperarError(get("/api/reservas/999"), 404);
        esperarError(post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservaJson(999, mesaId, fecha, "18:00", 2)), 404);
        esperarError(post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservaJson(clienteId, 999, fecha, "18:00", 2)), 404);

        esperarError(post("/api/mesas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mesaJson(51, 4, "Duplicada")), 409);

        long reservaId = crearReserva(clienteId, mesaId, fecha, "18:00", 2);
        esperarError(post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservaJson(clienteId, mesaId, fecha, "19:00", 2)), 409);
        esperarError(delete("/api/clientes/{id}", clienteId), 409);
        esperarError(delete("/api/mesas/{id}", mesaId), 409);

        mockMvc.perform(patch("/api/reservas/{id}/cancelar", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        assertEquals(EstadoReserva.CANCELADA, leerDatosPersistidos().getReservas().get(0).getEstado());
        esperarError(patch("/api/reservas/{id}/cancelar", reservaId), 409);
    }

    private long crearCliente(String nombre, String email) throws Exception {
        long id = crear("/api/clientes", clienteJson(nombre, email));
        assertTrue(leerDatosPersistidos().getClientes().stream().anyMatch(cliente -> id == cliente.getId()));
        return id;
    }

    private long crearMesa(int numero, int capacidad, String ubicacion) throws Exception {
        long id = crear("/api/mesas", mesaJson(numero, capacidad, ubicacion));
        assertTrue(leerDatosPersistidos().getMesas().stream().anyMatch(mesa -> id == mesa.getId()));
        return id;
    }

    private long crearReserva(long clienteId, long mesaId, LocalDate fecha, String hora, int personas) throws Exception {
        long id = crear("/api/reservas", reservaJson(clienteId, mesaId, fecha, hora, personas));
        assertTrue(leerDatosPersistidos().getReservas().stream().anyMatch(reserva -> id == reserva.getId()));
        return id;
    }

    private long crear(String ruta, String cuerpo) throws Exception {
        MvcResult resultado = mockMvc.perform(post(ruta)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        String location = resultado.getResponse().getHeader(HttpHeaders.LOCATION);
        assertNotNull(location);
        String prefijo = ruta + "/";
        assertTrue(location.startsWith(prefijo), location);
        return Long.parseLong(location.substring(prefijo.length()));
    }

    private MvcResult esperarError(RequestBuilder solicitud, int estadoEsperado) throws Exception {
        MvcResult resultado = mockMvc.perform(solicitud)
                .andExpect(status().is(estadoEsperado))
                .andExpect(jsonPath("$.status").value(estadoEsperado))
                .andReturn();
        String cuerpo = resultado.getResponse().getContentAsString();
        assertFalse(cuerpo.contains("Exception"), cuerpo);
        assertFalse(cuerpo.contains("at com."), cuerpo);
        assertFalse(cuerpo.contains("Caused by:"), cuerpo);
        return resultado;
    }

    private DatosRestaurante leerDatosPersistidos() throws IOException {
        Path archivo = archivoDatos();
        assertTrue(Files.isRegularFile(archivo));
        assertFalse(Files.exists(archivo.resolveSibling("datos.json.tmp")));
        return objectMapper.readValue(archivo.toFile(), DatosRestaurante.class);
    }

    private static Path archivoDatos() {
        return directorioTemporal.resolve("datos.json");
    }

    private static String clienteJson(String nombre, String email) {
        return """
                {"nombre":"%s","email":"%s","telefono":"+56912345678"}
                """.formatted(nombre, email);
    }

    private static String mesaJson(int numero, int capacidad, String ubicacion) {
        return """
                {"numero":%d,"capacidad":%d,"ubicacion":"%s"}
                """.formatted(numero, capacidad, ubicacion);
    }

    private static String reservaJson(long clienteId, long mesaId, LocalDate fecha, String hora, int personas) {
        return """
                {"clienteId":%d,"mesaId":%d,"fecha":"%s","hora":"%s","cantidadPersonas":%d}
                """.formatted(clienteId, mesaId, fecha, hora, personas);
    }
}
