package com.saborgourmet.reservas.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saborgourmet.reservas.dto.MesaRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.model.Mesa;
import com.saborgourmet.reservas.service.MesaService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Comprueba el contrato HTTP del controlador de mesas con su service simulado. */
@WebMvcTest(MesaController.class)
class MesaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MesaService servicio;

    @Test
    void exponeCrudConCodigosYLocation() throws Exception {
        Mesa mesa = Mesa.builder().id(1L).numero(7).capacidad(4).ubicacion("Salón").build();
        when(servicio.listar()).thenReturn(List.of(mesa));
        when(servicio.obtener(1L)).thenReturn(mesa);
        when(servicio.crear(any(MesaRequest.class))).thenReturn(mesa);
        when(servicio.actualizar(eq(1L), any(MesaRequest.class))).thenReturn(mesa);

        mockMvc.perform(get("/api/mesas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value(7));
        mockMvc.perform(get("/api/mesas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidad").value(4));
        mockMvc.perform(post("/api/mesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":7,\"capacidad\":4,\"ubicacion\":\"Salón\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/mesas/1"));
        mockMvc.perform(put("/api/mesas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":7,\"capacidad\":4,\"ubicacion\":\"Salón\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(delete("/api/mesas/1")).andExpect(status().isNoContent());
        verify(servicio).eliminar(1L);
    }

    @Test
    void disponibilidadUsaLaRutaLiteralYConvierteLosParametros() throws Exception {
        Mesa mesa = Mesa.builder().id(2L).numero(8).capacidad(4).ubicacion("Terraza").build();
        LocalDate fecha = LocalDate.of(2026, 12, 5);
        LocalTime hora = LocalTime.of(20, 0);
        when(servicio.disponibles(fecha, hora, 2)).thenReturn(List.of(mesa));

        mockMvc.perform(get("/api/mesas/disponibles")
                        .param("fecha", "2026-12-05")
                        .param("hora", "20:00")
                        .param("personas", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
        verify(servicio).disponibles(fecha, hora, 2);
    }

    @Test
    void validaElCuerpoYTraduceErroresDelServicio() throws Exception {
        String respuesta = mockMvc.perform(post("/api/mesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":0,\"capacidad\":0}"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(respuesta.contains("\"detalles\":["), respuesta);

        when(servicio.obtener(404L)).thenThrow(new RecursoNoEncontradoException("No existe la mesa"));
        when(servicio.crear(any(MesaRequest.class))).thenThrow(new ConflictoException("Número duplicado"));
        mockMvc.perform(get("/api/mesas/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(post("/api/mesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":7,\"capacidad\":4}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
