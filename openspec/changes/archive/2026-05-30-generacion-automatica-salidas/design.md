# Design: Generación Automática de Salidas

## Technical Approach

Scheduler `@Scheduled` a las 05:00 (America/Caracas) + endpoint manual `POST /generar-del-dia` con la misma lógica. El scheduler consulta `HorarioRepository` por `diaSemana = hoy`, valida `terminalOrigen == ruta.origen`, y genera `Salida` para cada intervalo. Dos endpoints nuevos PUT para asignar autobús y ajustar retraso con propagación en memoria. API pública extendida con `horaReal`.

## Architecture Decisions

| Decisión | Opciones | Tradeoff | Decisión |
|----------|----------|----------|----------|
| Idempotencia scheduler | `existsByHorarioIdAndDate()` vs tabla bitácora | Bitácora añade entidad y migración; query simple alcanza | Query JPQL: `count salidas where horario_id = ? and date(hora_programada) = ?` |
| Propagación | En memoria (Java) vs SQL UPDATE secuencial | SQL requiere CTE/ventana; en memoria es testeable y el volume n es bajo (< 30 salidas/día/ruta) | En memoria: `findByRutaIdAndFechaAfter(horaProgramada)` → ordenar → sumar retraso |
| `@EnableScheduling` | En aplicación main vs @Configuration separada | Main ya existe, una línea más no duele; clase separada es más limpia | `@Configuration @EnableScheduling` separada en `config/SchedulingConfig.java` |
| `horario_id` nullable | `nullable = false` nuevo vs mantener | Migración de salidas existentes (~0) es trivial; pero retrocompat es más segura | Mantener `nullable = true` en JPA, forzar no-nulo en código del scheduler |
| Filtro público | Solo `PROGRAMADA` y `EN_CURSO` vs todo excepto `CANCELADA` | Hoy excluye solo `CANCELADA`; `COMPLETADA` ya no se muestra al público | Cambiar query pública a `estado IN (PROGRAMADA, EN_CURSO)` |
| Orden público | `horaReal` vs `horaProgramada` | El público necesita ver la hora real corregida; ordenar por `horaReal` con fallback `horaProgramada` | `ORDER BY (horaProgramada + retrasoMinutos), horaProgramada` |

## Data Flow

**Scheduler / Generación manual:**
```
Cron 05:00 ─→ SalidaScheduler.generarSalidasDelDia()
                   │
                   ├─→ HorarioRepo.findByActivoTrueAndDiaSemana(hoy)
                   │       └─→ [Horario: LUNES 06-08 c/60min, ...]
                   │
                   └─→ por cada Horario:
                         ├─→ validar terminalOrigen == ruta.origen
                         ├─→ SalidaRepo.countByHorarioIdAndFecha(horarioId, today)
                         │       └─→ si > 0 → skip (idempotencia)
                         └─→ generar salidas desde horaInicio hasta horaFin
                               └─→ salidaRepository.saveAll(salidas)
```

**Asignación de autobús:**
```
PUT /salidas/{id}/asignar-autobus { autobusId }
  → SalidaService.asignarAutobus(id, autobusId)
      ├─→ validar salida existe, estado PROGRAMADA
      ├─→ validar autobús activo, con chofer, mismo terminal
      ├─→ validar no solapa otra salida del mismo bus (misma hora y día)
      └─→ salida.setAutobus(bus) → save
```

**Propagación de retraso:**
```
PUT /salidas/{id}/ajustar-retraso { retrasoMinutos: 30 }
  → SalidaService.ajustarRetrasoConPropagacion(id, 30)
      ├─→ salida.setRetrasoMinutos(30)
      ├─→ SalidaRepo.findByRutaIdAndHoraProgramadaAfter(rutaId, salida.horaProgramada)
      │       └─→ [S2(09:00,PROG), S3(10:00,PROG), S4(11:00,EN_CURSO)]
      └─→ por cada destino if estado == PROGRAMADA:
            destino.setHoraProgramada(destino.horaProgramada + 30min)
            → salidaRepository.saveAll([S1, S2, S3])
```

## File Changes

| File | Acción | Descripción |
|------|--------|-------------|
| `config/SchedulingConfig.java` | **Crear** | `@Configuration @EnableScheduling` + `@Bean` zone America/Caracas |
| `scheduler/SalidaScheduler.java` | **Crear** | `@Scheduled(cron = "0 0 5 * * *")` + método `generarSalidasDelDia()` |
| `repository/HorarioRepository.java` | Modificar | + `findByActivoTrueAndDiaSemana(DiaSemana)` |
| `repository/SalidaRepository.java` | Modificar | + `countByHorarioIdAndFecha()`, + `findByRutaIdAndHoraProgramadaAfter()` |
| `service/SalidaService.java` | Modificar | + `asignarAutobus()`, + `ajustarRetrasoConPropagacion()` |
| `service/HorarioService.java` | Modificar | + `findActivosByDia()` (delegado del scheduler) |
| `controller/SalidaController.java` | Modificar | + 3 endpoints: `POST /generar-del-dia`, `PUT /{id}/asignar-autobus`, `PUT /{id}/ajustar-retraso` |
| `controller/PublicController.java` | Modificar | Filtrar por `PROGRAMADA, EN_CURSO`; ordenar por `horaReal` |
| `dto/response/SalidaResponse.java` | Modificar | + `horaReal` (LocalDateTime), + `tieneAutobus` (boolean) |
| `TerminalAutobusesBackendApplication.java` | Modificar | Quitar `@EnableScheduling` si existe (pasa a `SchedulingConfig`) |
| `services/adminApi.js` | Modificar | + `asignarAutobus()`, + `ajustarRetraso()`, + `generarSalidasDelDia()` |
| `services/publicApi.js` | Modificar | Sin cambios de interfaz (responde nuevos campos automáticamente) |
| `pages/admin/SalidasPage.jsx` | Modificar | + columna autobús, + filtro "sin autobús", + botones asignar/ajustar |
| `pages/MainPage.jsx` | Modificar | Usar `horaReal` en lugar de `horaProgramada` para próxima salida |
| `components/TerminalCard.jsx` | Modificar | Usar `horaReal` donde esté disponible; mostrar retraso si existe |

## Interfaces / Contracts

**Nuevos endpoints protegidos:**

```
POST /api/v1/salidas/generar-del-dia
  → 200 OK { "salidasGeneradas": 42, "horariosProcesados": 5, "horariosSaltados": 1 }

PUT /api/v1/salidas/{id}/asignar-autobus
  Body: { "autobusId": Long }
  → 200 OK SalidaResponse | 400 autobús otro terminal/sin chofer | 409 conflicto horario

PUT /api/v1/salidas/{id}/ajustar-retraso
  Body: { "retrasoMinutos": int } (≥ 0)
  → 200 OK SalidaResponse | 404 no encontrada | 403 otro terminal
```

**DTO `SalidaResponse` modificado:**

```java
// Nuevos campos agregados:
private LocalDateTime horaReal;   // horaProgramada + retrasoMinutos
private boolean tieneAutobus;     // autobusId != null
```

`horaReal` se calcula en `fromEntity()` como `horaProgramada.plusMinutes(retrasoMinutos)`.

## Testing Strategy

| Capa | Qué probar | Cómo |
|------|-----------|------|
| Unit/Scheduler | Generación de salidas desde horarios activos, idempotencia, skip horario inválido | Mock repositorios; assert cantidad de salidas generadas |
| Unit/SalidaService | `asignarAutobus` validaciones (otro terminal, sin chofer, conflicto horario) | JUnit 5 + Mockito, cada escenario del spec |
| Unit/SalidaService | `ajustarRetrasoConPropagacion`: propagación correcta, skip estados no-PROGRAMADA | Mock repositorios; verificar horas actualizadas |
| Int/Repository | Queries nuevas (`countByHorarioIdAndFecha`, `findByRutaIdAndHoraProgramadaAfter`) | @DataJpaTest con H2 o MysqlContainer |
| Int/Controller | Endpoints con WebMvcTest, autorización terminal | @WebMvcTest con Security mock |
| Frontend | Manual: flujo asignar autobús, ajustar retraso, vista pública con horaReal | Sin tests automatizados según política del proyecto |

## Migration / Rollout

No se requiere migración de datos. Las salidas existentes sin `horario` siguen funcionando (`horario_id = null`). El scheduler genera salidas nuevas desde el día siguiente de su activación. Rollback: comentar `@Scheduled` y remover endpoints nuevos. Las salidas generadas persisten sin problema (son datos válidos).

## Open Questions

- [ ] ¿Qué pasa si el scheduler falla (app caída a las 05:00)? — El endpoint manual `POST /generar-del-dia` cubre recuperación. ¿Necesitamos reintento automático?
- [ ] Zona horaria: el spec dice `America/Caracas`. ¿MySQL ya está configurado con esa zona o usamos `ZoneId` en Java?
