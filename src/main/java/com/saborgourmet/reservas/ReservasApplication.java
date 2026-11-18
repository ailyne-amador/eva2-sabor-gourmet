package com.saborgourmet.reservas;

import com.saborgourmet.reservas.config.ReservasProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Inicia la API de reservas y configura el escaneo de componentes de la aplicación.
 * En la arquitectura CSR, los controllers reciben solicitudes HTTP, los services
 * aplican las reglas de negocio y los repositories gestionan la persistencia JSON.
 */
@SpringBootApplication
@EnableConfigurationProperties(ReservasProperties.class)
public class ReservasApplication {

    /**
     * Inicia el contexto de Spring Boot y el servidor HTTP integrado.
     *
     * @param args argumentos de línea de comandos para la aplicación
     */
    public static void main(String[] args) {
        SpringApplication.run(ReservasApplication.class, args);
    }
}
