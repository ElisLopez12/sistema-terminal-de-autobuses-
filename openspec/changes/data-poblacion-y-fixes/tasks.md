# Tasks: Data Population & Bug Fixes

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~443 (10 bug1 + 35 bug2 + 400 seed + 2 config + 50 tests) |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | auto-forecast |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend + Frontend fixes | Single PR | Independent from seed data |
| 2 | Seed data + dev config | Single PR | Depends on nothing — runs last |

## Phase 1: Backend Bug Fixes

- [x] 1.1 `SalidaService.java` — add `autobus.getChofer() == null` guard after `isActivo()` in `create()` (line 184)
- [x] 1.2 `SalidaService.java` — same guard in `update()` (line 244)
- [x] 1.3 `RutaRepository.java` — add `findByActivoTrueAndOrigenId(Long, Pageable)`
- [x] 1.4 `PublicController.java` — add `@RequestParam(required=false) Long origenId` to `getRutas()` + conditional repo call

## Phase 2: Frontend Integration

- [x] 2.1 `publicApi.js` — add optional `{ origenId }` param to `getRutas()` signature
- [x] 2.2 `MainPage.jsx` — pass `origenId` from terminal to `getRutas()` call

## Phase 3: Seed Data

- [x] 3.1 Create `application-dev.properties` — `defer-datasource-initialization=true`, `sql.init.mode=always`
- [x] 3.2 Create `data-dev.sql` — `DELETE FROM horarios`
- [x] 3.3 `data-dev.sql` — INSERT 500 choferes + 500 colectores (50/terminal, cedulas secuenciales)
- [x] 3.4 `data-dev.sql` — INSERT 500 autobuses (5/ruta, route IDs 106-205) + UPDATE chofer_id/colector_id
- [x] 3.5 `data-dev.sql` — INSERT ~700 horarios (LUN-VIE 05:00-20:00, SÁB 06:00-18:00, DOM 08:00-14:00)

## Phase 4: Testing & Verification

- [x] 4.1 `SalidaServiceTest.java` — unit test: `create()` rejects autobus without chofer
- [x] 4.2 `SalidaServiceTest.java` — unit test: `update()` rejects autobus without chofer
- [ ] 4.3 Manual: start app with `dev` profile, call `GET /public/rutas?origenId={id}`, verify filtered
- [ ] 4.4 Manual: load frontend, verify correct route count per terminal card
