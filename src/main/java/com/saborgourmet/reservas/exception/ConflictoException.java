package com.saborgourmet.reservas.exception;

/**
 * Señala un conflicto con el estado actual de un recurso y se traduce en una
 * respuesta HTTP 409 por el manejador global.
 */
public class ConflictoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea el error de conflicto con un mensaje apto para el cliente.
     *
     * @param mensaje descripción del conflicto detectado
     */
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
