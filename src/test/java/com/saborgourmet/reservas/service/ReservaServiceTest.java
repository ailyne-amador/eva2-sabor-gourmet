package com.saborgourmet.reservas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saborgourmet.reservas.config.ReservasProperties;
import com.saborgourmet.reservas.dto.ReservaRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.exception.SolicitudInvalidaException;
import com.saborgourmet.reservas.model.Cliente;
import com.saborgourmet.reservas.model.EstadoReserva;
import com.saborgourmet.reservas.model.Mesa;
import com.saborgourmet.reservas.model.Reserva;
import com.saborgourmet.reservas.repository.ClienteRepository;
import com.saborgourmet.reservas.repository.MesaRepository;
import com.saborgourmet.reservas.repository.ReservaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica las reglas de creación, modificación, cancelación y consulta de reservas. */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final LocalDate FECHA_FUTURA = LocalDate.of(2026, 12, 5);

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MesaRepository mesaRepository;

    private ReservasProperties propiedades;
    private Clock reloj;
    private ReservaService servicio;

    @BeforeEach
    void inicializar() {
        propiedades = new ReservasProperties();
        propiedades.setDuracionMinutos(120);
        propiedades.setHoraApertura(LocalTime.of(12, 0));
        propiedades.setHoraCierre(LocalTime.of(23, 0));
        reloj = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);
        servicio = new ReservaService(reservaRepository, clienteRepository, mesaRepository, propiedades, reloj);
    }

    @Test
    void crearGuardaReservaActivaConDuracionConfigurada() {
        Mesa mesa = mesa(2L, 2, 4);
        prepararReferencias(mesa);
        when(reservaRepository.findByMesaIdAndFecha(2L, FECHA_FUTURA)).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Reserva resultado = servicio.crear(solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), 4));

        assertEquals(1L, resultado.getClienteId());
        assertEquals(2L, resultado.getMesaId());
        assertEquals(120, resultado.getDuracionMinutos());
        assertEquals(EstadoReserva.ACTIVA, resultado.getEstado());
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void crearConClienteInexistenteDevuelveNoEncontrado() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.crear(solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), 4)));

        verify(mesaRepository, never()).findById(2L);
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void crearConMesaInexistenteDevuelveNoEncontrado() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(mesaRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.crear(solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), 4)));

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void crearRechazaFechaPasadaHorarioFueraDeAtencionYCapacidadExcedida() {
        prepararReferencias(mesa(2L, 2, 4));

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear(solicitud(1L, 2L, LocalDate.of(2026, 11, 17), LocalTime.of(20, 0), 4)));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear(solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(22, 0), 4)));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear(solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), 5)));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.crear(solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), 0)));

        verify(reservaRepository, never()).findByMesaIdAndFecha(anyLong(), any(LocalDate.class));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void crearRechazaSolapamientoExactoYParciales() {
        prepararReferencias(mesa(2L, 2, 4));
        when(reservaRepository.findByMesaIdAndFecha(2L, FECHA_FUTURA)).thenReturn(List.of(
                reserva(10L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), EstadoReserva.ACTIVA, 4)));

        for (LocalTime hora : List.of(LocalTime.of(20, 0), LocalTime.of(19, 0), LocalTime.of(21, 0))) {
            assertThrows(ConflictoException.class,
                    () -> servicio.crear(solicitud(1L, 2L, FECHA_FUTURA, hora, 4)));
        }

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void crearPermiteHorarioContiguoYNoConsideraReservasCanceladas() {
        prepararReferencias(mesa(2L, 2, 4));
        when(reservaRepository.findByMesaIdAndFecha(2L, FECHA_FUTURA)).thenReturn(List.of(
                reserva(10L, 2L, FECHA_FUTURA, LocalTime.of(18, 0), EstadoReserva.ACTIVA, 4),
                reserva(11L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), EstadoReserva.CANCELADA, 4)));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Reserva resultado = servicio.crear(solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), 4));

        assertEquals(EstadoReserva.ACTIVA, resultado.getEstado());
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void crearPermiteElMismoHorarioEnOtraMesa() {
        prepararReferencias(mesa(2L, 2, 4));
        when(reservaRepository.findByMesaIdAndFecha(2L, FECHA_FUTURA)).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Reserva resultado = servicio.crear(solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), 4));

        assertEquals(2L, resultado.getMesaId());
        verify(reservaRepository).findByMesaIdAndFecha(2L, FECHA_FUTURA);
    }

    @Test
    void modificarCambiaMesaYHora() {
        Reserva existente = reserva(1L, 1L, FECHA_FUTURA, LocalTime.of(20, 0), EstadoReserva.ACTIVA, 2);
        prepararReferencias(mesa(2L, 2, 4));
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(reservaRepository.findByMesaIdAndFecha(2L, FECHA_FUTURA)).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Reserva resultado = servicio.modificar(
                1L, solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(19, 0), 4));

        assertEquals(1L, resultado.getId());
        assertEquals(2L, resultado.getMesaId());
        assertEquals(LocalTime.of(19, 0), resultado.getHora());
    }

    @Test
    void modificarNoColisionaConsigoMisma() {
        Reserva existente = reserva(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), EstadoReserva.ACTIVA, 4);
        prepararReferencias(mesa(2L, 2, 4));
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(reservaRepository.findByMesaIdAndFecha(2L, FECHA_FUTURA)).thenReturn(List.of(existente));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Reserva resultado = servicio.modificar(
                1L, solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), 4));

        assertEquals(1L, resultado.getId());
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void modificarInexistenteOCanceladaFalla() {
        Reserva cancelada = reserva(2L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), EstadoReserva.CANCELADA, 2);
        when(reservaRepository.findById(9L)).thenReturn(Optional.empty());
        when(reservaRepository.findById(2L)).thenReturn(Optional.of(cancelada));
        ReservaRequest solicitud = solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), 2);

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.modificar(9L, solicitud));
        assertThrows(ConflictoException.class, () -> servicio.modificar(2L, solicitud));

        verify(clienteRepository, never()).findById(anyLong());
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void modificarFallaCuandoElNuevoHorarioSeSuperpone() {
        Reserva existente = reserva(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), EstadoReserva.ACTIVA, 2);
        Reserva otra = reserva(2L, 2L, FECHA_FUTURA, LocalTime.of(19, 0), EstadoReserva.ACTIVA, 4);
        prepararReferencias(mesa(2L, 2, 4));
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(reservaRepository.findByMesaIdAndFecha(2L, FECHA_FUTURA)).thenReturn(List.of(existente, otra));

        assertThrows(ConflictoException.class,
                () -> servicio.modificar(1L, solicitud(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), 4)));

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void cancelarActualizaEstadoYRechazaUnaSegundaCancelacion() {
        Reserva existente = reserva(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), EstadoReserva.ACTIVA, 4);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Reserva resultado = servicio.cancelar(1L);

        assertEquals(EstadoReserva.CANCELADA, resultado.getEstado());
        assertThrows(ConflictoException.class, () -> servicio.cancelar(1L));
        verify(reservaRepository).save(existente);
    }

    @Test
    void cancelarReservaInexistenteDevuelveNoEncontrado() {
        when(reservaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.cancelar(9L));

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void listarCombinaFiltrosYObtenerBuscaPorId() {
        Reserva coincidente = reserva(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), EstadoReserva.ACTIVA, 4);
        Reserva otraFecha = reserva(2L, 2L, FECHA_FUTURA.plusDays(1), LocalTime.of(20, 0), EstadoReserva.ACTIVA, 4);
        Reserva otroCliente = reserva(3L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), EstadoReserva.ACTIVA, 4);
        otroCliente.setClienteId(2L);
        when(reservaRepository.findAll()).thenReturn(List.of(coincidente, otraFecha, otroCliente));
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(coincidente));
        when(reservaRepository.findById(9L)).thenReturn(Optional.empty());

        assertEquals(List.of(coincidente), servicio.listar(FECHA_FUTURA, EstadoReserva.ACTIVA, 1L, 2L));
        assertSame(coincidente, servicio.obtener(1L));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(9L));
    }

    @Test
    void cancelarLiberaMesaEnLaConsultaDeDisponibilidad() {
        Mesa mesa = mesa(2L, 2, 4);
        Reserva existente = reserva(1L, 2L, FECHA_FUTURA, LocalTime.of(20, 0), EstadoReserva.ACTIVA, 4);
        List<Reserva> reservas = new ArrayList<>(List.of(existente));
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(reservaRepository.findAll()).thenAnswer(invocacion -> List.copyOf(reservas));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> {
            Reserva guardada = invocacion.getArgument(0);
            reservas.set(0, guardada);
            return guardada;
        });
        when(mesaRepository.findAll()).thenReturn(List.of(mesa));
        MesaService servicioMesas = new MesaService(mesaRepository, reservaRepository, propiedades, reloj);

        assertTrue(servicioMesas.disponibles(FECHA_FUTURA, LocalTime.of(20, 0), 4).isEmpty());
        servicio.cancelar(1L);

        assertEquals(List.of(mesa), servicioMesas.disponibles(FECHA_FUTURA, LocalTime.of(20, 0), 4));
    }

    private void prepararReferencias(Mesa... mesas) {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        for (Mesa mesa : mesas) {
            when(mesaRepository.findById(mesa.getId())).thenReturn(Optional.of(mesa));
        }
    }

    private ReservaRequest solicitud(Long clienteId, Long mesaId, LocalDate fecha, LocalTime hora, Integer personas) {
        return new ReservaRequest(clienteId, mesaId, fecha, hora, personas);
    }

    private Cliente cliente(Long id) {
        return Cliente.builder().id(id).nombre("Ana Pérez").email("ana@example.com").telefono("+56912345678").build();
    }

    private Mesa mesa(Long id, int numero, int capacidad) {
        return Mesa.builder().id(id).numero(numero).capacidad(capacidad).ubicacion("Salón principal").build();
    }

    private Reserva reserva(Long id, Long mesaId, LocalDate fecha, LocalTime hora,
            EstadoReserva estado, int personas) {
        return Reserva.builder()
                .id(id)
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
