# Proposal: generacion-automatica-salidas

## Intent

Eliminar la creación manual de salidas diarias. Hoy el admin crea cada salida a mano aunque el horario ya define frecuencia y días. Queremos que un scheduler genere salidas para cada horario activo al inicio del día, y el admin solo asigne el autobús y ajuste retrasos. La página pública debe reflejar los ajustes en tiempo real.

## Scope

### In Scope
- Scheduler diario que genera salidas del día según horarios activos (día de semana matching)
- `Salida.horario` pasa de nullable a NOT NULL en las generadas
- Endpoint `PUT /salidas/{id}/asignar-autobus` — admin asigna bus
- Endpoint `PUT /salidas/{id}/ajustar-retraso` — ajusta retraso y propaga a salidas siguientes de la misma ruta ese día
- Página pública muestra `horaProgramada + retrasoMinutos` con hora real de salida
- Admin puede ver salidas sin autobús asignado y filtrar por ello

### Out of Scope
- Generar salidas para días futuros (solo el día actual)
- Edición de horarios desde el panel de salidas
- Notificaciones push/correo cuando se ajusta un retraso
- Historial de retrasos o auditoría de cambios

## Capabilities

### New Capabilities
- `generacion-automatica-salidas`: Scheduler que crea salidas del día desde horarios activos
- `asignacion-autobus-salida`: Admin asigna/reasigna autobús a una salida generada
- `ajuste-retraso-propagacion`: Admin ajusta retraso de una salida con propagación a siguientes de la misma ruta ese día
- `public-salidas-tiempo-real`: API pública expone hora real (programada + retraso) para la página principal

### Modified Capabilities
- None (no existing specs to modify)

## Approach

1. **Scheduler**: `@Scheduled(cron = "0 0 5 * * *")` en nuevo `SalidaScheduler.java`. Consulta horarios activos cuyo `diaSemana` coincida con hoy. Por cada horario, genera salidas desde `horaInicio` hasta `horaFin` con paso `intervaloMinutos`. Cada salida queda con `estado=PROGRAMADA`, `autobus=null`.
2. **Asignación**: Nuevo endpoint en `SalidaController` que valida que el autobús pertenezca al terminal de origen y tenga chofer asignado.
3. **Propagación de retraso**: Al ajustar retraso en salida N, las salidas N+1 del mismo día/ruta corren su horaProgramada en la misma cantidad (sin bajar del mínimo intervalo).
4. **API pública**: `GET /public/salidas` ya existe; se modifica para incluir `horaReal = horaProgramada + retrasoMinutos` en la respuesta.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `model/Salida.java` | Modified | `horario` pasa a nullable=false en las generadas (relación fuerte) |
| `service/SalidaService.java` | Modified | + métodos asignarBus, ajustarRetrasoConPropagacion |
| `service/HorarioService.java` | Modified | + método para generar salidas desde horario |
| `controller/SalidaController.java` | Modified | + endpoints asignar-autobus, ajustar-retraso |
| `controller/PublicController.java` | Modified | Respuesta incluye horaReal |
| `dto/response/SalidaResponse.java` | Modified | + campo horaReal |
| `pages/admin/SalidasPage.jsx` | Modified | Filtro "sin autobús", botones asignar/ajustar |
| `pages/MainPage.jsx` | Modified | Muestra horaReal con retraso visible |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Scheduler duplica salidas si corre múltiples veces | Medium | Idempotencia: verificar que no existan salidas para ese horario+fecha antes de generar |
| Propagación de retraso encadena errores | Low | Propagar solo si la salida destino está PROGRAMADA y no tiene autobús asignado aún |
| Admin asigna bus que ya tiene otra salida solapada | Low | Validar solapamiento horario del autobús en el mismo día |

## Rollback Plan

1. Desactivar `@Scheduled` commentando la annotation en `SalidaScheduler.java`
2. Las salidas ya generadas quedan en BD como si el admin las hubiera creado manualmente (no se pierden)
3. Los endpoints nuevos (`asignar-autobus`, `ajustar-retraso`) pueden seguir usándose opcionalmente
4. Revertir cambios en `PublicController` y `MainPage.jsx` si la horaReal causa confusión

## Dependencies

- Spring `@EnableScheduling` ya configurado (verificar en clase main)
- El scheduler depende de que el horario tenga `diaSemana` correcto y `activo=true`

## Success Criteria

- [ ] Scheduler ejecutado manualmente (o al iniciar el día) crea todas las salidas del día para horarios activos
- [ ] Admin asigna autobús a una salida y persiste correctamente
- [ ] Admin ajusta retraso en salida #1 de una ruta, y las salidas #2 y #3 de ese mismo día/ruta corren su horaProgramada
- [ ] Página pública muestra `horaProgramada + retraso` para cada salida
- [ ] No se generan salidas duplicadas si el scheduler corre dos veces el mismo día
