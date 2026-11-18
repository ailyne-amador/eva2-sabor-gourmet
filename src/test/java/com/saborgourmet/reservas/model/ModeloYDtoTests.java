package com.saborgourmet.reservas.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saborgourmet.reservas.dto.ClienteRequest;
import com.saborgourmet.reservas.dto.MesaRequest;
import com.saborgourmet.reservas.dto.ReservaRequest;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

/** Comprueba la serialización del modelo y las restricciones de los DTOs de entrada. */
@SpringBootTest
class ModeloYDtoTests {

    @TempDir
    static Path directorioTemporal;

    @DynamicPropertySource
    static void registrarRutaTemporal(DynamicPropertyRegistry registro) {
        registro.add("app.datos.ruta", () -> directorioTemporal.resolve("datos.json").toString());
    }

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private Validator validator;

    /** Verifica que las fechas sean texto ISO y que el modelo sobreviva al ciclo JSON. */
    @Test
    void serializacionYDeserializacionConservaFechasYHoras() throws Exception {
        DatosRestaurante original = DatosRestaurante.builder()
                .reservas(List.of(Reserva.builder()
                        .id(1L)
                        .clienteId(1L)
                        .mesaId(2L)
                        .fecha(LocalDate.of(2026, 12, 5))
                        .hora(LocalTime.of(20, 0))
                        .duracionMinutos(120)
                        .cantidadPersonas(4)
                        .estado(EstadoReserva.ACTIVA)
                        .build()))
                .build();

        String json = jsonMapper.writeValueAsString(original);
        DatosRestaurante copia = jsonMapper.readValue(json, DatosRestaurante.class);

        assertTrue(json.contains("\"fecha\":\"2026-12-05\""));
        assertTrue(json.contains("\"hora\":\"20:00\""));
        assertEquals(original, copia);
    }
    /** Comprueba que archivos JSON antiguos con el campo retirado sigan cargando. */
    @Test
    void ignoraIdentificacionObsoletaEnClientePersistido() throws Exception {
        String json = """
                {"id":1,"nombre":"Camila Rojas","identificacion":"12.345.678-5",
                 "email":"camila@example.com","telefono":"+56912345678"}
                """;

        Cliente cliente = jsonMapper.readValue(json, Cliente.class);

        assertEquals(Long.valueOf(1L), cliente.getId());
        assertFalse(jsonMapper.writeValueAsString(cliente).contains("identificacion"));
    }

    /** Verifica que las restricciones rechacen ausencias, textos vacíos y números no positivos. */
    @Test
    void dtoRechazaCamposObligatoriosInvalidos() {
        assertEquals(Set.of("nombre", "email", "telefono"), camposInvalidos(
                new ClienteRequest("", "correo-invalido", "")));
        assertEquals(Set.of("numero", "capacidad"), camposInvalidos(
                new MesaRequest(null, 0, null)));
        assertEquals(Set.of("clienteId", "mesaId", "fecha", "hora", "cantidadPersonas"), camposInvalidos(
                new ReservaRequest(null, 0L, null, null, -1)));
    }

    /** Obtiene los nombres de los campos que incumplen al menos una restricción. */
    private Set<String> camposInvalidos(Object solicitud) {
        return validator.validate(solicitud).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
