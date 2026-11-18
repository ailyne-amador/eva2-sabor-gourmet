package com.saborgourmet.reservas.service;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Coordina los casos de uso de mesas en la arquitectura CSR: aplica reglas de
 * negocio y usa repositories para leer y guardar los datos JSON.
 */
@Service
@RequiredArgsConstructor
public class MesaService {

    private final MesaRepository mesaRepository;
    private final ReservaRepository reservaRepository;
    private final ReservasProperties propiedades;
    private final Clock reloj;

    /**
     * Lista las mesas registradas.
     *
     * @return mesas persistidas
     */
    public List<Mesa> listar() {
        return mesaRepository.findAll();
    }

    /**
     * Busca una mesa por ID.
     *
     * @param id identificador de la mesa
     * @return mesa encontrada
     * @throws RecursoNoEncontradoException si no existe una mesa con ese ID
     */
    public Mesa obtener(Long id) {
        return buscarObligatoria(id);
    }

    /**
     * Crea una mesa si su número no está registrado.
     *
     * @param solicitud campos validados de la mesa
     * @return mesa guardada con ID asignado
     * @throws ConflictoException si otra mesa ya usa el número solicitado
     */
    public Mesa crear(MesaRequest solicitud) {
        validarNumeroDisponible(solicitud.numero(), null);
        return mesaRepository.save(mapear(null, solicitud));
    }

    /**
     * Actualiza una mesa, conservando su ID y respetando las reservas activas futuras.
     *
     * @param id identificador de la mesa que se actualizará
     * @param solicitud nueva configuración validada
     * @return mesa actualizada
     * @throws RecursoNoEncontradoException si no existe una mesa con ese ID
     * @throws ConflictoException si el número pertenece a otra mesa o la capacidad
     *         nueva es insuficiente para una reserva activa futura
     */
    public Mesa actualizar(Long id, MesaRequest solicitud) {
        Mesa existente = buscarObligatoria(id);
        validarNumeroDisponible(solicitud.numero(), id);
        validarCapacidadFutura(existente, solicitud.capacidad());
        return mesaRepository.save(mapear(id, solicitud));
    }

    /**
     * Elimina una mesa que existe y no conserva reservas asociadas, incluso canceladas.
     *
     * @param id identificador de la mesa que se eliminará
     * @throws RecursoNoEncontradoException si no existe una mesa con ese ID
     * @throws ConflictoException si alguna reserva conserva una referencia a la mesa
     */
    public void eliminar(Long id) {
        buscarObligatoria(id);
        if (reservaRepository.existsByMesaId(id)) {
            throw new ConflictoException("No se puede eliminar la mesa porque tiene reservas asociadas");
        }
        mesaRepository.deleteById(id);
    }

    /**
     * Devuelve mesas con capacidad suficiente y sin reservas activas superpuestas
     * dentro del horario de atención.
     *
     * @param fecha fecha solicitada
     * @param hora hora de inicio solicitada
     * @param personas cantidad de personas
     * @return mesas disponibles; puede ser una lista vacía
     * @throws SolicitudInvalidaException si faltan parámetros, las personas no son
     *         positivas o el horario queda fuera de atención
     */
    public List<Mesa> disponibles(LocalDate fecha, LocalTime hora, Integer personas) {
        if (fecha == null) {
            throw new SolicitudInvalidaException("La fecha es obligatoria");
        }
        if (hora == null) {
            throw new SolicitudInvalidaException("La hora es obligatoria");
        }
        if (personas == null || personas <= 0) {
            throw new SolicitudInvalidaException("La cantidad de personas debe ser positiva");
        }
        validarHorario(hora);

        LocalDateTime inicioSolicitado = LocalDateTime.of(fecha, hora);
        LocalDateTime finSolicitado = inicioSolicitado.plusMinutes(propiedades.getDuracionMinutos());
        List<Reserva> reservas = reservaRepository.findAll();
        return mesaRepository.findAll().stream()
                .filter(mesa -> mesa.getCapacidad() >= personas)
                .filter(mesa -> reservas.stream().noneMatch(reserva ->
                        reserva.getEstado() == EstadoReserva.ACTIVA
                                && Objects.equals(reserva.getMesaId(), mesa.getId())
                                && fecha.equals(reserva.getFecha())
                                && seSuperpone(reserva, inicioSolicitado, finSolicitado)))
                .toList();
    }

    private Mesa buscarObligatoria(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una mesa con ID " + id));
    }

    private void validarNumeroDisponible(Integer numero, Long idActual) {
        boolean duplicado = mesaRepository.findByNumero(numero)
                .filter(mesa -> idActual == null || !Objects.equals(mesa.getId(), idActual))
                .isPresent();
        if (duplicado) {
            throw new ConflictoException("El número de mesa " + numero + " ya está registrado");
        }
    }

    private void validarCapacidadFutura(Mesa mesa, Integer capacidad) {
        if (capacidad >= mesa.getCapacidad()) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        boolean reservaIncompatible = reservaRepository.findAll().stream()
                .filter(reserva -> Objects.equals(reserva.getMesaId(), mesa.getId()))
                .filter(reserva -> reserva.getEstado() == EstadoReserva.ACTIVA)
                .filter(reserva -> LocalDateTime.of(reserva.getFecha(), reserva.getHora()).isAfter(ahora))
                .anyMatch(reserva -> reserva.getCantidadPersonas() > capacidad);
        if (reservaIncompatible) {
            throw new ConflictoException(
                    "La capacidad no puede ser menor que la cantidad de personas de una reserva activa futura");
        }
    }

    private void validarHorario(LocalTime hora) {
        long finEnSegundos = hora.toSecondOfDay() + propiedades.getDuracionMinutos().longValue() * 60L;
        if (hora.isBefore(propiedades.getHoraApertura())
                || finEnSegundos > propiedades.getHoraCierre().toSecondOfDay()) {
            throw new SolicitudInvalidaException("El horario solicitado está fuera del horario de atención");
        }
    }

    private boolean seSuperpone(Reserva reserva, LocalDateTime inicioSolicitado, LocalDateTime finSolicitado) {
        LocalDateTime inicioReserva = LocalDateTime.of(reserva.getFecha(), reserva.getHora());
        LocalDateTime finReserva = inicioReserva.plusMinutes(reserva.getDuracionMinutos());
        return inicioSolicitado.isBefore(finReserva) && inicioReserva.isBefore(finSolicitado);
    }

    private Mesa mapear(Long id, MesaRequest solicitud) {
        return Mesa.builder()
                .id(id)
                .numero(solicitud.numero())
                .capacidad(solicitud.capacidad())
                .ubicacion(solicitud.ubicacion())
                .build();
    }
}
