package com.saborgourmet.reservas.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contenedor raíz que agrupa los datos de JSON leídos y escritos por repositories;
 * los services coordinan su uso en la arquitectura CSR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatosRestaurante {

    /** Clientes persistidos; la lista se inicializa vacía si no se proporciona. */
    @Builder.Default
    private List<Cliente> clientes = new ArrayList<>();

    /** Mesas persistidas; la lista se inicializa vacía si no se proporciona. */
    @Builder.Default
    private List<Mesa> mesas = new ArrayList<>();

    /** Reservas persistidas; la lista se inicializa vacía si no se proporciona. */
    @Builder.Default
    private List<Reserva> reservas = new ArrayList<>();
}
