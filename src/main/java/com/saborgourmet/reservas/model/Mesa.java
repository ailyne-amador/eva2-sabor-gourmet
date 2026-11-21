package com.saborgourmet.reservas.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de dominio de una mesa, compartida entre services y repositories
 * y serializada como parte del archivo JSON en la arquitectura CSR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mesa {

    /** Identificador único de la mesa dentro de la colección persistida. */
    private Long id;

    /** Número visible de la mesa, único entre las mesas registradas. */
    private int numero;

    /** Cantidad máxima de personas que puede recibir la mesa. */
    private int capacidad;

    /** Ubicación descriptiva de la mesa; puede ser nula. */
    private String ubicacion;
}
