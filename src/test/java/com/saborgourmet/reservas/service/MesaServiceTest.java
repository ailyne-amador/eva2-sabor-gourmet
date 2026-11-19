package com.saborgourmet.reservas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saborgourmet.reservas.config.ReservasProperties;
import com.saborgourmet.reservas.dto.MesaRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.exception.SolicitudInvalidaException;
import com.saborgourmet.reservas.model.EstadoReserva;
import com.saborgourmet.reservas.model.Mesa;
import com.saborgourmet.reservas.model.Reserva;
import com.saborgourmet.reservas.repository.MesaRepository;
import com.saborgourmet.reservas.repository.ReservaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica CRUD, integridad referencial, capacidad futura y disponibilidad de mesas. */
@ExtendWith(MockitoExtension.class)
class MesaServiceTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    private MesaService servicio;

    @BeforeEach
    void inicializar() {
        ReservasProperties propiedades = new ReservasProperties();
        propiedades.setDuracionMinutos(120);
        propiedades.setHoraApertura(LocalTime.of(12, 0));
        propiedades.setHoraCierre(LocalTime.of(23, 0));
        Clock reloj = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);
        servicio = new MesaService(mesaRepository, reservaRepository, propiedades, reloj);
    }

    @Test
    void crearGuardaMesaConNumeroUnico() {
        Mesa guardada = mesa(1L, 1, 4);
        when(mesaRepository.findByNumero(1)).thenReturn(Optional.empty());
        when(mesaRepository.save(any(Mesa.class))).thenReturn(guardada);

        Mesa resultado = servicio.crear(solicitud(1, 4));

        assertSame(guardada, resultado);
    }

    @Test
    void crearConNumeroDuplicadoFalla() {
        when(mesaRepository.findByNumero(1)).thenReturn(Optional.of(mesa(1L, 1, 4)));

        assertThrows(ConflictoException.class, () -> servicio.crear(solicitud(1, 4)));
        verify(mesaRepository, never()).save(any(Mesa.class));
    }

    @Test
    void operacionesPorIdInexistenteDevuelvenNoEncontrado() {
        when(mesaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(9L));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.actualizar(9L, solicitud(1, 4)));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(9L));
    }

    @Test
    void eliminarMesaConReservasFallaSinBorrar() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa(1L, 1, 4)));
        when(reservaRepository.existsByMesaId(1L)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.eliminar(1L));
        verify(mesaRepository, never()).deleteById(1L);
    }

    @Test
    void actualizarConservandoSuNumeroEsValido() {
        Mesa existente = mesa(1L, 1, 4);
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(mesaRepository.findByNumero(1)).thenReturn(Optional.of(existente));
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Mesa resultado = servicio.actualizar(1L, solicitud(1, 6));

        assertEquals(1L, resultado.getId());
        assertEquals(6, resultado.getCapacidad());
    }

    @Test
    void actualizarConNumeroDeOtraMesaFalla() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa(1L, 1, 4)));
        when(mesaRepository.findByNumero(2)).thenReturn(Optional.of(mesa(2L, 2, 4)));

        assertThrows(ConflictoException.class, () -> servicio.actualizar(1L, solicitud(2, 4)));
        verify(mesaRepository, never()).save(any(Mesa.class));
    }

    @Test
    void reducirCapacidadBajoUnaReservaActivaFuturaFalla() {
        Mesa existente = mesa(1L, 1, 6);
        Reserva reservaFutura = reserva(1L, LocalDate.of(2026, 12, 5), LocalTime.of(20, 0), 4,
                EstadoReserva.ACTIVA);
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(mesaRepository.findByNumero(1)).thenReturn(Optional.of(existente));
        when(reservaRepository.findAll()).thenReturn(List.of(reservaFutura));

        assertThrows(ConflictoException.class, () -> servicio.actualizar(1L, solicitud(1, 3)));
        verify(mesaRepository, never()).save(any(Mesa.class));
    }

    @Test
    void reducirCapacidadNoConflictaConReservaCanceladaOPasada() {
        Mesa existente = mesa(1L, 1, 6);
        Reserva canceladaFutura = reserva(1L, LocalDate.of(2026, 12, 5), LocalTime.of(20, 0), 5,
                EstadoReserva.CANCELADA);
        Reserva activaPasada = reserva(1L, LocalDate.of(2026, 11, 17), LocalTime.of(20, 0), 5,
                EstadoReserva.ACTIVA);
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(mesaRepository.findByNumero(1)).thenReturn(Optional.of(existente));
        when(reservaRepository.findAll()).thenReturn(List.of(canceladaFutura, activaPasada));
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Mesa resultado = servicio.actualizar(1L, solicitud(1, 3));

        assertEquals(3, resultado.getCapacidad());
    }

    @Test
    void disponibilidadExcluyeMesasOcupadasYPequenasPeroPermiteContiguasOCanceladas() {
        LocalDate fecha = LocalDate.of(2026, 12, 5);
        when(mesaRepository.findAll()).thenReturn(List.of(
                mesa(1L, 1, 4), mesa(2L, 2, 2), mesa(3L, 3, 4), mesa(4L, 4, 4)));
        when(reservaRepository.findAll()).thenReturn(List.of(
                reserva(1L, fecha, LocalTime.of(19, 0), 4, EstadoReserva.ACTIVA),
                reserva(3L, fecha, LocalTime.of(20, 0), 4, EstadoReserva.CANCELADA),
                reserva(4L, fecha, LocalTime.of(22, 0), 4, EstadoReserva.ACTIVA)));

        List<Mesa> disponibles = servicio.disponibles(fecha, LocalTime.of(20, 0), 4);

        assertEquals(Set.of(3L, 4L), disponibles.stream().map(Mesa::getId).collect(Collectors.toSet()));
    }

    @Test
    void disponibilidadRechazaCantidadNoPositivaYHorarioFueraDeAtencion() {
        LocalDate fecha = LocalDate.of(2026, 12, 5);

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.disponibles(fecha, LocalTime.of(20, 0), 0));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.disponibles(fecha, LocalTime.of(22, 0), 2));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.disponibles(null, LocalTime.of(20, 0), 2));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.disponibles(fecha, null, 2));
    }

    private MesaRequest solicitud(int numero, int capacidad) {
        return new MesaRequest(numero, capacidad, "Salón principal");
    }

    private Mesa mesa(Long id, int numero, int capacidad) {
        return Mesa.builder()
                .id(id)
                .numero(numero)
                .capacidad(capacidad)
                .ubicacion("Salón principal")
                .build();
    }

    private Reserva reserva(Long mesaId, LocalDate fecha, LocalTime hora, int personas, EstadoReserva estado) {
        return Reserva.builder()
                .id(1L)
                .clienteId(1L)
                .mesaId(mesaId)
                .fecha(fecha)
                .hora(hora)
                .duracionMinutos(120)
                .cantidadPersonas(personas)
                .estado(estado)
                .build();
    }
}
