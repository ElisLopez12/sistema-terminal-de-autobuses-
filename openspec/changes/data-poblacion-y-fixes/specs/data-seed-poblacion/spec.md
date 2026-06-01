# Data Seed Población Specification

## Purpose

Proveer datos de desarrollo poblados vía SQL script: 500 autobuses, 500 choferes, 500 colectores y horarios por patrón.

## Requirements

### Requirement: SQL script con población base

The system MUST incluir `src/main/resources/data-dev.sql` que inserte datos de prueba cuando el perfil `dev` esté activo.

#### Scenario: Inserción de población completa

- GIVEN terminales y rutas activas existentes
- WHEN se ejecuta `data-dev.sql`
- THEN MUST insertar 500 choferes, 500 colectores, 500 autobuses (5/ruta) con chofer y colector asignados

### Requirement: Horarios por patrón semanal

The system MUST generar horarios con patrón: LUN-VIE 05:00-20:00 /60 min, SÁB 06:00-18:00 /60 min, DOM 08:00-14:00 /60 min.

#### Scenario: Horarios generados

- GIVEN una ruta activa
- WHEN se ejecuta el script
- THEN MUST existir horarios con esos patrones para cada día aplicable

### Requirement: Aislamiento por perfil

The system MUST ejecutar `data-dev.sql` SOLO cuando `spring.profiles.active=dev`.

#### Scenario: Perfil dev carga, perfil no-dev no

- GIVEN `spring.profiles.active=dev`
- WHEN arranca Spring Boot
- THEN MUST ejecutar `data-dev.sql`
- AND GIVEN perfil `prod` o `test`
- THEN MUST omitir `data-dev.sql`
