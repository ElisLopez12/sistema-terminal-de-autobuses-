# Design: Data Population & Bug Fixes

## Technical Approach

Two validation fixes plus a SQL seed script targeting dev profile only. Each fix follows existing code patterns. Seed data uses a single transactional SQL file with profile-specific activation.

## Architecture Decisions

### Decision 1: Chofer validation strategy

**Choice**: Inline guard in `create()` and `update()`, no shared method
**Alternatives considered**: Extract `validateAutobusForSalida(Autobus)` private helper
**Rationale**: The check is a single `if` statement — 3 lines. Extracting to a method adds indirection without reuse benefit (the check differs slightly by endpoint context). The exact same inline pattern already exists at line 363 in `asignarAutobus()` — consistency with existing code wins.

```
// Added after isActivo() check in both create() and update():
if (autobus.getChofer() == null) {
    throw new IllegalArgumentException("El autobús no tiene un chofer asignado");
}
```

### Decision 2: Public routes filter shape

**Choice**: Optional `@RequestParam(required=false) Long origenId` on existing method
**Alternatives considered**: Method overload (second `getRutas()` method), separate endpoint
**Rationale**: Spring handles optional params cleanly — no method duplication. If param is null, call existing `findByActivoTrue()`; if present, call new `findByActivoTrueAndOrigenId()`. The frontend already groups routes by `origenId` on the client — filtering at the API level makes the grouping more efficient and fixes the "0 routes" display issue.

### Decision 3: SQL script isolation

**Choice**: Profile-specific `data-dev.sql` + `application-dev.properties`
**Alternatives considered**: Single `data.sql` with conditional logic, Spring `@Sql` annotations
**Rationale**: Spring Boot loads `data.sql` (or `data-{profile}.sql`) + `application-{profile}.properties` automatically when the matching profile is active. Naming it `data-dev.sql` guarantees zero risk of running in production. The `application-dev.properties` file sets `spring.jpa.defer-datasource-initialization=true` (Hibernate DDL before SQL) and `spring.sql.init.mode=always` (MySQL needs explicit mode — `embedded` default only works for H2).

### Decision 4: Horario generation scope

**Choice**: Full spec pattern — LUN-VIE 16 slots, SÁB 13 slots, DOM 7 slots per route
**Rationale**: 100 routes × 100 horarios each ≈ 10,000 rows. This is dev data designed for realistic testing — the scheduler generates real salidas from these horarios. Fewer horarios means fewer salidas and a non-representative test surface. The proposal's "400+" was a rough estimate; the spec pattern is authoritative.

## Data Flow

```
Bug 1 (chofer guard):
  POST/PUT /salidas { autobusId } 
    → AutobusRepository.findById() 
    → isActivo() check (existing) 
    → getChofer() == null check (NEW) 
    → throw 400 if null

Bug 2 (public filter):
  GET /public/rutas?origenId=13
    → PublicController.getRutas(origenId)
    → RutaRepository.findByActivoTrueAndOrigenId(13, pageable)
    → RutaConParadasResponse.fromEntity() per ruta
    → Response already includes origenId

Seed script:
  🡒 DELETE horarios (fresh start)
  🡒 INSERT 500 choferes (50/terminal)
  🡒 INSERT 500 colectores (50/terminal)
  🡒 INSERT 500 autobuses (5/ruta, 50/terminal)
  🡒 UPDATE autobuses SET chofer_id, colector_id (sequential assignment)
  🡒 INSERT horarios per route (LUN-VIE, SÁB, DOM pattern)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `SalidaService.java` | Modify | Add chofer null-check in `create()` (after line 183) and `update()` (after line 244) |
| `PublicController.java` | Modify | Add `@RequestParam(required=false) Long origenId` to `getRutas()`; conditional repo call |
| `RutaRepository.java` | Modify | Add `Page<Ruta> findByActivoTrueAndOrigenId(Long origenId, Pageable pageable)` |
| `publicApi.js` | Modify | Add `{ origenId }` param to `getRutas()` function signature |
| `MainPage.jsx` | Modify | Pass `origenId` from selected terminal to `getRutas()` (currently fetches all then filters client-side) |
| `data-dev.sql` | Create | Seed script: 500 choferes, 500 colectores, 500 autobuses, ~10,000 horarios |
| `application-dev.properties` | Create | Profile config: `defer-datasource-initialization=true`, `sql.init.mode=always` |

## Interfaces / Contracts

```
GET /api/v1/public/rutas?origenId={terminalId}
  → 200 OK [RutaConParadasResponse]  (same shape, optional filter)

RutaRepository:
  Page<Ruta> findByActivoTrueAndOrigenId(Long origenId, Pageable pageable);
  // Spring Data derives: WHERE activo=true AND origen.id = ?1
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `create()` + `update()` chofer guard | Mock Autobus with null chofer → expect `IllegalArgumentException` |
| Unit | `asignarAutobus()` unchanged | Verify existing test still passes (regression) |
| API | `GET /public/rutas?origenId=` | Start app with `dev` profile, call endpoint, assert filtered results |
| Manual | MainPage display | Load frontend, verify correct route count per terminal card |

## Cloud / Dev Environment

No cloud migration impact. Seed data is local-dev only via profile flag.

## Migration / Rollout

No migration required. The SQL script runs at app startup when `dev` profile is active. Existing production terminals (IDs 13-22) and routes (IDs 106-205) are referenced directly by FK.

## Open Questions

- None. All decisions resolve to existing code patterns.
