package com.saborgourmet.reservas.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de dominio de un cliente persistida en JSON y utilizada por los
 * services y repositories de la arquitectura CSR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    /** Identificador único del cliente dentro de la colección persistida. */
    private Long id;

    /** Nombre completo del cliente. */
    private String nombre;

    /** Identificación oficial del cliente, única entre los registros. */
    private String identificacion;

    /** Correo electrónico de contacto del cliente. */
    private String email;

    /** Teléfono de contacto del cliente. */
    private String telefono;
}
