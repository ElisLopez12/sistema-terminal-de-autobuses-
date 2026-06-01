# Asignación de Autobús a Salida — Specification

## Purpose

Permitir que un administrador asigne o reasigne un autobús a una salida generada automáticamente, asegurando que el autobús esté disponible y cumpla las reglas de pertenencia al terminal.

## Requirements

### Requirement: Endpoint de asignación

The system MUST exponer `PUT /api/v1/salidas/{id}/asignar-autobus` que reciba un body con `{ "autobusId": Long }`.

#### Scenario: Asignación exitosa

- GIVEN una salida PROGRAMADA sin autobús asignado, y un autobús activo con chofer asignado, perteneciente al mismo terminal que la salida
- WHEN un admin invoca `PUT /api/v1/salidas/{id}/asignar-autobus` con el `autobusId` válido
- THEN MUST asignar el autobús a la salida
- AND MUST responder 200 OK con la salida actualizada incluyendo `autobusId` y `autobusNumeroUnidad`

### Requirement: Validaciones de integridad

The system MUST validar antes de asignar:

1. La salida MUST existir y estar activa
2. El autobús MUST existir y estar activo (`activo = true`)
3. El autobús MUST tener un chofer asignado (`autobus.chofer != null`)
4. El autobús MUST pertenecer al mismo terminal que `salida.terminalOrigen`
5. El autobús MUST no tener otra salida PROGRAMADA o EN_CURSO en el mismo horario (misma hora o solapada dentro del intervalo de la salida)

#### Scenario: Autobús de otro terminal es rechazado

- GIVEN una salida del terminal "Maracay" y un autobús del terminal "Valencia"
- WHEN se intenta asignar ese autobús a la salida
- THEN MUST responder 400 Bad Request con mensaje de error

#### Scenario: Autobús sin chofer es rechazado

- GIVEN un autobús activo sin chofer asignado
- WHEN se intenta asignar a una salida
- THEN MUST responder 400 Bad Request indicando que el autobús no tiene chofer

#### Scenario: Autobús ocupado en otra salida simultánea

- GIVEN un autobús que ya tiene una salida PROGRAMADA a las 10:00 del mismo día
- WHEN se intenta asignar a otra salida también a las 10:00
- THEN MUST responder 409 Conflict indicando conflicto de horario

### Requirement: Autorización por terminal

The system MUST aplicar las reglas de autorización existentes: `CENTRAL_ADMIN` puede asignar cualquier autobús; `TERMINAL_ADMIN` SOLO puede asignar autobuses de su propio terminal y SOLO a salidas de su terminal.

#### Scenario: TERMINAL_ADMIN asigna autobús de su terminal

- GIVEN un usuario con rol `TERMINAL_ADMIN` del terminal "Maracay", una salida de "Maracay", y un autobús de "Maracay"
- WHEN el admin asigna el autobús
- THEN MUST responder 200 OK

#### Scenario: TERMINAL_ADMIN intenta asignar autobús de otro terminal

- GIVEN un usuario con rol `TERMINAL_ADMIN` del terminal "Maracay"
- WHEN intenta asignar un autobús del terminal "Valencia"
- THEN MUST responder 403 Forbidden
