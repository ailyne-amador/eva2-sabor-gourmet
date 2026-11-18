package com.saborgourmet.reservas.repository;

import com.saborgourmet.reservas.model.Reserva;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Repository de reservas; accede únicamente al conjunto JSON mediante {@link JsonFileStore}. */
@Repository
public class ReservaRepository {

    private final JsonFileStore almacenamiento;

    /**
     * Construye el repository con el almacenamiento JSON compartido.
     *
     * @param almacenamiento componente encargado de leer y guardar el archivo
     */
    public ReservaRepository(JsonFileStore almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    /** @return todas las reservas presentes en el archivo JSON */
    public List<Reserva> findAll() {
        return almacenamiento.leer(datos -> datos.getReservas());
    }

    /**
     * Busca una reserva por su identificador.
     *
     * @param id identificador de la reserva
     * @return reserva encontrada o {@link Optional#empty()} si no existe
     */
    public Optional<Reserva> findById(Long id) {
        return almacenamiento.leer(datos -> datos.getReservas().stream()
                .filter(reserva -> Objects.equals(reserva.getId(), id))
                .findFirst());
    }

    /**
     * Inserta la reserva asignándole el siguiente ID o reemplaza el registro con el mismo ID.
     *
     * @param reserva reserva que se guardará
     * @return reserva guardada, con su ID asignado cuando correspondía
     */
    public Reserva save(Reserva reserva) {
        return almacenamiento.modificar(datos -> {
            if (reserva.getId() == null) {
                reserva.setId(datos.getReservas().stream()
                        .map(Reserva::getId)
                        .filter(Objects::nonNull)
                        .max(Long::compareTo)
                        .orElse(0L) + 1L);
            }
            int indice = -1;
            for (int i = 0; i < datos.getReservas().size(); i++) {
                if (Objects.equals(datos.getReservas().get(i).getId(), reserva.getId())) {
                    indice = i;
                    break;
                }
            }
            if (indice < 0) {
                datos.getReservas().add(reserva);
            } else {
                datos.getReservas().set(indice, reserva);
            }
            return reserva;
        });
    }

    /**
     * Busca las reservas asociadas a una mesa y fecha sin filtrar por estado.
     *
     * @param mesaId identificador de la mesa
     * @param fecha fecha de las reservas
     * @return reservas de la mesa en esa fecha
     */
    public List<Reserva> findByMesaIdAndFecha(Long mesaId, LocalDate fecha) {
        return almacenamiento.leer(datos -> datos.getReservas().stream()
                .filter(reserva -> Objects.equals(reserva.getMesaId(), mesaId)
                        && Objects.equals(reserva.getFecha(), fecha))
                .toList());
    }

    /**
     * Comprueba si existe alguna reserva para el cliente, incluso si está cancelada.
     *
     * @param clienteId identificador del cliente
     * @return {@code true} si alguna reserva conserva esa referencia
     */
    public boolean existsByClienteId(Long clienteId) {
        return almacenamiento.leer(datos -> datos.getReservas().stream()
                .anyMatch(reserva -> Objects.equals(reserva.getClienteId(), clienteId)));
    }

    /**
     * Comprueba si existe alguna reserva para la mesa, incluso si está cancelada.
     *
     * @param mesaId identificador de la mesa
     * @return {@code true} si alguna reserva conserva esa referencia
     */
    public boolean existsByMesaId(Long mesaId) {
        return almacenamiento.leer(datos -> datos.getReservas().stream()
                .anyMatch(reserva -> Objects.equals(reserva.getMesaId(), mesaId)));
    }
}
