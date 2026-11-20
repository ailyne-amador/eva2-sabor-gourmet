package com.saborgourmet.reservas.exception;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saborgourmet.reservas.dto.ClienteRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Comprueba que el manejador global traduzca los errores MVC y de negocio a JSON seguro. */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void prepararMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /** Verifica los estados, mensajes y estructura de las excepciones de negocio. */
    @Test
    void excepcionesDeNegocioDevuelvenElEstadoYFormatoEsperados() throws Exception {
        assertError("/prueba/error/no-encontrado", 404, "Not Found",
                "No existe la reserva 42");
        assertError("/prueba/error/solicitud-invalida", 400, "Bad Request",
                "La fecha solicitada ya pasó");
        assertError("/prueba/error/conflicto", 409, "Conflict",
                "La mesa ya está reservada");
    }

    /** Verifica que Bean Validation incluya errores por campo sin exponer detalles internos. */
    @Test
    void cuerpoConCamposInvalidosIncluyeDetallesDeValidacion() throws Exception {
        String cuerpo = respuesta(mockMvc.perform(post("/prueba/validar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")), 400);

        assertTrue(cuerpo.contains("\"timestamp\":"), cuerpo);
        assertTrue(cuerpo.contains("\"status\":400"), cuerpo);
        assertTrue(cuerpo.contains("\"error\":\"Bad Request\""), cuerpo);
        assertTrue(cuerpo.contains("\"mensaje\":\"La solicitud contiene campos inválidos\""), cuerpo);
        assertTrue(cuerpo.contains("\"ruta\":\"/prueba/validar\""), cuerpo);
        assertTrue(cuerpo.contains("\"detalles\":["), cuerpo);
        assertTrue(cuerpo.contains("nombre: El nombre es obligatorio"), cuerpo);
        assertFalse(cuerpo.contains("Exception"), cuerpo);
        assertFalse(cuerpo.contains("at com."), cuerpo);
    }

    /** Verifica que JSON ilegible, tipos incompatibles y parámetros faltantes respondan 400. */
    @Test
    void erroresDeFormatoYParametrosDevuelvenSolicitudInvalida() throws Exception {
        String jsonIlegible = respuesta(mockMvc.perform(post("/prueba/validar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{")), 400);
        comprobarError(jsonIlegible, 400, "Bad Request",
                "El cuerpo de la solicitud no es válido", "/prueba/validar");

        String tipoInvalido = respuesta(mockMvc.perform(get("/prueba/tipo/no-es-numero")), 400);
        comprobarError(tipoInvalido, 400, "Bad Request",
                "El parámetro 'id' tiene un formato inválido", "/prueba/tipo/no-es-numero");

        String parametroFaltante = respuesta(mockMvc.perform(get("/prueba/parametro")), 400);
        comprobarError(parametroFaltante, 400, "Bad Request",
                "Falta el parámetro obligatorio 'nombre'", "/prueba/parametro");
    }

    /** Verifica que los fallos de persistencia y no controlados solo expongan un mensaje genérico. */
    @Test
    void erroresInternosNoExponenExcepcionesNiTrazas() throws Exception {
        for (String ruta : new String[] {"/prueba/error/persistencia", "/prueba/error/inesperado"}) {
            String cuerpo = respuesta(mockMvc.perform(get(ruta)), 500);
            comprobarError(cuerpo, 500, "Internal Server Error",
                    "Error interno del servidor", ruta);
            assertFalse(cuerpo.contains("Causa privada"), cuerpo);
            assertFalse(cuerpo.contains("Detalle confidencial"), cuerpo);
            assertFalse(cuerpo.contains("implementación privado"), cuerpo);
            assertFalse(cuerpo.contains("IllegalStateException"), cuerpo);
            assertFalse(cuerpo.contains("Exception"), cuerpo);
            assertFalse(cuerpo.contains("at com."), cuerpo);
        }
    }

    private void assertError(String ruta, int codigo, String error, String mensaje) throws Exception {
        String cuerpo = respuesta(mockMvc.perform(get(ruta)), codigo);
        comprobarError(cuerpo, codigo, error, mensaje, ruta);
    }

    private String respuesta(ResultActions solicitud, int codigo) throws Exception {
        return solicitud.andExpect(status().is(codigo))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void comprobarError(
            String cuerpo, int codigo, String error, String mensaje, String ruta) {
        assertTrue(cuerpo.contains("\"timestamp\":"), cuerpo);
        assertTrue(cuerpo.contains("\"status\":" + codigo), cuerpo);
        assertTrue(cuerpo.contains("\"error\":\"" + error + "\""), cuerpo);
        assertTrue(cuerpo.contains("\"mensaje\":\"" + mensaje + "\""), cuerpo);
        assertTrue(cuerpo.contains("\"ruta\":\"" + ruta + "\""), cuerpo);
        assertTrue(cuerpo.contains("\"detalles\":[]"), cuerpo);
        assertFalse(cuerpo.contains("Exception"), cuerpo);
        assertFalse(cuerpo.contains("at com."), cuerpo);
    }

    /** Endpoints mínimos que generan excepciones para ejercitar el manejador sin iniciar Spring Boot. */
    @RestController
    public static class ControladorDePrueba {

        /**
         * Genera una excepción de negocio para probar su conversión a respuesta HTTP.
         *
         * @param tipo categoría de excepción que debe provocar el endpoint
         * @throws RecursoNoEncontradoException si tipo es `no-encontrado`
         * @throws SolicitudInvalidaException si tipo es `solicitud-invalida`
         * @throws ConflictoException si tipo es `conflicto`
         * @throws PersistenciaException si tipo es `persistencia`
         * @throws IllegalStateException si tipo es `inesperado`
         * @throws IllegalArgumentException si tipo no coincide con los casos admitidos
         */
        @GetMapping("/prueba/error/{tipo}")
        public void provocarError(@PathVariable String tipo) {
            switch (tipo) {
                case "no-encontrado" -> throw new RecursoNoEncontradoException("No existe la reserva 42");
                case "solicitud-invalida" -> throw new SolicitudInvalidaException("La fecha solicitada ya pasó");
                case "conflicto" -> throw new ConflictoException("La mesa ya está reservada");
                case "persistencia" -> throw new PersistenciaException(
                        "Detalle confidencial", new IllegalStateException("Causa privada"));
                case "inesperado" -> throw new IllegalStateException("Detalle de implementación privado");
                default -> throw new IllegalArgumentException("Tipo de prueba desconocido");
            }
        }

        /**
         * Convierte el identificador de ruta a número para probar errores de tipo.
         *
         * @param id identificador numérico requerido
         * @return identificador recibido cuando su formato es válido
         */
        @GetMapping("/prueba/tipo/{id}")
        public Long obtenerPorId(@PathVariable Long id) {
            return id;
        }

        /**
         * Requiere un parámetro para probar la respuesta ante parámetros ausentes.
         *
         * @param nombre valor obligatorio de consulta
         * @return valor recibido cuando el parámetro está presente
         */
        @GetMapping("/prueba/parametro")
        public String obtenerParametro(@RequestParam String nombre) {
            return nombre;
        }

        /**
         * Aplica validación de Bean Validation al cuerpo de prueba.
         *
         * @param solicitud datos de cliente recibidos
         * @return datos recibidos cuando todos sus campos son válidos
         */
        @PostMapping("/prueba/validar")
        public ClienteRequest validar(@Valid @RequestBody ClienteRequest solicitud) {
            return solicitud;
        }
    }
}
