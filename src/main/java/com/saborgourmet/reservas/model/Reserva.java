package com.saborgourmet.reservas.model;

import java.time.LocalDate;
import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de dominio de una reserva. Sus referencias a cliente y mesa son IDs;
 * services aplican sus reglas y repositories la persisten en JSON dentro de CSR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    /** Identificador único de la reserva dentro de la colección persistida. */
    private Long id;

    /** Identificador del cliente asociado a la reserva. */
    private Long clienteId;

    /** Identificador de la mesa asignada a la reserva. */
    private Long mesaId;

    /** Fecha local en que se realizará la reserva. */
    private LocalDate fecha;

    /** Hora local de inicio de la reserva. */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime hora;

    /** Duración fijada para esta reserva, expresada en minutos. */
    private Integer duracionMinutos;

    /** Cantidad de personas para quienes se reserva la mesa. */
    private Integer cantidadPersonas;

    /** Estado que determina si la reserva bloquea disponibilidad. */
    private EstadoReserva estado;
}
