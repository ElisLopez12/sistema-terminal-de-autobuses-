# Public Routes Filter Specification

## Purpose

Permitir filtrar rutas públicas por terminal de origen (`origenId`) en `GET /api/v1/public/rutas`.

## Requirements

### Requirement: Filtro opcional por terminal origen

The system MUST aceptar un parámetro `origenId` opcional en `GET /api/v1/public/rutas` y retornar solo las rutas cuyo `terminalOrigen.id` coincida.

#### Scenario: Sin filtro retorna todas

- GIVEN rutas de múltiples terminales
- WHEN `GET /api/v1/public/rutas` sin `origenId`
- THEN MUST retornar todas las rutas activas

#### Scenario: Con origenId filtra por terminal

- GIVEN rutas de "Maracay" y "Valencia"
- WHEN `GET /api/v1/public/rutas?origenId={idMaracay}`
- THEN MUST retornar solo rutas con `terminalOrigen.id = idMaracay`

#### Scenario: origenId inválido retorna lista vacía

- GIVEN `origenId=99999` inexistente
- WHEN `GET /api/v1/public/rutas?origenId=99999`
- THEN MUST retornar 200 OK con lista vacía

### Requirement: Retrocompatibilidad

The system MUST conservar el formato de respuesta actual (lista JSON plana) al agregar el filtro.

#### Scenario: Formato sin cambios

- GIVEN `GET /api/v1/public/rutas` con o sin `origenId`
- WHEN se procesa la solicitud
- THEN MUST retornar el mismo formato JSON que antes
