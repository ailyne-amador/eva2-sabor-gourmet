package com.saborgourmet.reservas.controller;

import com.saborgourmet.reservas.dto.MesaRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.PersistenciaException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.exception.SolicitudInvalidaException;
import com.saborgourmet.reservas.model.Mesa;
import com.saborgourmet.reservas.service.MesaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP de mesas en CSR: delega reglas de negocio a MesaService y no accede al Repository. */
@RestController
@RequestMapping("/api/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final MesaService servicio;

    /**
     * GET /api/mesas; responde 200 con todas las mesas.
     *
     * @return lista de mesas
     * @throws PersistenciaException si falla la lectura del archivo JSON
     */
    @GetMapping
    public ResponseEntity<List<Mesa>> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    /**
     * GET /api/mesas/disponibles; responde 200 con las mesas libres o 400 si los parámetros son inválidos.
     *
     * @param fecha fecha solicitada en formato YYYY-MM-DD
     * @param hora hora de inicio en formato HH:mm
     * @param personas cantidad positiva de personas
     * @return mesas disponibles; puede ser una lista vacía
     * @throws SolicitudInvalidaException si la fecha, la hora o la cantidad no son válidas
     * @throws PersistenciaException si falla la lectura del archivo JSON
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<Mesa>> disponibles(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("hora") @DateTimeFormat(pattern = "HH:mm") LocalTime hora,
            @RequestParam("personas") Integer personas) {
        return ResponseEntity.ok(servicio.disponibles(fecha, hora, personas));
    }

    /**
     * GET /api/mesas/{id}; responde 200 o 404 si no existe la mesa.
     *
     * @param id identificador de la mesa
     * @return mesa encontrada
     * @throws RecursoNoEncontradoException si no existe la mesa
     * @throws PersistenciaException si falla la lectura del archivo JSON
     */
    @GetMapping("/{id}")
    public ResponseEntity<Mesa> obtener(@PathVariable("id") Long id) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    /**
     * POST /api/mesas; responde 201 con la mesa y su encabezado Location, 400 si el cuerpo es inválido o 409 si el número está duplicado.
     *
     * @param solicitud configuración validada de la mesa
     * @return mesa creada
     * @throws ConflictoException si otra mesa ya usa el número solicitado
     * @throws PersistenciaException si falla la escritura del archivo JSON
     */
    @PostMapping
    public ResponseEntity<Mesa> crear(@Valid @RequestBody MesaRequest solicitud) {
        Mesa mesa = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/mesas/" + mesa.getId())).body(mesa);
    }

    /**
     * PUT /api/mesas/{id}; responde 200, 400 si el cuerpo es inválido, 404 si no existe o 409 si hay conflicto.
     *
     * @param id identificador de la mesa que se actualizará
     * @param solicitud nueva configuración validada
     * @return mesa actualizada
     * @throws RecursoNoEncontradoException si no existe la mesa
     * @throws ConflictoException si el número está duplicado o la capacidad no admite una reserva activa futura
     * @throws PersistenciaException si falla la escritura del archivo JSON
     */
    @PutMapping("/{id}")
    public ResponseEntity<Mesa> actualizar(
            @PathVariable("id") Long id, @Valid @RequestBody MesaRequest solicitud) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud));
    }

    /**
     * DELETE /api/mesas/{id}; responde 204, 404 si no existe o 409 si conserva reservas.
     *
     * @param id identificador de la mesa que se eliminará
     * @return respuesta sin cuerpo
     * @throws RecursoNoEncontradoException si no existe la mesa
     * @throws ConflictoException si la mesa conserva reservas asociadas
     * @throws PersistenciaException si falla la escritura del archivo JSON
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
