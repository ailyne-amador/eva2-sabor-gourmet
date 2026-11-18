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
    void crearGuardaCliente() {
        Cliente guardado = cliente(1L);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(guardado);

        Cliente resultado = servicio.crear(solicitud("Ana Rojas"));

        assertSame(guardado, resultado);
        verify(clienteRepository).save(argThat(cliente -> cliente.getId() == null
                && cliente.getNombre().equals("Ana Rojas")
                && cliente.getEmail().equals("ana@example.com")
                && cliente.getTelefono().equals("+56912345678")));
    }

    @Test
    void operacionesPorIdInexistenteDevuelvenNoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(9L));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.actualizar(9L, solicitud("Ana Rojas")));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(9L));
    }

    @Test
    void eliminarClienteConReservasFallaSinBorrar() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(reservaRepository.existsByClienteId(1L)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.eliminar(1L));
        verify(clienteRepository, never()).deleteById(1L);
    }

    @Test
    void actualizarConservaIdYActualizaDatos() {
        Cliente existente = cliente(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Cliente resultado = servicio.actualizar(1L, solicitud("Ana Rojas actualizada"));

        assertEquals(Long.valueOf(1L), resultado.getId());
        assertEquals("Ana Rojas actualizada", resultado.getNombre());
        verify(clienteRepository).save(argThat(cliente -> cliente.getId().equals(1L)));
    }

    private ClienteRequest solicitud(String nombre) {
        return new ClienteRequest(nombre, "ana@example.com", "+56912345678");
    }

    private Cliente cliente(Long id) {
        return Cliente.builder()
                .id(id)
                .nombre("Ana Rojas")
                .email("ana@example.com")
                .telefono("+56912345678")
                .build();
    }
}
