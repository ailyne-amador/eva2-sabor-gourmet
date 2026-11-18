package com.saborgourmet.reservas.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Datos recibidos para crear o modificar una reserva; separa el contrato HTTP
 * del modelo persistido y valida campos básicos antes del Service.
 *
 * @param clienteId identificador positivo y obligatorio del cliente
 * @param mesaId identificador positivo y obligatorio de la mesa
 * @param fecha fecha obligatoria de la reserva
 * @param hora hora obligatoria de inicio de la reserva
 * @param cantidadPersonas cantidad positiva y obligatoria de personas
 */
public record ReservaRequest(
        @NotNull(message = "El identificador del cliente es obligatorio")
        @Positive(message = "El identificador del cliente debe ser positivo") Long clienteId,
        @NotNull(message = "El identificador de la mesa es obligatorio")
        @Positive(message = "El identificador de la mesa debe ser positivo") Long mesaId,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotNull(message = "La hora es obligatoria") LocalTime hora,
        @NotNull(message = "La cantidad de personas es obligatoria")
        @Positive(message = "La cantidad de personas debe ser positiva") Integer cantidadPersonas) {
}
