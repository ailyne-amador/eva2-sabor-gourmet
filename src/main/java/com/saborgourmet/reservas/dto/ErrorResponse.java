package com.saborgourmet.reservas.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representación estable de errores HTTP para los Controllers, construida por
 * el manejador global y serializada en las respuestas de la API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** Fecha y hora local en que se generó la respuesta de error. */
    private LocalDateTime timestamp;

    /** Código de estado HTTP de la respuesta. */
    private Integer status;

    /** Descripción HTTP breve del estado. */
    private String error;

    /** Mensaje seguro y comprensible para quien consume la API. */
    private String mensaje;

    /** Ruta de la solicitud que produjo el error. */
    private String ruta;

    /** Detalles de validación por campo; vacío cuando no hay detalles. */
    @Builder.Default
    private List<String> detalles = new ArrayList<>();
}
