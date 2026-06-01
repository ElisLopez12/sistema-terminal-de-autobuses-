# Proposal: Data Population & Bug Fixes

## Intent

The app has 0 choferes/colectores, 1 bus per route, and horarios only on LUNES — unusable for realistic testing. Two bugs compound this: buses without drivers can bypass chofer validation when assigned via `POST/PUT /salidas`, and `GET /public/rutas` returns unfiltered data making the main page show 0 routes for most terminals.

## Scope

### In Scope
- **Bug 1**: Add chofer null-check in `SalidaService.create()` and `update()`
- **Bug 2**: Add `origenId` filter to `GET /public/rutas` (backend + frontend)
- **Seed data**: SQL script — 100 choferes, 100 colectores, 400 autobuses (→ 500 total), 400+ horarios (LUNES–VIERNES, SÁBADO, DOMINGO)

### Out of Scope
- No new endpoints or UI features
- No frontend design changes
- No production deployment config

## Capabilities

### New
- `public-rutas-filtro`: Public routes endpoint with optional `origenId` filter
- `data-seed-poblacion`: SQL seed script for dev data population (choferes, colectores, autobuses, horarios)

### Modified
- `asignacion-autobus-salida`: Expand chofer validation scope — current spec covers `PUT /asignar-autobus` only; MUST also cover `POST /salidas` and `PUT /salidas/{id}`

## Approach

| Item | How |
|------|-----|
| **Bug 1** | Add 3-line guard in `create()`/`update()` after autobus load: `if (autobus.getChofer() == null) throw...` — same pattern as `asignarAutobus()` |
| **Bug 2** | Add `@RequestParam(required=false) Long origenId` to `PublicController.getRutas()`, add `findByActivoTrueAndOrigenId()` to `RutaRepository`, wire `origenId` in frontend `publicApi.getRutas()` and `MainPage.jsx` |
| **Seed data** | Single `data-poblacion.sql` with `spring.jpa.defer-datasource-initialization=true` (dev profile): DELETE old horarios → INSERT 100 choferes → 100 colectores → 400 autobuses → UPDATE existing 100 autobuses → INSERT horarios LUNES–VIERNES 05:00–20:00 /60min, SÁBADO 06:00–18:00 /60min, DOMINGO 08:00–14:00 /60min |

## Affected Areas

| Area | Impact |
|------|--------|
| `SalidaService.java` | Modified — chofer validation in create()/update() |
| `PublicController.java` | Modified — origenId param on getRutas() |
| `RutaRepository.java` | Modified — findByActivoTrueAndOrigenId() |
| `publicApi.js` | Modified — origenId param on getRutas() |
| `MainPage.jsx` | Modified — pass origenId in call |
| `data-poblacion.sql` | New — seed script (dev only) |
| `application-dev.properties` | Modified — defer-datasource-init flag |

## Risks

| Risk | Lvl | Mitigation |
|------|-----|------------|
| SQL cedula collision | Low | Sequential generation, unique per table |
| Script partial failure | Low | Transactional SQL, verify FK order |
| data.sql runs in prod | Med | Profile-specific file (`application-dev`) only |

## Rollback Plan

1. Revert `SalidaService.java` guards
2. Revert `PublicController.java` + `RutaRepository.java`
3. Drop DB schema → Hibernate recreates via `ddl-auto=update`
4. Remove `data-poblacion.sql` and `spring.jpa.defer-datasource-initialization=true`

## Success Criteria

- [ ] `POST /salidas` with bus without driver → 400 Bad Request
- [ ] `GET /public/rutas?origenId=1` → only terminal 1 routes
- [ ] Main page shows correct route counts per terminal
- [ ] 500 autobuses exist (5/route), all with chofer+colector
- [ ] Horarios exist for LUNES–VIERNES, SÁBADO, DOMINGO per route
