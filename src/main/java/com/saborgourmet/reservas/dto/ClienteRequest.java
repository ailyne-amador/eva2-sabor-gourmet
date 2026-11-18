package com.saborgourmet.reservas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos recibidos para crear o actualizar un cliente; define la validación de
 * entrada del Controller antes de que la solicitud llegue al Service.
 *
 * @param nombre nombre completo obligatorio
 * @param identificacion documento obligatorio y único según las reglas del Service
 * @param email correo obligatorio con formato válido
 * @param telefono teléfono de contacto obligatorio
 */
public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "La identificación es obligatoria") String identificacion,
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe tener un formato válido") String email,
        @NotBlank(message = "El teléfono es obligatorio") String telefono) {
}
