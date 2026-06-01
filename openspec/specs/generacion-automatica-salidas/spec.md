# Generación Automática de Salidas — Specification

## Purpose

Automatizar la creación diaria de salidas a partir de horarios activos, eliminando la intervención manual del administrador para generar viajes individuales de cada ruta.

## Requirements

### Requirement: Scheduler diario genera salidas del día

The system MUST execute a scheduled task at 05:00 (America/Caracas) cada día que consulte los horarios activos (`activo = true`) cuyo `diaSemana` coincida con el día actual de la semana.

Para cada horario compatible, MUST generar salidas desde `horaInicio` hasta `horaFin` con paso `intervaloMinutos`. Cada salida MUST tener:
- `estado = PROGRAMADA`
- `autobus = null`
- `horario` seteado (relación no nula hacia el horario que la originó)
- `horaProgramada` calculada como la fecha actual con la hora correspondiente del intervalo
- `retrasoMinutos = 0`

The system MUST verificar que `horario.terminalOrigen` coincida con `horario.ruta.origen` antes de generar salidas para ese horario. Si no coincide, MUST registrar un error y saltar ese horario.

#### Scenario: Generación exitosa para un horario LUNES

- GIVEN un horario activo con `diaSemana = LUNES`, `horaInicio = 06:00`, `horaFin = 08:00`, `intervaloMinutos = 60`, y `terminalOrigen` coincide con `ruta.origen`
- WHEN el scheduler ejecuta un día LUNES a las 05:00
- THEN MUST crear dos salidas: una a las 06:00 y otra a las 07:00 del día actual
- AND ambas salidas MUST tener `estado = PROGRAMADA` y `autobus = null`

#### Scenario: Idempotencia — no duplica salidas existentes

- GIVEN que ya existe una salida para el horario ID 5 con `horaProgramada = 2026-05-30T06:00`
- WHEN el scheduler ejecuta y procesa ese mismo horario para el mismo día
- THEN MUST no crear una segunda salida duplicada para ese horario+fecha
- AND MUST continuar generando las salidas faltantes del intervalo

#### Scenario: Horario con terminalOrigen inválido se salta

- GIVEN un horario activo cuyo `terminalOrigen.id ≠ ruta.origen.id`
- WHEN el scheduler procesa ese horario
- THEN MUST no generar salidas para ese horario
- AND MUST registrar un error en el log

#### Scenario: Trigger manual del scheduler

- The system MAY exponer un endpoint `POST /api/v1/salidas/generar-del-dia` que ejecute la misma lógica del scheduler bajo demanda
- WHEN un admin autenticado invoca el endpoint manualmente
- THEN MUST seguir las mismas reglas de idempotencia y generación

### Requirement: Persistencia del horario en salidas generadas

The system MUST establecer `horario` como no nulo en todas las salidas generadas automáticamente. La relación `Salida → Horario` SHOULD mantener la anotación `nullable = true` a nivel JPA para retrocompatibilidad con salidas existentes sin horario.

#### Scenario: Salida generada tiene horario seteado

- GIVEN una salida generada por el scheduler
- THEN `salida.horario` MUST no ser nulo
- AND `salida.horario.id` MUST coincidir con el horario que originó la generación
