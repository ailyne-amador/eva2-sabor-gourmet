package com.saborgourmet.reservas.model;

/**
 * Estados persistidos de una reserva, compartidos por los servicios y repositories
 * para controlar su vigencia dentro de la arquitectura CSR.
 */
public enum EstadoReserva {

    /** Reserva vigente que bloquea disponibilidad en su horario. */
    ACTIVA,

    /** Reserva cancelada que conserva el historial y libera la mesa. */
    CANCELADA
}
