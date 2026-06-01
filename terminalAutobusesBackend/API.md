# API — Terminal de Autobuses

Base URL: `http://localhost:8080/api/v1`

Formato: JSON. Paginación: `?page=0&size=20&sort=id,asc` (default page=0, size=20, sort=id).

---

## Autenticación

### `POST /auth/login` → público

Login. Devuelve un JWT (24h) para usar en el header `Authorization: Bearer <token>`.

```json
// Request
{ "username": "admin", "password": "admin123" }

// Response 200
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "username": "admin",
  "rol": "CENTRAL_ADMIN",
  "terminalId": null,
  "terminalNombre": null
}

// Response 401
{ "error": "Unauthorized", "message": "Credenciales inválidas", "status": 401 }
```

---

## Terminales (`/terminales`)

Requiere token. `TERMINAL_ADMIN` solo puede ver su terminal.

### `GET /terminales`
```json
// Response 200
{
  "content": [
    { "id": 1, "nombre": "Terminal Maracay", "ubicacion": "Maracay", "activo": true }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
}
```

### `GET /terminales/{id}` → 200 / 404
### `POST /terminales` (solo CENTRAL_ADMIN) → 201 / 403
```json
// Request
{ "nombre": "Terminal Maracay", "ubicacion": "Maracay" }
```

### `PUT /terminales/{id}` (solo CENTRAL_ADMIN) → 200 / 403
### `DELETE /terminales/{id}` (solo CENTRAL_ADMIN) → 204 / 403

---

## Autobuses (`/autobuses`)

Requiere token. `TERMINAL_ADMIN` crea/ve solo buses de su terminal (ignora `terminalId` en create).

### `GET /autobuses?terminalId=`
```json
// Response 200 (paginated)
{
  "content": [
    {
      "id": 1,
      "numeroUnidad": "1",
      "matricula": "ABC123",
      "marca": null,
      "modelo": null,
      "anio": null,
      "capacidadPasajeros": 45,
      "terminalId": 2,
      "terminalNombre": "Terminal Maracay",
      "choferId": null,
      "choferNombre": null,
      "colectorId": null,
      "colectorNombre": null,
      "rutaId": null,
      "rutaNombre": null,
      "activo": true
    }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
}
```

### `GET /autobuses/{id}` → 200 / 404
### `POST /autobuses` → 201
```json
// Request
{
  "numeroUnidad": "001",
  "matricula": "ABC123",
  "marca": "Mercedes",          // opcional
  "modelo": "Sprinter",         // opcional
  "anio": 2022,                 // opcional, min 1900
  "capacidadPasajeros": 45,     // opcional, min 1
  "terminalId": 2,
  "choferId": null,             // opcional
  "colectorId": null,           // opcional
  "rutaId": null                // opcional, debe ser del mismo terminal
}
```

### `PUT /autobuses/{id}` → 200
### `DELETE /autobuses/{id}` → 204

---

## Choferes (`/choferes`), Colectores (`/colectores`)

Estructura idéntica. Requiere token. `TERMINAL_ADMIN` restringido a su terminal.

### `GET /choferes?terminalId=`
```json
// Response 200 (paginated)
{
  "content": [
    {
      "id": 1,
      "nombre": "Carlos",
      "apellido": "Gómez",
      "cedula": "V12345678",
      "telefono": "0412-1234567",
      "fechaNacimiento": null,
      "direccion": null,
      "terminalId": 2,
      "terminalNombre": "Terminal Maracay",
      "activo": true
    }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
}
```

### `GET /choferes/{id}` / `GET /colectores/{id}` → 200 / 404
### `POST /choferes` / `POST /colectores` → 201
```json
// Request
{
  "nombre": "Carlos",
  "apellido": "Gómez",             // obligatorio
  "cedula": "V12345678",           // obligatorio
  "telefono": "0412-1234567",      // opcional
  "terminalId": 2,
  "direccion": "Av. Bolívar",      // opcional
  "email": "juan@email.com"        // opcional
}
```

### `PUT /{id}` → 200
### `DELETE /{id}` → 204

---

## Rutas (`/rutas`)

Requiere token. `TERMINAL_ADMIN` restringido a su terminal.

### `GET /rutas` → paginado
```json
// Response
{
  "content": [
    {
      "id": 1,
      "nombre": "Maracay - Valencia",
      "origenId": 2,
      "origenNombre": "Terminal Maracay",
      "destinoNombre": "Valencia",
      "destinoUbicacion": null,
      "distanciaKm": 120,
      "duracionEstimadaMin": 90,
      "precioBase": 5.50,
      "activo": true
    }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
}
```

### `GET /rutas/{id}` → 200 / 404
### `POST /rutas` → 201
```json
// Request
{
  "nombre": "Maracay - Valencia",
  "destinoNombre": "Valencia",
  "origenId": 2,                // FK al terminal de origen
  "precioBase": 5.50,           // opcional
  "distanciaKm": 120,           // opcional
  "duracionEstimadaMin": 90     // opcional
}
```

### `PUT /rutas/{id}` → 200
### `DELETE /rutas/{id}` → 204

---

## Paradas (`/paradas`)

Requiere token. `TERMINAL_ADMIN` solo paradas de rutas de su terminal.

### `GET /paradas?rutaId=` → paginado
```json
// Response
{
  "content": [
    {
      "id": 1,
      "nombre": "Plaza Bolívar - Maracay",
      "direccion": "Av. Las Delicias",
      "orden": 1,
      "rutaId": 1,
      "rutaNombre": "Maracay - Valencia",
      "activo": true
    }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
}
```

### `GET /paradas/{id}` → 200 / 404
### `POST /paradas` → 201
```json
// Request
{
  "nombre": "Plaza Bolívar - Maracay",
  "direccion": "Av. Las Delicias",     // opcional
  "orden": 1,                           // opcional
  "rutaId": 1
}
```

### `PUT /paradas/{id}` → 200
### `DELETE /paradas/{id}` → 204

---

## Horarios (`/horarios`)

Requiere token. `TERMINAL_ADMIN` restringido a su terminal.

### `GET /horarios?rutaId=&terminalId=` → paginado
```json
// Response
{
  "content": [
    {
      "id": 1,
      "diaSemana": "LUNES",
      "horaInicio": "06:00",
      "horaFin": "20:00",
      "intervaloMinutos": 30,
      "rutaId": 1,
      "rutaNombre": "Maracay - Valencia",
      "terminalOrigenId": 2,
      "terminalOrigenNombre": "Terminal Maracay",
      "activo": true
    }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
}
```

### `GET /horarios/{id}` → 200 / 404
### `POST /horarios` → 201
```json
// Request
{
  "rutaId": 1,
  "terminalOrigenId": 2,
  "diaSemana": "LUNES",               // LUNES|MARTES|MIERCOLES|JUEVES|VIERNES|SABADO|DOMINGO
  "horaInicio": "06:00",              // HH:mm
  "horaFin": "20:00",                 // HH:mm
  "intervaloMinutos": 30              // min 1
}
```

### `PUT /horarios/{id}` → 200
### `DELETE /horarios/{id}` → 204

---

## Salidas (`/salidas`)

Requiere token. `TERMINAL_ADMIN` restringido a su terminal.

### `GET /salidas?rutaId=&terminalId=` → paginado
```json
// Response
{
  "content": [
    {
      "id": 1,
      "horaProgramada": "2026-06-01T06:00",
      "retrasoMinutos": 0,
      "estado": "PROGRAMADA",
      "rutaId": 1,
      "rutaNombre": "Maracay - Valencia",
      "horarioId": null,
      "terminalOrigenId": 2,
      "terminalOrigenNombre": "Terminal Maracay",
      "autobusId": null,
      "autobusNumeroUnidad": "001",
      "activo": true
    }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
}
```

### `GET /salidas/{id}` → 200 / 404
### `POST /salidas` → 201
```json
// Request
{
  "rutaId": 1,
  "horarioId": null,               // opcional
  "terminalOrigenId": 2,
  "autobusId": null,                // opcional
  "horaProgramada": "2026-06-01T06:00",   // yyyy-MM-dd'T'HH:mm
  "retrasoMinutos": 0,              // opcional, default 0
  "estado": "PROGRAMADA"            // opcional: PROGRAMADA|EN_CURSO|FINALIZADA|CANCELADA
}
```

### `PUT /salidas/{id}` → 200
### `DELETE /salidas/{id}` → 204

---

## Usuarios (`/usuarios`)

Requiere token. `TERMINAL_ADMIN` solo puede crear usuarios `TERMINAL_ADMIN` de su terminal.

### `GET /usuarios` → paginado
```json
// Response
{
  "content": [
    {
      "id": 1,
      "username": "admin_maracay",
      "rol": "TERMINAL_ADMIN",
      "terminalId": 2,
      "terminalNombre": "Terminal Maracay",
      "activo": true
    }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
}
```

### `GET /usuarios/{id}` → 200 / 404
### `POST /usuarios` → 201
```json
// Request
{
  "username": "admin_maracay",
  "password": "maracay123",
  "rol": "TERMINAL_ADMIN",             // CENTRAL_ADMIN | TERMINAL_ADMIN
  "terminalId": 2                      // opcional (obligatorio para TERMINAL_ADMIN)
}
```

### `PUT /usuarios/{id}` → 200
### `DELETE /usuarios/{id}` → 204

---

## Endpoints públicos (`/public`)

No requieren token.

### `GET /public/rutas`
```json
// Response 200 — rutas activas con paradas
{
  "content": [
    {
      "id": 1,
      "nombre": "Maracay - Valencia",
      "origenId": 2,
      "origenNombre": "Terminal Maracay",
      "destinoNombre": "Valencia",
      "destinoUbicacion": null,
      "distanciaKm": null,
      "duracionEstimadaMin": null,
      "precioBase": 5.50,
      "activo": true,
      "paradas": [
        { "id": 1, "nombre": "Plaza Bolívar", "direccion": "...", "orden": 1, "tiempoDesdeSalidaMin": null }
      ]
    }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
}
```

### `GET /public/rutas/{id}` → 200 + paradas + horarios / 404

### `GET /public/salidas?rutaId=&origenId=`
```json
// Response 200 — salidas activas NO canceladas, ordenadas por hora
{
  "content": [
    {
      "id": 1,
      "horaProgramada": "2026-06-01T06:00",
      "retrasoMinutos": 0,
      "estado": "PROGRAMADA",
      "rutaId": 1,
      "rutaNombre": "Maracay - Valencia",
      "terminalOrigenId": 2,
      "terminalOrigenNombre": "Terminal Maracay",
      "autobusId": null,
      "autobusNumeroUnidad": "001",
      "activo": true
    }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
}
```

### `GET /public/terminales` → terminales activos (paginado)

---

## Códigos de error

| Status | Significado |
|--------|------------|
| 200 | OK |
| 201 | Creado |
| 204 | Eliminado (sin contenido) |
| 400 | Error de validación — `{ "fieldErrors": [...], "error": "Validation Failed" }` |
| 401 | No autenticado — `{ "message": "Token de autenticación requerido" }` |
| 403 | Sin permiso — `{ "message": "No tienes permiso para acceder a este recurso" }` |
| 404 | No encontrado |
| 409 | Conflicto (registro duplicado, integridad) |
| 500 | Error interno |

## Paginación

Todos los listados (`GET ...`) devuelven:

```json
{
  "content": [ ... ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,        // página actual
  "size": 20,          // elementos por página
  "first": true,
  "last": false,
  "empty": false
}
```

Parámetros query: `?page=0&size=20&sort=id,asc` (o `sort=nombre,desc`).

## CORS

Abierto a cualquier origen (`*`). No hay restricciones desde frontend.
