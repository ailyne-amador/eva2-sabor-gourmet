package com.saborgourmet.reservas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Provee el reloj del sistema para que los servicios puedan validar fechas actuales. */
@Configuration
public class ClockConfig {

    /**
     * Crea un reloj del sistema con la zona horaria local de la aplicación.
     *
     * @return reloj utilizado para obtener la fecha y hora actuales
     */
    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
