package com.saborgourmet.reservas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Datos recibidos para crear o actualizar una mesa; el Controller valida los
 * campos obligatorios antes de delegar las reglas de negocio al Service.
 *
 * @param numero número positivo y obligatorio de la mesa
 * @param capacidad capacidad positiva y obligatoria de la mesa
 * @param ubicacion descripción opcional de la ubicación
 */
public record MesaRequest(
        @NotNull(message = "El número de mesa es obligatorio")
        @Positive(message = "El número de mesa debe ser positivo") Integer numero,
        @NotNull(message = "La capacidad es obligatoria")
        @Positive(message = "La capacidad debe ser positiva") Integer capacidad,
        String ubicacion) {
}
