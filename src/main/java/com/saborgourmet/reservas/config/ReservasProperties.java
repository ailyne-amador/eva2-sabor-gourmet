package com.saborgourmet.reservas.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;

/**
 * Configuración tipada de duración y horario de reservas, validada al iniciar.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.reservas")
public class ReservasProperties {

    /** Duración fija, en minutos, de cada reserva. */
    @NotNull
    @Positive
    private Integer duracionMinutos;

    /** Hora local desde la que se aceptan reservas. */
    @NotNull
    private LocalTime horaApertura;

    /** Hora local hasta la que puede extenderse una reserva. */
    @NotNull
    private LocalTime horaCierre;
}
