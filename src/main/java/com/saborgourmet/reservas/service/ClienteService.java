package com.saborgourmet.reservas.service;

import com.saborgourmet.reservas.dto.ClienteRequest;
import com.saborgourmet.reservas.exception.ConflictoException;
import com.saborgourmet.reservas.exception.RecursoNoEncontradoException;
import com.saborgourmet.reservas.model.Cliente;
import com.saborgourmet.reservas.repository.ClienteRepository;
import com.saborgourmet.reservas.repository.ReservaRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Coordina los casos de uso de clientes en la arquitectura CSR: recibe los DTOs,
 * aplica reglas de negocio y delega la persistencia JSON en los repositories.
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;

    /**
     * Lista los clientes registrados.
     *
     * @return clientes persistidos
     */
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    /**
     * Busca un cliente por ID.
     *
     * @param id identificador del cliente
     * @return cliente encontrado
     * @throws RecursoNoEncontradoException si no existe un cliente con ese ID
     */
    public Cliente obtener(Long id) {
        return buscarObligatorio(id);
    }

    /**
     * Crea un cliente si su identificación aún no está registrada.
     *
     * @param solicitud campos validados del cliente
     * @return cliente guardado con ID asignado
     * @throws ConflictoException si otra ficha ya usa la identificación
     */
    public Cliente crear(ClienteRequest solicitud) {
        validarIdentificacionDisponible(solicitud.identificacion(), null);
        return clienteRepository.save(mapear(null, solicitud));
    }

    /**
     * Actualiza todos los datos del cliente, conservando su ID y permitiendo
     * mantener su propia identificación.
     *
     * @param id identificador del cliente que se actualizará
     * @param solicitud nuevos campos validados
     * @return cliente actualizado
     * @throws RecursoNoEncontradoException si no existe un cliente con ese ID
     * @throws ConflictoException si otro cliente ya usa la identificación solicitada
     */
    public Cliente actualizar(Long id, ClienteRequest solicitud) {
        buscarObligatorio(id);
        validarIdentificacionDisponible(solicitud.identificacion(), id);
        return clienteRepository.save(mapear(id, solicitud));
    }

    /**
     * Elimina un cliente que existe y no conserva reservas asociadas, incluso canceladas.
     *
     * @param id identificador del cliente que se eliminará
     * @throws RecursoNoEncontradoException si no existe un cliente con ese ID
     * @throws ConflictoException si alguna reserva conserva una referencia al cliente
     */
    public void eliminar(Long id) {
        buscarObligatorio(id);
        if (reservaRepository.existsByClienteId(id)) {
            throw new ConflictoException("No se puede eliminar el cliente porque tiene reservas asociadas");
        }
        clienteRepository.deleteById(id);
    }

    private Cliente buscarObligatorio(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un cliente con ID " + id));
    }

    private void validarIdentificacionDisponible(String identificacion, Long idActual) {
        boolean duplicada = clienteRepository.findByIdentificacion(identificacion)
                .filter(cliente -> idActual == null || !Objects.equals(cliente.getId(), idActual))
                .isPresent();
        if (duplicada) {
            throw new ConflictoException("La identificación " + identificacion + " ya está registrada");
        }
    }

    private Cliente mapear(Long id, ClienteRequest solicitud) {
        return Cliente.builder()
                .id(id)
                .nombre(solicitud.nombre())
                .identificacion(solicitud.identificacion())
                .email(solicitud.email())
                .telefono(solicitud.telefono())
                .build();
    }
}
