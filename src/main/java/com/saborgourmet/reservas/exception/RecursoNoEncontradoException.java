package com.saborgourmet.reservas.exception;

/**
 * Señala que un recurso solicitado no existe en la capa de Service y se traduce
 * en una respuesta HTTP 404 por el manejador global.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea el error de recurso inexistente con un mensaje apto para el cliente.
     *
     * @param mensaje descripción del recurso que no se encontró
     */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
