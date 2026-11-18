# Sabor Gourmet — API REST de Reservas

API REST para gestionar clientes, mesas y reservas del restaurante Sabor Gourmet. La arquitectura separa controllers, services y repositories; la persistencia del proyecto será un archivo JSON.

## Requisitos

- JDK 21
- Maven

## Ejecutar

```bash
mvn spring-boot:run
```

La aplicación escucha en `http://localhost:8080`.

## Pruebas

```bash
mvn clean test
```
