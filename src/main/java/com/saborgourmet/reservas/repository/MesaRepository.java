package com.saborgourmet.reservas.repository;

import com.saborgourmet.reservas.model.Mesa;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Repository de mesas; accede únicamente al conjunto JSON mediante {@link JsonFileStore}. */
@Repository
public class MesaRepository {

    private final JsonFileStore almacenamiento;

    /**
     * Construye el repository con el almacenamiento JSON compartido.
     *
     * @param almacenamiento componente encargado de leer y guardar el archivo
     */
    public MesaRepository(JsonFileStore almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    /** @return todas las mesas presentes en el archivo JSON */
    public List<Mesa> findAll() {
        return almacenamiento.leer(datos -> datos.getMesas());
    }

    /**
     * Busca una mesa por su identificador.
     *
     * @param id identificador de la mesa
     * @return mesa encontrada o {@link Optional#empty()} si no existe
     */
    public Optional<Mesa> findById(Long id) {
        return almacenamiento.leer(datos -> datos.getMesas().stream()
                .filter(mesa -> Objects.equals(mesa.getId(), id))
                .findFirst());
    }

    /**
     * Busca una mesa por su número visible en el restaurante.
     *
     * @param numero número de mesa
     * @return mesa encontrada o {@link Optional#empty()} si no existe
     */
    public Optional<Mesa> findByNumero(Integer numero) {
        return almacenamiento.leer(datos -> datos.getMesas().stream()
                .filter(mesa -> Objects.equals(mesa.getNumero(), numero))
                .findFirst());
    }

    /**
     * Inserta la mesa asignándole el siguiente ID o reemplaza el registro con el mismo ID.
     *
     * @param mesa mesa que se guardará
     * @return mesa guardada, con su ID asignado cuando correspondía
     */
    public Mesa save(Mesa mesa) {
        return almacenamiento.modificar(datos -> {
            if (mesa.getId() == null) {
                mesa.setId(datos.getMesas().stream()
                        .map(Mesa::getId)
                        .filter(Objects::nonNull)
                        .max(Long::compareTo)
                        .orElse(0L) + 1L);
            }
            int indice = -1;
            for (int i = 0; i < datos.getMesas().size(); i++) {
                if (Objects.equals(datos.getMesas().get(i).getId(), mesa.getId())) {
                    indice = i;
                    break;
                }
            }
            if (indice < 0) {
                datos.getMesas().add(mesa);
            } else {
                datos.getMesas().set(indice, mesa);
            }
            return mesa;
        });
    }

    /**
     * Elimina la mesa con el ID indicado; no hace nada si no existe.
     *
     * @param id identificador de la mesa que se eliminará
     */
    public void deleteById(Long id) {
        almacenamiento.modificar(datos -> datos.getMesas().removeIf(mesa -> Objects.equals(mesa.getId(), id)));
    }

    /**
     * Comprueba si existe una mesa con el ID indicado.
     *
     * @param id identificador de la mesa
     * @return {@code true} cuando la mesa está persistida
     */
    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }
}
