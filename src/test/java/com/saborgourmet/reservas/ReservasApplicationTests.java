package com.saborgourmet.reservas;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Comprueba que Spring pueda crear el contexto inicial de la aplicación. */
@SpringBootTest
class ReservasApplicationTests {

    @TempDir
    static Path directorioTemporal;

    @DynamicPropertySource
    static void registrarRutaTemporal(DynamicPropertyRegistry registro) {
        registro.add("app.datos.ruta", () -> directorioTemporal.resolve("datos.json").toString());
    }

    /** Verifica que el contexto de aplicación se cargue sin errores. */
    @Test
    void contextLoads() {
    }
}
