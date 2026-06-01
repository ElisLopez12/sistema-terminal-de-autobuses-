## Exploration: Data Población y Bug Fixes

### Current State

#### Bug 1: SalidaService.create()/update() missing chofer validation

- `SalidaService.asignarAutobus()` (line 342-407) **does** validate that `autobus.getChofer() != null` before assigning
- `SalidaService.create()` (line 147-191) only validates that autobus exists and is `activo` — **no chofer check**
- `SalidaService.update()` (line 207-264) only validates that autobus exists and is `activo` — **no chofer check**
- This means setting `autobusId` via `SalidaRequest` (the `create`/`update` endpoints) bypasses the chofer validation entirely
- Root cause: a passenger bus without a driver is nonsensical; the API should enforce this at every point of autobus assignment

#### Bug 2: PublicController GET /public/rutas missing origenId filter

- `PublicController.getRutas()` (line 48-55) calls `rutaRepository.findByActivoTrue(pageable)` with no filter parameter
- The frontend `MainPage.jsx` (line 35-38) groups routes by `ruta.origenId` client-side — works but inefficient, fetches ALL routes
- `TerminalCard.jsx` receives `rutas` already filtered from parent, but currently gets ALL routes from the unfiltered API
- `RutaRepository` already has `findByOrigenId(Long terminalId, Pageable pageable)` — but this doesn't filter by `activo`
- The existing `PublicController.getSalidas()` (line 92-121) is a good example: it accepts `origenId` and `rutaId` params with a clean `if/else` pattern
- `RutaConParadasResponse` already includes `origenId` (line 24) — the field exists

#### Data 3: Choferes and Colectores (none exist)

- `Chofer` extends `PersonaBase` with fields: id, terminal (ManyToOne), timestamps; inherited: nombre, apellido, cedula, telefono, fechaNacimiento, direccion, activo
- `Colector` extends `PersonaBase` — same structure
- `Autobus` has `chofer_id` and `colector_id` as nullable OneToOne FK
- `AutobusRequest` supports `choferId` and `colectorId` fields
- `AutobusService.create()` validates chofer/colector terminal match and uniqueness — but only when IDs are provided
- Current DB: all 100 autobuses have null chofer and null colector
- No seed SQL files or DataInitializer exist

#### Data 4: Expand autobuses from 100 to 500 (5 per route)

- 10 terminals, 100 routes (10 per terminal), 100 autobuses (1 per route)
- Target: 500 autobuses (5 per route = 50 per terminal)
- `numeroUnidad` is NOT globally unique — unique per terminal convention is documented
- Autobuses currently have `rutaId` set to their single route
- Need 400 new autobuses, assign to routes, assign choferes and colectores

#### Data 5: Expand horarios from LUNES to LUNES-VIERNES, SABADO, DOMINGO

- Currently: 100 horarios, all with `diaSemana = LUNES`, `horaInicio = 06:00`, `horaFin = 18:00`, `intervaloMinutos = 60`
- `DiaSemana` enum has: LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO, TODOS
- `HorarioRequest` has both `diaSemana` (single) and `diasSemana` (List — for batch creation)
- `HorarioService.create()` iterates over `diasSemana` and creates one Horario per day
- `SalidaScheduler.generarSalidasDelDia()` uses `HorarioService.findActivosByDia(dia)` to find schedules for today
- `findActivosByDia()` already queries `findByActivoTrueAndDiaSemanaIn(List.of(dia, TODOS))` — so the TODOS feature is already wired

### Affected Areas

- `terminalAutobusesBackend/src/main/java/.../service/SalidaService.java` — Bug 1: add chofer validation in create() and update()
- `terminalAutobusesBackend/src/main/java/.../controller/PublicController.java` — Bug 2: add `origenId` param to getRutas()
- `terminalAutobusesBackend/src/main/java/.../repository/RutaRepository.java` — Bug 2: add `findByActivoTrueAndOrigenId()` if needed
- `terminalAutobusesBackend/src/main/java/.../service/AutobusService.java` — Data 3+4: update operations (no changes needed, API is ready)
- `terminalAutobusesBackend/src/main/java/.../service/HorarioService.java` — Data 5: create(new) operations (no changes needed, API is ready)
- `terminalAutobusesFrontend/src/services/publicApi.js` — Bug 2: optionally add `origenId` param to `getRutas()` for future use
- `terminalAutobusesFrontend/src/pages/MainPage.jsx` — Bug 2: optionally pass `origenId` in `getRutas()` call

### Approaches

#### Bug 1: Chofer validation in create/update

1. **Add inline checks in SalidaService.create()/update()** — when `autobusId` is set, load the autobus and verify `autobus.getChofer() != null`
   - Pros: simple, consistent with existing pattern, same error message as `asignarAutobus`
   - Cons: adds a tiny bit of duplication (same check in 3 places now)
   - Effort: Low

2. **Extract a shared `validateAutobusAssignment()` method** — and call it from create, update, and asignarAutobus
   - Pros: DRY, single source of truth
   - Cons: slightly more refactoring
   - Effort: Low

**Recommendation**: Approach 1 — it's a 3-line addition per method, perfectly readable.

#### Bug 2: origenId filter for /public/rutas

1. **Add `origenId` param to getRutas()** — similar to getSalidas() pattern; add query methods to RutaRepository if needed
   - Pros: consistent API design, immediate efficiency gain for frontend
   - Cons: none
   - Effort: Low

**Recommendation**: Approach 1. Add `@RequestParam(required = false) Long origenId`, use existing `findByOrigenId` if origenId is present, otherwise fall back to `findByActivoTrue`. Verify whether `findByOrigenId` needs `activo` filter.

#### Data 3-5: Data population

1. **Single SQL script with all seed data** — one file that creates choferes, colectores, autobuses, and horarios in order
   - Pros: deterministic, reviewable, reusable, repeatable
   - Cons: raw SQL doesn't go through Hibernate lifecycle (@PrePersist still works though), more verbose
   - Effort: Medium (generating 500 person names, 400 buses, 400 new horarios)

2. **Java CommandLineRunner or DataInitializer** — Spring Boot component that seeds data at startup
   - Pros: uses existing services, goes through validation, type-safe
   - Cons: runs every startup (need check for existing data), mixing seed code with app code
   - Effort: High

3. **REST API calls via script** (curl/httpie) — use the existing endpoints to create data
   - Pros: uses real API paths, validates everything, can be tested
   - Cons: slow (500+ sequential requests), requires running backend, fragile
   - Effort: Medium (but time-consuming)

**Recommendation**: Approach 1 (SQL script). It's the fastest, most reviewable, and doesn't pollute application code. Put it in `terminalAutobusesBackend/src/main/resources/data.sql` or as a standalone migration script. Hibernate will execute `data.sql` if `spring.jpa.defer-datasource-initialization=true` and the file exists.

### Recommendation

Implement all 5 items in this order:

1. **Bug 1** (SalidaService chofer validation) — add 3-line check in `create()` and `update()`, same pattern as `asignarAutobus`
2. **Bug 2** (PublicController origenId filter) — add `origenId` param + new repo method `findByActivoTrueAndOrigenId`
3. **Data SQL script** — single `data-poblacion.sql` with:
   - 100 choferes (1 per existing autobus)
   - 100 colectores (1 per existing autobus)
   - 400 additional autobuses (4 more per route = 5 total) with chofer+colector
   - 400 additional horarios (LUNES-VIERNES, SABADO, DOMINGO copies with adjusted hours)
4. **Assignment script** — UPDATE autobuses to link existing 100 to their chofer and colector

### Risks

- **Chofer/Colector cedula uniqueness**: `PersonaBase.cedula` is `unique = true` (line 26). Must ensure all 500+ cedulas are unique across both choferes AND colectores (they share the column space? No — different tables). Actually, `@Column(unique = true)` applies per-table. So `choferes.cedula` and `colectores.cedula` can overlap. Still, within each table they must be unique.
- **Autobús conflict validation**: When assigning chofer/colector via `AutobusService.update()`, it validates the person isn't already assigned to another autobús. The SQL script must set these AFTER the autobuses are created, or use IDs in the correct order.
- **Hibernate data.sql execution order**: If using `data.sql`, need `spring.jpa.defer-datasource-initialization=true` and ensure foreign keys reference existing IDs. `schema.sql` (if used) runs before Hibernate DDL; `data.sql` runs after Hibernate DDL.
- **Horario interval calculation**: Going from 60-min intervals to e.g. 30-min will generate WAY more salidas per day. Each route with `horaInicio=05:00`, `horaFin=20:00` at 30min intervals = 31 salidas per route per day × 100 routes = 3,100 daily salidas. The scheduler generates these automatically. Is 3,100 salidas/day acceptable for the app? If not, keep 60-min intervals.
- **Test data vs real data**: The SQL script should be clearly marked as test/development data, not run in production. A `data-local.sql` that's gitignored from `application-prod.properties` is safer.

### Ready for Proposal

Yes — all areas are fully explored. The orchestrator should tell the user:
- Both bugs are confirmed, low-effort fixes
- Data population is best done as a SQL script (Approach 1)
- The SQL approach avoids polluting application code with seed logic
- Total effort: ~2-3 days (1 day for bugs, 1-2 days for data script generation)
