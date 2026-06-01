# Salidas en Tiempo Real — API Pública — Specification

## Purpose

Mejorar el endpoint público de salidas para que devuelva la hora real (programada + retraso) y el estado de asignación de autobús, filtrando solo salidas relevantes para el público.

## Requirements

### Requirement: Hora real en respuesta pública

The system MUST agregar el campo `horaReal` (tipo `LocalDateTime`) a la respuesta de `GET /api/v1/public/salidas`, calculado como `horaProgramada + retrasoMinutos`.

The system MUST agregar el campo `tieneAutobus` (tipo `boolean`) que sea `true` si `autobusId` no es nulo.

#### Scenario: Salida con retraso muestra horaReal diferente

- GIVEN una salida con `horaProgramada = 2026-05-30T08:00` y `retrasoMinutos = 15`
- WHEN se consulta `GET /api/v1/public/salidas`
- THEN `horaReal` MUST ser `2026-05-30T08:15`
- AND `tieneAutobus` MUST ser el valor correspondiente

#### Scenario: Salida sin retraso muestra horaReal igual a programada

- GIVEN una salida con `horaProgramada = 2026-05-30T08:00` y `retrasoMinutos = 0`
- WHEN se consulta `GET /api/v1/public/salidas`
- THEN `horaReal` MUST ser `2026-05-30T08:00`

### Requirement: Filtro de estados relevantes

The system MUST modificar la consulta pública para incluir SOLO salidas cuyo estado sea `PROGRAMADA` o `EN_CURSO`, además de los filtros existentes (`activo = true`, estado distinto de `CANCELADA`).

#### Scenario: Salida COMPLETADA no aparece en respuesta pública

- GIVEN una salida activa con `estado = COMPLETADA`
- WHEN se consulta `GET /api/v1/public/salidas`
- THEN esa salida MUST no aparecer en la respuesta

#### Scenario: Salida PROGRAMADA sí aparece

- GIVEN una salida activa con `estado = PROGRAMADA`
- WHEN se consulta `GET /api/v1/public/salidas`
- THEN esa salida MUST aparecer en la respuesta

### Requirement: Ordenamiento por hora real

The system MUST ordenar las salidas públicas por `horaReal` ascendente, no por `horaProgramada`.

#### Scenario: Orden correcto con retrasos

- GIVEN salidas: S1 (08:00, retraso 0 → horaReal 08:00), S2 (08:30, retraso 30 → horaReal 09:00), S3 (09:00, retraso 0 → horaReal 09:00)
- WHEN se consulta `GET /api/v1/public/salidas`
- THEN el orden MUST ser S1 (08:00), S3 (09:00), S2 (09:00) — S3 antes que S2 por horaReal ascendente
- AND en caso de empate, SHOULD ordenar por `horaProgramada` ascendente como criterio secundario

### Requirement: Modificación del DTO de respuesta

The system MUST extender `SalidaResponse` (o crear un DTO específico público) con los campos `horaReal` y `tieneAutobus`. El campo `retrasoMinutos` SHOULD mantenerse visible para transparencia.

#### Scenario: El DTO público incluye los nuevos campos

- GIVEN la respuesta de `GET /api/v1/public/salidas`
- THEN cada elemento MUST contener `horaReal`, `tieneAutobus`, y `retrasoMinutos`
