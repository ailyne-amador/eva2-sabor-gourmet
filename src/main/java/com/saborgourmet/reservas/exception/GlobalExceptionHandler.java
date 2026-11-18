package com.saborgourmet.reservas.exception;

import com.saborgourmet.reservas.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Convierte errores de las capas Controller y Service en respuestas HTTP estables
 * con {@link ErrorResponse}, sin exponer detalles internos de la aplicación.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Responde 404 cuando la capa Service no encuentra el recurso solicitado.
     *
     * @param excepcion error de recurso inexistente
     * @param solicitud solicitud HTTP que produjo el error
     * @return cuerpo de error con el mensaje seguro de la excepción
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException excepcion, HttpServletRequest solicitud) {
        return construirRespuesta(HttpStatus.NOT_FOUND, excepcion.getMessage(), solicitud, List.of());
    }

    /**
     * Responde 400 cuando una regla de negocio invalida la solicitud.
     *
     * @param excepcion error de solicitud inválida
     * @param solicitud solicitud HTTP que produjo el error
     * @return cuerpo de error con el mensaje de validación
     */
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarSolicitudInvalida(
            SolicitudInvalidaException excepcion, HttpServletRequest solicitud) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, excepcion.getMessage(), solicitud, List.of());
    }

    /**
     * Responde 409 cuando el estado actual de un recurso impide la operación.
     *
     * @param excepcion error de conflicto de negocio
     * @param solicitud solicitud HTTP que produjo el error
     * @return cuerpo de error con el mensaje del conflicto
     */
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> manejarConflicto(
            ConflictoException excepcion, HttpServletRequest solicitud) {
        return construirRespuesta(HttpStatus.CONFLICT, excepcion.getMessage(), solicitud, List.of());
    }

    /**
     * Responde 400 e identifica los campos que incumplen sus restricciones Bean Validation.
     *
     * @param excepcion errores de validación del cuerpo recibido
     * @param solicitud solicitud HTTP que produjo el error
     * @return cuerpo de error con un detalle por cada campo inválido
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(
            MethodArgumentNotValidException excepcion, HttpServletRequest solicitud) {
        List<String> detalles = excepcion.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        return construirRespuesta(
                HttpStatus.BAD_REQUEST, "La solicitud contiene campos inválidos", solicitud, detalles);
    }

    /**
     * Responde 400 si el cuerpo JSON no se puede deserializar al DTO solicitado.
     *
     * @param excepcion error al leer o convertir el cuerpo HTTP
     * @param solicitud solicitud HTTP que produjo el error
     * @return cuerpo de error genérico sin detalles del parser
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(
            HttpMessageNotReadableException excepcion, HttpServletRequest solicitud) {
        return construirRespuesta(
                HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido", solicitud, List.of());
    }

    /**
     * Responde 400 si un parámetro de ruta o consulta no coincide con su tipo esperado.
     *
     * @param excepcion parámetro cuyo valor no pudo convertirse
     * @param solicitud solicitud HTTP que produjo el error
     * @return cuerpo de error que identifica el parámetro, nunca su valor recibido
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoDeParametroInvalido(
            MethodArgumentTypeMismatchException excepcion, HttpServletRequest solicitud) {
        String mensaje = "El parámetro '%s' tiene un formato inválido".formatted(excepcion.getName());
        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje, solicitud, List.of());
    }

    /**
     * Responde 400 cuando falta un parámetro de consulta obligatorio.
     *
     * @param excepcion parámetro obligatorio ausente
     * @param solicitud solicitud HTTP que produjo el error
     * @return cuerpo de error que identifica el parámetro faltante
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> manejarParametroFaltante(
            MissingServletRequestParameterException excepcion, HttpServletRequest solicitud) {
        String mensaje = "Falta el parámetro obligatorio '%s'".formatted(excepcion.getParameterName());
        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje, solicitud, List.of());
    }

    /**
     * Responde 500 ante fallos de lectura o escritura del archivo JSON y registra la causa técnica.
     *
     * @param excepcion error de persistencia producido por Repository
     * @param solicitud solicitud HTTP que produjo el error
     * @return cuerpo de error genérico sin exponer la causa
     */
    @ExceptionHandler(PersistenciaException.class)
    public ResponseEntity<ErrorResponse> manejarPersistencia(
            PersistenciaException excepcion, HttpServletRequest solicitud) {
        log.error("Error al acceder a la persistencia JSON", excepcion);
        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", solicitud, List.of());
    }

    /**
     * Responde 500 ante cualquier excepción no controlada y conserva el detalle solo en el log.
     *
     * @param excepcion error inesperado de la aplicación
     * @param solicitud solicitud HTTP que produjo el error
     * @return cuerpo de error genérico sin exponer información interna
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorNoControlado(
            Exception excepcion, HttpServletRequest solicitud) {
        log.error("Error no controlado al procesar la solicitud", excepcion);
        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", solicitud, List.of());
    }

    private ResponseEntity<ErrorResponse> construirRespuesta(
            HttpStatus estado, String mensaje, HttpServletRequest solicitud, List<String> detalles) {
        ErrorResponse respuesta = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(estado.value())
                .error(estado.getReasonPhrase())
                .mensaje(mensaje)
                .ruta(solicitud.getRequestURI())
                .detalles(detalles)
                .build();
        return ResponseEntity.status(estado).body(respuesta);
    }
}
