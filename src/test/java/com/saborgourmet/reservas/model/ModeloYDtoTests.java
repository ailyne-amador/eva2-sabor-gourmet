package com.saborgourmet.reservas.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saborgourmet.reservas.dto.ClienteRequest;
import com.saborgourmet.reservas.dto.MesaRequest;
import com.saborgourmet.reservas.dto.ReservaRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

/** Comprueba la serialización del modelo y las restricciones de los DTOs de entrada. */
@SpringBootTest
class ModeloYDtoTests {

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

    /** Verifica que las restricciones rechacen ausencias, textos vacíos y números no positivos. */
    @Test
    void dtoRechazaCamposObligatoriosInvalidos() {
        assertEquals(Set.of("nombre", "identificacion", "email", "telefono"), camposInvalidos(
                new ClienteRequest("", "", "correo-invalido", "")));
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
