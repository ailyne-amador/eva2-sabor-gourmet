package com.saborgourmet.reservas.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saborgourmet.reservas.exception.PersistenciaException;
import com.saborgourmet.reservas.model.Cliente;
import com.saborgourmet.reservas.model.DatosRestaurante;
import com.saborgourmet.reservas.model.Mesa;
import com.saborgourmet.reservas.model.Reserva;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** Comprueba la persistencia JSON y las operaciones principales de los repositories. */
class JsonFileStoreTest {

    @TempDir
    Path directorioTemporal;

    /** Verifica que una ruta ausente se inicialice con las tres colecciones vacías. */
    @Test
    void creaArchivoConEstructuraVaciaCuandoNoExiste() throws IOException {
        Path ruta = directorioTemporal.resolve("subdirectorio/datos.json");
        JsonFileStore almacenamiento = nuevoAlmacenamiento(ruta);

        almacenamiento.inicializar();

        DatosRestaurante datos = JsonMapper.builder().build().readValue(ruta.toFile(), DatosRestaurante.class);
        assertEquals(new DatosRestaurante(), datos);
    }

    /** Comprueba que otra instancia recupere lo escrito, como al reiniciar la aplicación. */
    @Test
    void conservaDatosAlCrearUnaNuevaInstancia() {
        Path ruta = directorioTemporal.resolve("datos.json");
        JsonFileStore almacenamiento = nuevoAlmacenamiento(ruta);
        almacenamiento.inicializar();
        Cliente cliente = Cliente.builder()
                .nombre("Camila Rojas")
                .identificacion("12.345.678-5")
                .email("camila@example.com")
                .telefono("+56912345678")
                .build();

        almacenamiento.modificar(datos -> {
            datos.getClientes().add(cliente);
            return null;
        });
        JsonFileStore reiniciado = nuevoAlmacenamiento(ruta);
        reiniciado.inicializar();

        assertEquals("Camila Rojas", reiniciado.leer(datos -> datos.getClientes().getFirst().getNombre()));
    }

    /** Verifica el autoincremento independiente y el reemplazo por ID en cada repository. */
    @Test
    void asignaIdsPorColeccionYReemplazaRegistrosExistentes() {
        JsonFileStore almacenamiento = nuevoAlmacenamiento(directorioTemporal.resolve("datos.json"));
        almacenamiento.inicializar();
        ClienteRepository clientes = new ClienteRepository(almacenamiento);
        MesaRepository mesas = new MesaRepository(almacenamiento);
        ReservaRepository reservas = new ReservaRepository(almacenamiento);

        Cliente primerCliente = clientes.save(Cliente.builder().nombre("Ana").build());
        Cliente segundoCliente = clientes.save(Cliente.builder().nombre("Luis").build());
        Mesa primeraMesa = mesas.save(Mesa.builder().numero(1).capacidad(2).build());
        Mesa segundaMesa = mesas.save(Mesa.builder().numero(2).capacidad(4).build());
        Reserva primeraReserva = reservas.save(Reserva.builder().clienteId(primerCliente.getId()).mesaId(primeraMesa.getId()).build());
        Reserva segundaReserva = reservas.save(Reserva.builder().clienteId(segundoCliente.getId()).mesaId(segundaMesa.getId()).build());
        primerCliente.setNombre("Ana actualizada");
        clientes.save(primerCliente);

        assertEquals(1L, primerCliente.getId());
        assertEquals(2L, segundoCliente.getId());
        assertEquals(1L, primeraMesa.getId());
        assertEquals(2L, segundaMesa.getId());
        assertEquals(1L, primeraReserva.getId());
        assertEquals(2L, segundaReserva.getId());
        assertEquals(2, clientes.findAll().size());
        assertEquals("Ana actualizada", clientes.findById(primerCliente.getId()).orElseThrow().getNombre());
    }

    /** Verifica que un JSON corrupto falle sin modificar los bytes originales. */
    @Test
    void jsonCorruptoFallaSinSobrescribirlo() throws IOException {
        Path ruta = directorioTemporal.resolve("datos.json");
        Files.writeString(ruta, "{contenido inválido");
        JsonFileStore almacenamiento = nuevoAlmacenamiento(ruta);

        assertThrows(PersistenciaException.class, almacenamiento::inicializar);

        assertEquals("{contenido inválido", Files.readString(ruta));
    }

    /** Verifica que la escritura correcta no deje archivos temporales huérfanos. */
    @Test
    void eliminaArchivoTemporalDespuesDeGuardar() {
        Path ruta = directorioTemporal.resolve("datos.json");
        JsonFileStore almacenamiento = nuevoAlmacenamiento(ruta);
        almacenamiento.inicializar();

        almacenamiento.modificar(datos -> {
            datos.getMesas().add(Mesa.builder().numero(1).capacidad(2).build());
            return null;
        });

        assertTrue(Files.exists(ruta));
        assertFalse(Files.exists(ruta.resolveSibling("datos.json.tmp")));
    }

    private JsonFileStore nuevoAlmacenamiento(Path ruta) {
        return new JsonFileStore(ruta.toString(), JsonMapper.builder().build());
    }
}
