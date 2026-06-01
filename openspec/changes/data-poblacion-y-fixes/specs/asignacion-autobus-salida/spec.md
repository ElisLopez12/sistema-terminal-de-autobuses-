# Delta for Asignación de Autobús a Salida

## MODIFIED Requirements

### Requirement: Validaciones de integridad

The system MUST validar las reglas de integridad al asignar o cambiar el autobús de una salida mediante cualquier endpoint (`PUT /asignar-autobus`, `POST /salidas`, `PUT /salidas/{id}`):

1. La salida MUST existir y estar activa
2. El autobús MUST existir y estar activo (`activo = true`)
3. El autobús MUST tener un chofer asignado (`autobus.chofer != null`)
4. El autobús MUST pertenecer al mismo terminal que `salida.terminalOrigen`
5. El autobús MUST no tener otra salida PROGRAMADA o EN_CURSO en el mismo horario (misma hora o solapada dentro del intervalo de la salida)

(Previously: validaciones solo referenciaban PUT /asignar-autobus como contexto de aplicación)

#### Scenario: Autobús de otro terminal es rechazado

- GIVEN salida de "Maracay", autobús de "Valencia"
- WHEN se asigna ese autobús a la salida
- THEN MUST responder 400 Bad Request

#### Scenario: Autobús sin chofer es rechazado

- GIVEN autobús activo sin chofer
- WHEN se asigna a una salida (cualquier endpoint)
- THEN MUST responder 400 Bad Request

#### Scenario: Autobús ocupado en otra salida simultánea

- GIVEN autobús con salida PROGRAMADA a las 10:00 del mismo día
- WHEN se asigna a otra salida también a las 10:00
- THEN MUST responder 409 Conflict

#### Scenario: POST/PUT rechaza autobús sin chofer

- GIVEN un autobús activo sin chofer asignado
- WHEN se invoca `POST /api/v1/salidas` o `PUT /api/v1/salidas/{id}` con ese `autobusId`
- THEN MUST responder 400 Bad Request
