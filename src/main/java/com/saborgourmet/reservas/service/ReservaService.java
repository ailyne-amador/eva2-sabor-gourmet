package com.saborgourmet.reservas.service;

import com.saborgourmet.reservas.config.ReservasProperties;
import com.saborgourmet.reservas.dto.ReservaRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.exception.SolicitudInvalidaException;
import com.saborgourmet.reservas.model.EstadoReserva;
import com.saborgourmet.reservas.model.Mesa;
import com.saborgourmet.reservas.model.Reserva;
import com.saborgourmet.reservas.repository.ClienteRepository;
import com.saborgourmet.reservas.repository.MesaRepository;
import com.saborgourmet.reservas.repository.ReservaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Coordina las operaciones de reservas en la arquitectura CSR: valida reglas de
 * negocio y delega la lectura y escritura del JSON en los repositories.
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final MesaRepository mesaRepository;
    private final ReservasProperties propiedades;
    private final Clock reloj;

    // ponytail: synchronized coordina una instancia; varias réplicas requieren un bloqueo compartido.

    /**
     * Crea una reserva activa después de validar referencias, horario, capacidad y solapamientos.
     *
     * @param solicitud datos del cliente, mesa, fecha, hora y cantidad de personas
     * @return reserva guardada con ID, duración y estado asignados
     * @throws RecursoNoEncontradoException si el cliente o la mesa no existen
     * @throws SolicitudInvalidaException si la fecha, hora o cantidad incumplen las reglas
     * @throws ConflictoException si otra reserva activa se superpone en la misma mesa
     */
    public synchronized Reserva crear(ReservaRequest solicitud) {
        validarSolicitud(solicitud);
        Mesa mesa = validarReferencias(solicitud);
        validarReglas(solicitud, mesa, null);
        return reservaRepository.save(mapear(null, solicitud));
    }

    /**
     * Modifica una reserva activa, excluyéndola de su propia comprobación de solapamiento.
     *
     * @param id identificador de la reserva que se modificará
     * @param solicitud nuevos datos de la reserva
     * @return reserva actualizada con el mismo ID y estado activo
     * @throws RecursoNoEncontradoException si la reserva, el cliente o la mesa no existen
     * @throws SolicitudInvalidaException si la fecha, hora o cantidad incumplen las reglas
     * @throws ConflictoException si la reserva no está activa o existe un solapamiento
     */
    public synchronized Reserva modificar(Long id, ReservaRequest solicitud) {
        Reserva existente = buscarObligatoria(id);
        if (existente.getEstado() != EstadoReserva.ACTIVA) {
            throw new ConflictoException("Solo se pueden modificar reservas activas");
        }
        validarSolicitud(solicitud);
        Mesa mesa = validarReferencias(solicitud);
        validarReglas(solicitud, mesa, id);
        return reservaRepository.save(mapear(id, solicitud));
    }

    /**
     * Cancela una reserva sin borrarla, conservando el registro histórico y liberando la mesa.
     *
     * @param id identificador de la reserva que se cancelará
     * @return reserva guardada con estado {@link EstadoReserva#CANCELADA}
     * @throws RecursoNoEncontradoException si no existe una reserva con ese ID
     * @throws ConflictoException si la reserva ya estaba cancelada
     */
    public synchronized Reserva cancelar(Long id) {
        Reserva reserva = buscarObligatoria(id);
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ConflictoException("La reserva ya está cancelada");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(reserva);
    }

    /**
     * Lista reservas aplicando de forma combinada los filtros no nulos.
     *
     * @param fecha filtro opcional por fecha
     * @param estado filtro opcional por estado
     * @param clienteId filtro opcional por identificador de cliente
     * @param mesaId filtro opcional por identificador de mesa
     * @return reservas que cumplen todos los filtros informados
     */
    public List<Reserva> listar(LocalDate fecha, EstadoReserva estado, Long clienteId, Long mesaId) {
        return reservaRepository.findAll().stream()
                .filter(reserva -> fecha == null || fecha.equals(reserva.getFecha()))
                .filter(reserva -> estado == null || estado == reserva.getEstado())
                .filter(reserva -> clienteId == null || clienteId.equals(reserva.getClienteId()))
                .filter(reserva -> mesaId == null || mesaId.equals(reserva.getMesaId()))
                .toList();
    }

    /**
     * Busca una reserva por su identificador.
     *
     * @param id identificador de la reserva
     * @return reserva encontrada
     * @throws RecursoNoEncontradoException si no existe una reserva con ese ID
     */
    public Reserva obtener(Long id) {
        return buscarObligatoria(id);
    }

    private void validarSolicitud(ReservaRequest solicitud) {
        if (solicitud == null) {
            throw new SolicitudInvalidaException("La solicitud de reserva es obligatoria");
        }
    }

    private Mesa validarReferencias(ReservaRequest solicitud) {
        clienteRepository.findById(solicitud.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un cliente con ID " + solicitud.clienteId()));
        return mesaRepository.findById(solicitud.mesaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una mesa con ID " + solicitud.mesaId()));
    }

    private void validarReglas(ReservaRequest solicitud, Mesa mesa, Long idAExcluir) {
        validarFechaHora(solicitud.fecha(), solicitud.hora());
        validarHorario(solicitud.fecha(), solicitud.hora());
        validarCantidadPersonas(solicitud.cantidadPersonas());
        if (solicitud.cantidadPersonas() > mesa.getCapacidad()) {
            throw new SolicitudInvalidaException("La cantidad de personas supera la capacidad de la mesa");
        }
        if (haySolapamiento(mesa.getId(), solicitud.fecha(), solicitud.hora(),
                propiedades.getDuracionMinutos(), idAExcluir)) {
            throw new ConflictoException("La mesa " + mesa.getNumero()
                    + " ya tiene una reserva activa que se superpone con el horario solicitado");
        }
    }

    private void validarFechaHora(LocalDate fecha, LocalTime hora) {
        if (fecha == null) {
            throw new SolicitudInvalidaException("La fecha es obligatoria");
        }
        if (hora == null) {
            throw new SolicitudInvalidaException("La hora es obligatoria");
        }
        if (fecha.atTime(hora).isBefore(LocalDateTime.now(reloj))) {
            throw new SolicitudInvalidaException("La fecha y hora de la reserva no pueden ser anteriores a ahora");
        }
    }

    private void validarHorario(LocalDate fecha, LocalTime hora) {
        LocalDateTime inicio = fecha.atTime(hora);
        LocalDateTime fin = inicio.plusMinutes(propiedades.getDuracionMinutos());
        LocalDateTime apertura = fecha.atTime(propiedades.getHoraApertura());
        LocalDateTime cierre = fecha.atTime(propiedades.getHoraCierre());
        if (inicio.isBefore(apertura) || fin.isAfter(cierre)) {
            throw new SolicitudInvalidaException("El horario solicitado está fuera del horario de atención");
        }
    }

    private void validarCantidadPersonas(Integer personas) {
        if (personas == null || personas <= 0) {
            throw new SolicitudInvalidaException("La cantidad de personas debe ser positiva");
        }
    }

    private boolean haySolapamiento(Long mesaId, LocalDate fecha, LocalTime hora,
            Integer duracionMinutos, Long idAExcluir) {
        LocalDateTime inicioSolicitado = fecha.atTime(hora);
        LocalDateTime finSolicitado = inicioSolicitado.plusMinutes(duracionMinutos);
        return reservaRepository.findByMesaIdAndFecha(mesaId, fecha).stream()
                .filter(reserva -> reserva.getEstado() == EstadoReserva.ACTIVA)
                .filter(reserva -> idAExcluir == null || !idAExcluir.equals(reserva.getId()))
                .anyMatch(reserva -> {
                    LocalDateTime inicioExistente = reserva.getFecha().atTime(reserva.getHora());
                    LocalDateTime finExistente = inicioExistente.plusMinutes(reserva.getDuracionMinutos());
                    return inicioSolicitado.isBefore(finExistente) && inicioExistente.isBefore(finSolicitado);
                });
    }

    private Reserva buscarObligatoria(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una reserva con ID " + id));
    }

    private Reserva mapear(Long id, ReservaRequest solicitud) {
        return Reserva.builder()
                .id(id)
                .clienteId(solicitud.clienteId())
                .mesaId(solicitud.mesaId())
                .fecha(solicitud.fecha())
                .hora(solicitud.hora())
                .duracionMinutos(propiedades.getDuracionMinutos())
                .cantidadPersonas(solicitud.cantidadPersonas())
                .estado(EstadoReserva.ACTIVA)
                .build();
    }
}
