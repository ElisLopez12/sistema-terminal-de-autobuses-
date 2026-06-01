# Ajuste de Retraso con Propagación — Specification

## Purpose

Permitir que un administrador ajuste el retraso de una salida y propague el mismo retraso a las salidas siguientes de la misma ruta el mismo día, reflejando el efecto dominó de una demora.

## Requirements

### Requirement: Endpoint de ajuste con propagación

The system MUST exponer `PUT /api/v1/salidas/{id}/ajustar-retraso` que reciba `{ "retrasoMinutos": N }` con N >= 0.

El endpoint MUST:
1. Actualizar `salida.retrasoMinutos` con el valor recibido
2. Buscar salidas siguientes de la misma ruta y mismo día: aquellas con `horaProgramada > salida.horaProgramada`, misma `ruta.id`, y cuya fecha sea la misma
3. A cada salida destino que esté en estado `PROGRAMADA`, MUST sumarle el mismo valor `retrasoMinutos` a su `horaProgramada`
4. No propaga a salidas destino que estén en estado `EN_CURSO`, `COMPLETADA` o `CANCELADA`

#### Scenario: Propagación a tres salidas siguientes

- GIVEN salidas de la ruta R para hoy: S1 (08:00), S2 (09:00, PROGRAMADA), S3 (10:00, PROGRAMADA), S4 (11:00, PROGRAMADA)
- WHEN se ajusta retraso de S1 con `retrasoMinutos = 30`
- THEN S1.retrasoMinutos = 30
- AND S2.horaProgramada → 09:30, S3.horaProgramada → 10:30, S4.horaProgramada → 11:30

#### Scenario: No propaga a salidas EN_CURSO o COMPLETADAS

- GIVEN salidas de la ruta R: S1 (08:00), S2 (09:00, EN_CURSO), S3 (10:00, PROGRAMADA)
- WHEN se ajusta retraso de S1 con `retrasoMinutos = 15`
- THEN S2.horaProgramada MUST permanecer en 09:00 (no se modifica)
- AND S3.horaProgramada MUST actualizarse a 10:15

#### Scenario: Retraso en salida intermedia solo propaga hacia adelante

- GIVEN salidas de la ruta R: S1 (08:00, PROGRAMADA), S2 (09:00), S3 (10:00, PROGRAMADA)
- WHEN se ajusta retraso de S2 con `retrasoMinutos = 20`
- THEN S1.horaProgramada MUST permanecer en 08:00
- AND S3.horaProgramada MUST actualizarse a 10:20

#### Scenario: Retraso en cero no modifica nada

- GIVEN salidas de la ruta R: S1 (08:00, PROGRAMADA), S2 (09:00, PROGRAMADA)
- WHEN se ajusta retraso de S1 con `retrasoMinutos = 0`
- THEN S1.retrasoMinutos = 0
- AND S2.horaProgramada MUST permanecer en 09:00

### Requirement: Autorización por terminal

The system MUST aplicar las reglas de autorización existentes. `TERMINAL_ADMIN` SOLO puede ajustar retraso de salidas que pertenezcan a su terminal.

#### Scenario: TERMINAL_ADMIN ajusta salida de su terminal

- GIVEN un admin del terminal "Maracay" y una salida de "Maracay"
- WHEN ajusta el retraso
- THEN MUST responder 200 OK con la salida actualizada

#### Scenario: TERMINAL_ADMIN rechazado para salida de otro terminal

- GIVEN un admin del terminal "Maracay" y una salida del terminal "Valencia"
- WHEN intenta ajustar el retraso
- THEN MUST responder 403 Forbidden

#### Scenario: Salida no encontrada

- GIVEN un ID de salida que no existe
- WHEN se invoca el endpoint
- THEN MUST responder 404 Not Found
