package com.saborgourmet.reservas.exception;

/**
 * Señala que una solicitud incumple una regla de negocio y se traduce en una
 * respuesta HTTP 400 por el manejador global.
 */
public class SolicitudInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea el error de solicitud inválida con un mensaje apto para el cliente.
     *
     * @param mensaje descripción de la regla incumplida
     */
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
