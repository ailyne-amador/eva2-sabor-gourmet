package com.saborgourmet.reservas.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saborgourmet.reservas.dto.ReservaRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.model.EstadoReserva;
import com.saborgourmet.reservas.model.Reserva;
import com.saborgourmet.reservas.service.ReservaService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Comprueba el contrato HTTP del controlador de reservas con su service simulado. */
@WebMvcTest(ReservaController.class)
class ReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService servicio;

    @Test
    void exponeConsultasModificacionYCancelacion() throws Exception {
        Reserva activa = reserva(5L, EstadoReserva.ACTIVA);
        Reserva cancelada = reserva(5L, EstadoReserva.CANCELADA);
        LocalDate fecha = LocalDate.of(2026, 12, 5);
        when(servicio.listar(fecha, EstadoReserva.ACTIVA, 3L, 4L)).thenReturn(List.of(activa));
        when(servicio.obtener(5L)).thenReturn(activa);
        when(servicio.crear(any(ReservaRequest.class))).thenReturn(activa);
        when(servicio.modificar(eq(5L), any(ReservaRequest.class))).thenReturn(activa);
        when(servicio.cancelar(5L)).thenReturn(cancelada);

        mockMvc.perform(get("/api/reservas")
                        .param("fecha", "2026-12-05")
                        .param("estado", "ACTIVA")
                        .param("clienteId", "3")
                        .param("mesaId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"));
        verify(servicio).listar(fecha, EstadoReserva.ACTIVA, 3L, 4L);
        mockMvc.perform(get("/api/reservas/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2026-12-05"));
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoReserva()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/reservas/5"));
        mockMvc.perform(put("/api/reservas/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoReserva()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
        mockMvc.perform(patch("/api/reservas/5/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void cuerpoInvalidoDevuelve400ConDetalles() throws Exception {
        String respuesta = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(respuesta.contains("\"detalles\":["), respuesta);
        assertTrue(respuesta.contains("clienteId:"), respuesta);
    }

    @Test
    void traduceErroresDeReservaA404Y409() throws Exception {
        when(servicio.obtener(404L)).thenThrow(new RecursoNoEncontradoException("No existe la reserva"));
        when(servicio.cancelar(9L)).thenThrow(new ConflictoException("La reserva ya está cancelada"));

        mockMvc.perform(get("/api/reservas/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(patch("/api/reservas/9/cancelar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private Reserva reserva(Long id, EstadoReserva estado) {
        return Reserva.builder()
                .id(id)
                .clienteId(3L)
                .mesaId(4L)
                .fecha(LocalDate.of(2026, 12, 5))
                .hora(LocalTime.of(20, 0))
                .duracionMinutos(120)
                .cantidadPersonas(2)
                .estado(estado)
                .build();
    }

    private String cuerpoReserva() {
        return "{\"clienteId\":3,\"mesaId\":4,\"fecha\":\"2026-12-05\",\"hora\":\"20:00\",\"cantidadPersonas\":2}";
    }
}
