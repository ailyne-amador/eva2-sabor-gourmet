package com.saborgourmet.reservas.controller;

import com.saborgourmet.reservas.dto.ReservaRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.PersistenciaException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.exception.SolicitudInvalidaException;
import com.saborgourmet.reservas.model.EstadoReserva;
import com.saborgourmet.reservas.model.Reserva;
import com.saborgourmet.reservas.service.ReservaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP de reservas en CSR: delega reglas de negocio a ReservaService y no accede al Repository. */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService servicio;

    /**
     * GET /api/reservas; responde 200 con reservas que cumplen todos los filtros informados, o 400 si un filtro no es válido.
     *
     * @param fecha filtro opcional en formato YYYY-MM-DD
     * @param estado filtro opcional ACTIVA o CANCELADA
     * @param clienteId filtro opcional por cliente
     * @param mesaId filtro opcional por mesa
     * @return reservas que cumplen los filtros
     * @throws PersistenciaException si falla la lectura del archivo JSON
     */
    @GetMapping
    public ResponseEntity<List<Reserva>> listar(
            @RequestParam(name = "fecha", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "estado", required = false) EstadoReserva estado,
            @RequestParam(name = "clienteId", required = false) Long clienteId,
            @RequestParam(name = "mesaId", required = false) Long mesaId) {
        return ResponseEntity.ok(servicio.listar(fecha, estado, clienteId, mesaId));
    }

    /**
     * GET /api/reservas/{id}; responde 200 o 404 si no existe la reserva.
     *
     * @param id identificador de la reserva
     * @return reserva encontrada
     * @throws RecursoNoEncontradoException si no existe la reserva
     * @throws PersistenciaException si falla la lectura del archivo JSON
     */
    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtener(@PathVariable("id") Long id) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    /**
     * POST /api/reservas; responde 201 con la reserva y su encabezado Location, 400 si es inválida, 404 si falta una referencia o 409 si hay solapamiento.
     *
     * @param solicitud datos validados de la reserva
     * @return reserva creada
     * @throws SolicitudInvalidaException si no cumple las reglas de fecha, horario o capacidad
     * @throws RecursoNoEncontradoException si no existe el cliente o la mesa solicitados
     * @throws ConflictoException si otra reserva activa se superpone en la mesa
     * @throws PersistenciaException si falla la escritura del archivo JSON
     */
    @PostMapping
    public ResponseEntity<Reserva> crear(@Valid @RequestBody ReservaRequest solicitud) {
        Reserva reserva = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/reservas/" + reserva.getId())).body(reserva);
    }

    /**
     * PUT /api/reservas/{id}; responde 200, 400 si es inválida, 404 si falta el recurso o una referencia, o 409 si no está activa o hay solapamiento.
     *
     * @param id identificador de la reserva que se modificará
     * @param solicitud nuevos datos validados de la reserva
     * @return reserva modificada
     * @throws SolicitudInvalidaException si no cumple las reglas de fecha, horario o capacidad
     * @throws RecursoNoEncontradoException si no existe la reserva, el cliente o la mesa
     * @throws ConflictoException si la reserva no está activa o se superpone con otra
     * @throws PersistenciaException si falla la escritura del archivo JSON
     */
    @PutMapping("/{id}")
    public ResponseEntity<Reserva> modificar(
            @PathVariable("id") Long id, @Valid @RequestBody ReservaRequest solicitud) {
        return ResponseEntity.ok(servicio.modificar(id, solicitud));
    }

    /**
     * PATCH /api/reservas/{id}/cancelar; responde 200, 404 si no existe o 409 si ya se canceló.
     *
     * @param id identificador de la reserva que se cancelará
     * @return reserva actualizada con estado CANCELADA
     * @throws RecursoNoEncontradoException si no existe la reserva
     * @throws ConflictoException si la reserva ya estaba cancelada
     * @throws PersistenciaException si falla la escritura del archivo JSON
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Reserva> cancelar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(servicio.cancelar(id));
    }
}
