# Tasks: Generación Automática de Salidas

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~700–900 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Delivery strategy | auto-forecast |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

## Phase 1: Foundation

- [x] 1.1 Crear `config/SchedulingConfig.java` — `@Configuration @EnableScheduling` + `@Bean TaskScheduler` con zone `America/Caracas`
- [x] 1.2 Add `findByActivoTrueAndDiaSemana(DiaSemana dia)` a `HorarioRepository.java`
- [x] 1.3 Add `countByHorarioIdAndFecha()` (JPQL) + `findByRutaIdAndHoraProgramadaAfter()` a `SalidaRepository.java`
- [x] 1.4 Add `horaReal` (LocalDateTime) + `tieneAutobus` (boolean) a `SalidaResponse.java`; computar en `fromEntity()`

## Phase 2: Backend Tests — TDD (RED)

- [x] 2.1 `SalidaSchedulerTest` — mock repos; assert genera salidas según intervalo, idempotencia, skip horario inválido
- [x] 2.2 `SalidaServiceTest.asignarAutobus` — 5 escenarios: éxito, otro terminal, sin chofer, bus inactivo, conflicto horario
- [x] 2.3 `SalidaServiceTest.ajustarRetrasoConPropagacion` — 4 escenarios: propaga 3 salidas, salta EN_CURSO, solo adelante, retraso 0
- [x] 2.4 `@DataJpaTest` para queries nuevas — `countByHorarioIdAndFecha` retorna conteo correcto

## Phase 3: Core Implementation — GREEN

- [x] 3.1 Crear `scheduler/SalidaScheduler.java` con `@Scheduled(cron = "0 0 5 * * *")` y método `generarSalidasDelDia()` (usa HorarioService + SalidaRepository)
- [x] 3.2 Add `asignarAutobus(Long id, Long autobusId)` a `SalidaService.java` — validaciones: salida PROGRAMADA, bus activo/con chofer/mismo terminal/sin solapamiento
- [x] 3.3 Add `ajustarRetrasoConPropagacion(Long id, int retrasoMinutos)` a `SalidaService.java` — actualiza retraso y propaga `horaProgramada` a salidas siguientes PROGRAMADAS
- [x] 3.4 Add `findActivosByDia(DiaSemana dia)` a `HorarioService.java` — delega al repositorio

## Phase 4: Controller Wiring + Tests

- [x] 4.1 Add 3 endpoints a `SalidaController.java`: `POST /generar-del-dia`, `PUT /{id}/asignar-autobus`, `PUT /{id}/ajustar-retraso`
- [x] 4.2 Modificar `PublicController.java` — filtrar por `estado IN (PROGRAMADA, EN_CURSO)`; ordenar por `horaProgramada ASC` (como tie-break)
- [x] 4.3 `SalidaControllerIntegrationTest` — prueba autorización y respuestas de los 3 endpoints nuevos (usando @SpringBootTest como patrón del proyecto)
- [x] 4.4 `PublicEndpointTest` — verifica filtro estados y acceso público (tests existentes ampliados)

## Phase 5: Frontend

- [x] 5.1 Add `asignarAutobus(id, autobusId)`, `ajustarRetraso(id, minutos)`, `generarSalidasDelDia()` a `services/adminApi.js`
- [x] 5.2 `pages/admin/SalidasPage.jsx` — columna autobús, filtro "sin autobús" + botones asignar y ajustar retraso
- [x] 5.3 `pages/MainPage.jsx` — mostrar `horaReal` en lugar de `horaProgramada` para próxima salida
- [x] 5.4 `components/TerminalCard.jsx` — mostrar etiqueta de retraso (`horaReal - hraProg`) si `retrasoMinutos > 0`

## Phase 6: Verificación Manual

- [ ] 6.1 Probar scheduler manual vía `POST /generar-del-dia`, asignar autobús, ajustar retraso y verificar página pública refleja `horaReal`
