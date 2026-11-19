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

import com.saborgourmet.reservas.dto.ClienteRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.model.Cliente;
import com.saborgourmet.reservas.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Comprueba el contrato HTTP del controlador de clientes con su service simulado. */
@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService servicio;

    @Test
    void exponeCrudConCodigosYLocation() throws Exception {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Ana Rojas")
                .email("ana@example.com")
                .telefono("+56912345678")
                .build();
        when(servicio.listar()).thenReturn(java.util.List.of(cliente));
        when(servicio.obtener(1L)).thenReturn(cliente);
        when(servicio.crear(any(ClienteRequest.class))).thenReturn(cliente);
        when(servicio.actualizar(eq(1L), any(ClienteRequest.class))).thenReturn(cliente);

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        mockMvc.perform(get("/api/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Rojas\",\"email\":\"ana@example.com\",\"telefono\":\"+56912345678\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/clientes/1"));
        mockMvc.perform(put("/api/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Rojas\",\"email\":\"ana@example.com\",\"telefono\":\"+56912345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(delete("/api/clientes/1")).andExpect(status().isNoContent());
        verify(servicio).eliminar(1L);
    }

    @Test
    void cuerpoInvalidoDevuelve400ConDetalles() throws Exception {
        String respuesta = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(respuesta.contains("\"detalles\":["), respuesta);
        assertTrue(respuesta.contains("nombre: El nombre es obligatorio"), respuesta);
    }

    @Test
    void traduceErroresDelServicioYUnIdMalFormado() throws Exception {
        when(servicio.obtener(404L)).thenThrow(new RecursoNoEncontradoException("No existe el cliente"));
        when(servicio.obtener(409L)).thenThrow(new ConflictoException("El cliente tiene reservas"));

        mockMvc.perform(get("/api/clientes/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/api/clientes/409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mockMvc.perform(get("/api/clientes/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
