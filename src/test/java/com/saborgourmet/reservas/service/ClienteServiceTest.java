package com.saborgourmet.reservas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saborgourmet.reservas.dto.ClienteRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.model.Cliente;
import com.saborgourmet.reservas.repository.ClienteRepository;
import com.saborgourmet.reservas.repository.ReservaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica las reglas de negocio del servicio de clientes usando repositories simulados. */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ReservaRepository reservaRepository;

    private ClienteService servicio;

    @BeforeEach
    void inicializar() {
        servicio = new ClienteService(clienteRepository, reservaRepository);
    }

    @Test
    void crearGuardaClienteConIdentificacionUnica() {
        Cliente guardado = cliente(1L, "12-3");
        when(clienteRepository.findByIdentificacion("12-3")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenReturn(guardado);

        Cliente resultado = servicio.crear(solicitud("12-3"));

        assertSame(guardado, resultado);
        verify(clienteRepository).save(argThat(cliente -> cliente.getId() == null
                && cliente.getIdentificacion().equals("12-3")
                && cliente.getNombre().equals("Ana Rojas")));
    }

    @Test
    void crearConIdentificacionDuplicadaFalla() {
        when(clienteRepository.findByIdentificacion("12-3")).thenReturn(Optional.of(cliente(1L, "12-3")));

        assertThrows(ConflictoException.class, () -> servicio.crear(solicitud("12-3")));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void operacionesPorIdInexistenteDevuelvenNoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(9L));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.actualizar(9L, solicitud("12-3")));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(9L));
    }

    @Test
    void eliminarClienteConReservasFallaSinBorrar() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, "12-3")));
        when(reservaRepository.existsByClienteId(1L)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.eliminar(1L));
        verify(clienteRepository, never()).deleteById(1L);
    }

    @Test
    void actualizarConservandoSuIdentificacionEsValido() {
        Cliente existente = cliente(1L, "12-3");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByIdentificacion("12-3")).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Cliente resultado = servicio.actualizar(1L, new ClienteRequest(
                "Ana Rojas actualizada", "12-3", "ana@example.com", "+56912345678"));

        assertEquals(1L, resultado.getId());
        assertEquals("Ana Rojas actualizada", resultado.getNombre());
        verify(clienteRepository).save(argThat(cliente -> cliente.getId().equals(1L)));
    }

    @Test
    void actualizarConIdentificacionDeOtroClienteFalla() {
        Cliente existente = cliente(1L, "12-3");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByIdentificacion("45-6")).thenReturn(Optional.of(cliente(2L, "45-6")));

        assertThrows(ConflictoException.class, () -> servicio.actualizar(1L, solicitud("45-6")));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    private ClienteRequest solicitud(String identificacion) {
        return new ClienteRequest("Ana Rojas", identificacion, "ana@example.com", "+56912345678");
    }

    private Cliente cliente(Long id, String identificacion) {
        return Cliente.builder()
                .id(id)
                .nombre("Ana Rojas")
                .identificacion(identificacion)
                .email("ana@example.com")
                .telefono("+56912345678")
                .build();
    }
}
