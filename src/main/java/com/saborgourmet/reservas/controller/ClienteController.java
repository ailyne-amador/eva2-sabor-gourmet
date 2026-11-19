package com.saborgourmet.reservas.controller;

import com.saborgourmet.reservas.dto.ClienteRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.PersistenciaException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.model.Cliente;
import com.saborgourmet.reservas.service.ClienteService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP de clientes en CSR: delega reglas de negocio a ClienteService y no accede al Repository. */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService servicio;

    /**
     * GET /api/clientes; responde 200 con todos los clientes.
     *
     * @return lista de clientes
     * @throws PersistenciaException si falla la lectura del archivo JSON
     */
    @GetMapping
    public ResponseEntity<List<Cliente>> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    /**
     * GET /api/clientes/{id}; responde 200 o 404 si no existe el cliente.
     *
     * @param id identificador del cliente
     * @return cliente encontrado
     * @throws RecursoNoEncontradoException si no existe el cliente
     * @throws PersistenciaException si falla la lectura del archivo JSON
     */
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtener(@PathVariable("id") Long id) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    /**
     * POST /api/clientes; responde 201 con el cliente y su encabezado Location, o 400 si el cuerpo es inválido.
     *
     * @param solicitud campos validados del cliente
     * @return cliente creado
     * @throws PersistenciaException si falla la escritura del archivo JSON
     */
    @PostMapping
    public ResponseEntity<Cliente> crear(@Valid @RequestBody ClienteRequest solicitud) {
        Cliente cliente = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/clientes/" + cliente.getId())).body(cliente);
    }

    /**
     * PUT /api/clientes/{id}; responde 200, 400 si el cuerpo es inválido o 404 si no existe.
     *
     * @param id identificador del cliente que se actualizará
     * @param solicitud nuevos campos validados del cliente
     * @return cliente actualizado
     * @throws RecursoNoEncontradoException si no existe el cliente
     * @throws PersistenciaException si falla la escritura del archivo JSON
     */
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(
            @PathVariable("id") Long id, @Valid @RequestBody ClienteRequest solicitud) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud));
    }

    /**
     * DELETE /api/clientes/{id}; responde 204, 404 si no existe o 409 si conserva reservas.
     *
     * @param id identificador del cliente que se eliminará
     * @return respuesta sin cuerpo
     * @throws RecursoNoEncontradoException si no existe el cliente
     * @throws ConflictoException si el cliente conserva reservas asociadas
     * @throws PersistenciaException si falla la escritura del archivo JSON
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
