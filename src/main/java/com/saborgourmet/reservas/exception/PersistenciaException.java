package com.saborgourmet.reservas.exception;

/**
 * Indica que el almacenamiento JSON no pudo leerse o modificarse.
 * La causa técnica se conserva para el registro interno y no debe exponerse al cliente HTTP.
 */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea el error de persistencia con un mensaje seguro y su causa técnica.
     *
     * @param mensaje descripción genérica apta para manejo interno
     * @param causa excepción original registrada por la capa Repository
     */
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
