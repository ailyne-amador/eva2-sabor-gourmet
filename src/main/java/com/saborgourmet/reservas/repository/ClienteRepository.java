package com.saborgourmet.reservas.repository;

import com.saborgourmet.reservas.model.Cliente;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Repository de clientes; accede únicamente al conjunto JSON mediante {@link JsonFileStore}. */
@Repository
public class ClienteRepository {

    private final JsonFileStore almacenamiento;

    /**
     * Construye el repository con el almacenamiento JSON compartido.
     * @param almacenamiento componente encargado de leer y guardar el archivo
     */
    public ClienteRepository(JsonFileStore almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    /** @return todos los clientes presentes en el archivo JSON */
    public List<Cliente> findAll() {
        return almacenamiento.leer(datos -> datos.getClientes());
    }

    /**
     * Busca un cliente por su identificador.
     *
     * @param id identificador del cliente
     * @return cliente encontrado o {@link Optional#empty()} si no existe
     */
    public Optional<Cliente> findById(Long id) {
        return almacenamiento.leer(datos -> datos.getClientes().stream()
                .filter(cliente -> Objects.equals(cliente.getId(), id))
                .findFirst());
    }


    /**
     * Inserta el cliente asignándole el siguiente ID o reemplaza el registro con el mismo ID.
     *
     * @param cliente cliente que se guardará
     * @return cliente guardado, con su ID asignado cuando correspondía
     */
    public Cliente save(Cliente cliente) {
        return almacenamiento.modificar(datos -> {
            if (cliente.getId() == null) {
                cliente.setId(datos.getClientes().stream()
                        .map(Cliente::getId)
                        .filter(Objects::nonNull)
                        .max(Long::compareTo)
                        .orElse(0L) + 1L);
            }
            int indice = -1;
            for (int i = 0; i < datos.getClientes().size(); i++) {
                if (Objects.equals(datos.getClientes().get(i).getId(), cliente.getId())) {
                    indice = i;
                    break;
                }
            }
            if (indice < 0) {
                datos.getClientes().add(cliente);
            } else {
                datos.getClientes().set(indice, cliente);
            }
            return cliente;
        });
    }

    /**
     * Elimina el cliente con el ID indicado; no hace nada si no existe.
     *
     * @param id identificador del cliente que se eliminará
     */
    public void deleteById(Long id) {
        almacenamiento.modificar(datos -> datos.getClientes().removeIf(cliente -> Objects.equals(cliente.getId(), id)));
    }

    /**
     * Comprueba si existe un cliente con el ID indicado.
     *
     * @param id identificador del cliente
     * @return {@code true} cuando el cliente está persistido
     */
    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }
}
