# Decisiones de diseño

- Se utilizarán Java 21 y Spring Boot 4.1.2, según lo indicado para este proyecto.
- MesaService usa el Clock inyectado para detectar reservas activas futuras al reducir la capacidad de una mesa.
- El cliente se identifica por su ID autoincremental; no se almacena un RUT o documento adicional.
- Las operaciones de reserva usan `synchronized` por instancia; varias réplicas requieren coordinación compartida.
