package com.saborgourmet.reservas.repository;

import com.saborgourmet.reservas.exception.PersistenciaException;
import com.saborgourmet.reservas.model.DatosRestaurante;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Lee y escribe el archivo JSON compartido por los repositories de la arquitectura CSR.
 * Protege cada operación con un lock y reemplaza el archivo mediante una escritura atómica.
 */
@Component
public class JsonFileStore {

    private static final Logger log = LoggerFactory.getLogger(JsonFileStore.class);

    private final Path ruta;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock bloqueo = new ReentrantReadWriteLock();

    /**
     * Construye el almacenamiento con la ruta configurada y el mapper de Spring.
     *
     * @param rutaArchivo ubicación del archivo JSON indicada por {@code app.datos.ruta}
     * @param objectMapper mapper configurado por Spring para serializar el modelo
     */
    public JsonFileStore(@Value("${app.datos.ruta}") String rutaArchivo, ObjectMapper objectMapper) {
        this.ruta = Path.of(rutaArchivo);
        this.objectMapper = objectMapper;
    }

    /**
     * Crea el archivo vacío si no existe o verifica el contenido existente sin sobrescribirlo.
     *
     * @throws PersistenciaException si no se puede crear el archivo o leer su contenido
     */
    @PostConstruct
    public void inicializar() {
        bloqueo.writeLock().lock();
        try {
            Files.createDirectories(ruta.toAbsolutePath().getParent());
            if (Files.exists(ruta)) {
                leerDatos();
            } else {
                guardarDatos(new DatosRestaurante());
            }
        } catch (IOException e) {
            log.error("No se pudo inicializar el archivo de datos", e);
            throw new PersistenciaException("No se pudo inicializar el almacenamiento de datos.", e);
        } finally {
            bloqueo.writeLock().unlock();
        }
    }

    /**
     * Lee el archivo del disco y aplica una consulta mientras mantiene el lock de lectura.
     * Cada llamada vuelve a leer el JSON para reflejar los cambios persistidos.
     *
     * @param consulta función que obtiene un resultado del conjunto de datos
     * @param <T> tipo del resultado de la consulta
     * @return resultado producido por la función
     * @throws PersistenciaException si el archivo no se puede leer o está mal formado
     */
    public <T> T leer(Function<DatosRestaurante, T> consulta) {
        bloqueo.readLock().lock();
        try {
            return consulta.apply(leerDatos());
        } finally {
            bloqueo.readLock().unlock();
        }
    }

    /**
     * Lee, modifica y guarda el conjunto completo bajo el lock de escritura.
     * Si la función falla, no se escribe el archivo.
     *
     * @param cambio función que aplica la modificación y produce el resultado
     * @param <T> tipo del resultado de la modificación
     * @return resultado producido por la función
     * @throws PersistenciaException si el archivo no se puede leer o escribir
     */
    public <T> T modificar(Function<DatosRestaurante, T> cambio) {
        bloqueo.writeLock().lock();
        try {
            DatosRestaurante datos = leerDatos();
            T resultado = cambio.apply(datos);
            guardarDatos(datos);
            return resultado;
        } finally {
            bloqueo.writeLock().unlock();
        }
    }

    private DatosRestaurante leerDatos() {
        try {
            DatosRestaurante datos = objectMapper.readValue(ruta.toFile(), DatosRestaurante.class);
            validar(datos);
            return datos;
        } catch (IOException | RuntimeException e) {
            log.error("No se pudo leer el archivo JSON de persistencia", e);
            throw new PersistenciaException("No se pudieron leer los datos persistidos.", e);
        }
    }

    private void guardarDatos(DatosRestaurante datos) {
        Path temporal = ruta.resolveSibling(ruta.getFileName() + ".tmp");
        try {
            validar(datos);
            objectMapper.writeValue(temporal.toFile(), datos);
            try {
                Files.move(temporal, ruta, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            log.error("No se pudo guardar el archivo JSON de persistencia", e);
            throw new PersistenciaException("No se pudieron guardar los datos persistidos.", e);
        } finally {
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException e) {
                log.warn("No se pudo eliminar el archivo temporal de persistencia", e);
            }
        }
    }

    private static void validar(DatosRestaurante datos) throws IOException {
        if (datos == null || datos.getClientes() == null || datos.getMesas() == null || datos.getReservas() == null) {
            throw new IOException("La estructura de datos persistidos es inválida.");
        }
    }
}
