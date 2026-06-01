# Sistema de Gestión de Terminales de Autobuses

Monorepo con frontend React + Vite y backend Spring Boot.

## Stack

| Capa | Tecnología | Versión |
|------|-----------|---------|
| Backend | Spring Boot (WebMVC + Data JPA) | 4.0.6 |
| Backend | Java | 21 |
| Backend | MySQL | 8.0.46 |
| Backend | Maven wrapper | ./mvnw |
| Backend | Lombok, Hibernate 7.2, Tomcat 11 | — |
| Frontend | React + Vite + JavaScript | — |
| Frontend | Axios (HTTP) | — |
| Frontend | Tailwind CSS v4 | — |
| Frontend | Auth: JWT via axios interceptor + AuthContext (localStorage) | — |

## Estructura del proyecto

```
terminalAutobuses-v2/
├── terminalAutobusesBackend/     ← Spring Boot API
│   ├── src/main/java/com/elis_lopez/terminalAutobusesBackend/
│   │   ├── controller/           ← @RestController, endpoints /api/v1/...
│   │   ├── dto/request/          ← DTOs de entrada con validaciones
│   │   ├── dto/response/         ← DTOs de salida con fromEntity()
│   │   ├── exception/            ← GlobalExceptionHandler + ResourceNotFoundException
│   │   ├── model/                ← Entidades JPA (8 entidades)
│   │   │   └── enums/            ← RolUsuario, DiaSemana, EstadoSalida
│   │   ├── repository/           ← Spring Data JPA interfaces
│   │   ├── service/              ← Lógica de negocio (constructor injection)
│   │   ├── config/               ← SecurityConfig, JwtAuthFilter, JwtUtil, SchedulingConfig
│   │   └── scheduler/            ← Tareas programadas (generación salidas, limpieza)
│   └── src/main/resources/
│       └── application.properties
├── terminalAutobusesFrontend/    ← React + Vite SPA
│   └── src/
│       ├── pages/admin/          ← Vistas del panel administrativo
│       ├── pages/public/         ← Vistas públicas (consulta de salidas)
│       ├── components/           ← Componentes reutilizables (Modal, etc.)
│       ├── context/              ← AuthContext (JWT en localStorage)
│       └── services/             ← adminApi.js, api.js (axios instance con interceptor JWT)
└── AGENTS.md                     ← Este archivo
```

## Regla de pertenencia (la más importante)

Cada autobús, chofer y colector pertenece a **UN solo terminal** (su base). El admin de ese terminal gestiona sus activos. Si un autobús de Maracay viaja a San Juan, el admin de San Juan **NO** gestiona esa unidad — solo ve rutas y salidas que **salen de San Juan**.

## Decisiones de modelo

| Decisión | Elección |
|---|---|
| FK Autobús ↔ Chofer/Colector | FK en **Autobús** (chofer_id, colector_id nullable). El bus "tiene" tripulación. |
| Destino de ruta | **Texto libre** (`destinoNombre`), no FK a Terminal. Puede ser una plaza, universidad, etc. |
| Horario vs Salida | **Horario** = plantilla guía (frecuencia, días). **Salida** = viaje individual creado manualmente por el admin. |
| Hora pública | `Salida.horaProgramada + retrasoMinutos` |
| Borrado | **Soft delete** — todas las entidades tienen `activo` (boolean) y nunca se borran físicamente. |
| Timestamps | `created_at` / `updated_at` manejados con `@PrePersist` / `@PreUpdate` en cada entidad. |
| Zona horaria | `America/Caracas` en MySQL, Hibernate, Jackson y JVM. |

## Entidades (8)

| Entidad | Tabla | PK | Relaciones clave |
|---|---|---|---|
| Terminal | `terminales` | id | — |
| Usuario | `usuarios` | id | ManyToOne → Terminal (nullable) |
| Autobus | `autobuses` | id | ManyToOne → Terminal; OneToOne → Chofer, Colector (nullable); ManyToOne → Ruta (nullable) |
| Chofer | `choferes` | id | ManyToOne → Terminal (extends PersonaBase) |
| Colector | `colectores` | id | ManyToOne → Terminal (extends PersonaBase) |
| Ruta | `rutas` | id | ManyToOne → Terminal (origen). Paradas son `@ElementCollection` embebida (ParadaEmbeddable) |
| Horario | `horarios` | id | ManyToOne → Ruta, Terminal (origen) |
| Salida | `salidas` | id | ManyToOne → Ruta, Horario (nullable), Terminal, Autobus (nullable) |

## Estados de Salida

| Estado | Badge | Descripción |
|--------|-------|-------------|
| `PROGRAMADA` | Gris (sin bus) / Azul con ✓ (con bus) | Programada, distinción visual si ya tiene autobús asignado |
| `ABORDAJE` | Ámbar | Pasajeros subiendo |
| `EN_CURSO` | Verde | Viaje en curso |
| `COMPLETADA` | Azul oscuro | Viaje terminado (reemplazó `FINALIZADA`) |
| `CANCELADA` | Rojo | Cancelada |

El cambio rápido de estado se hace mediante `PATCH /api/v1/salidas/{id}/estado` (evita validaciones `@NotNull` del PUT).

## API — Resumen

Base URL: `http://localhost:8080/api/v1` — Formato JSON — Paginación: `?page=0&size=20&sort=id,asc`

### Públicos (sin token)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/public/rutas` | Rutas activas con paradas |
| GET | `/public/rutas/{id}` | Ruta + paradas + horarios |
| GET | `/public/salidas` | Salidas activas no canceladas (`?rutaId=&origenId=`) |
| GET | `/public/terminales` | Terminales activos |

### Autenticación

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/auth/login` | Login → JWT (24h). Body: `{ username, password }` |

### CRUD protegidos (requieren token)

| Recurso | Endpoints | Filtros |
|---------|-----------|---------|
| Terminales | `GET/POST/PUT/DELETE /terminales` | — |
| Usuarios | `GET/POST/PUT/DELETE /usuarios` | — |
| Autobuses | `GET/POST/PUT/DELETE /autobuses` | `?terminalId=` |
| Choferes | `GET/POST/PUT/DELETE /choferes` | `?terminalId=` |
| Colectores | `GET/POST/PUT/DELETE /colectores` | `?terminalId=` |
| Rutas | `GET/POST/PUT/DELETE /rutas` | — |
| Paradas | `GET/POST/PUT/DELETE /paradas` | `?rutaId=` |
| Horarios | `GET/POST/PUT/DELETE /horarios` | `?rutaId=&terminalId=` |
| Salidas | `GET/POST/PUT/DELETE /salidas` | `?rutaId=&terminalId=` |

### Endpoints especiales de Salidas

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/salidas/generar-del-dia` | Genera salidas del día desde horarios activos |
| PUT | `/salidas/{id}/asignar-autobus` | Asigna un autobús a una salida (con validación de ruta) |
| PUT | `/salidas/{id}/ajustar-retraso` | Ajusta retraso y propaga a salidas siguientes |
| PATCH | `/salidas/{id}/estado` | Cambia estado sin validar otros campos |

## Seguridad

| Concepto | Detalle |
|---|---|
| Autenticación | JWT HS256 con 24h de expiración |
| Passwords | BCrypt con `PasswordEncoder` |
| Sesiones | Stateless (no se usa HttpSession) |
| Endpoints públicos | `/api/v1/public/**` y `/api/v1/auth/**` |
| Endpoints protegidos | Todo lo demás — requieren `Authorization: Bearer <token>` |
| Admin inicial | Se crea automáticamente al arrancar: `admin:admin123` |
| Roles | `CENTRAL_ADMIN` — acceso completo. `TERMINAL_ADMIN` — restringido a su terminal |
| Errores | 401 si falta token, 403 si el rol no tiene permiso |

El filtro JWT (`config/JwtAuthFilter.java`) omite explícitamente `/api/v1/public/` y `/api/v1/auth/`. CORS global permite cualquier origen con métodos GET, POST, PUT, PATCH, DELETE, OPTIONS.

## Frontend — Convenciones

- **React + Vite + JavaScript** con Tailwind CSS v4
- **Auth**: JWT en localStorage bajo key `auth` con formato `{ token, userId, username, rol, terminalId, terminalNombre }`
- **Axios interceptor** (`services/api.js`): lee `localStorage.auth.token` y lo agrega como `Authorization: Bearer <token>` a cada request
- **Roles**: `CENTRAL_ADMIN` ve todo. `TERMINAL_ADMIN` restringido a su terminal
- **Responsive**: mobile-first con Tailwind. Sidebar oculto en mobile con overlay hamburguesa. Tablas con `overflow-x-auto`

## Desarrollo

```bash
# Backend (desde terminalAutobusesBackend/)
./mvnw spring-boot:run    # http://localhost:8080

# Frontend (desde terminalAutobusesFrontend/)
pnpm dev                  # http://localhost:5173
pnpm build                # compila a dist/
```

## Base de datos

- Host: `localhost:3306`
- Database: `terminal`
- User: `root`
- DDL: `spring.jpa.hibernate.ddl-auto=update` (Hibernate gestiona el schema)

## Convenciones de código

### Backend
- **Constructor injection** con `@RequiredArgsConstructor` + campos `private final`
- Servicios con `@Service`, `@Transactional`, `@Slf4j`
- Controladores con `@RestController`, `@RequestMapping`, `ResponseEntity`
- DTOs de request con `jakarta.validation` (@NotBlank, @NotNull, @Size)
- DTOs de response con método estático `fromEntity(Entity)`
- `@Valid` en `@RequestBody` de endpoints POST/PUT
- Manejo global de errores con `@ControllerAdvice`
- Excepciones unchecked: `ResourceNotFoundException` → 404, validación → 400, integridad → 409
- **Documentación**: toda clase pública debe tener Javadoc. Todo método público debe tener `@param` y `@return`

### Frontend
- Componentes funcionales con hooks (`useState`, `useEffect`, `useCallback`)
- API calls en `services/adminApi.js` (importa `api` de `services/api.js`)
- Manejo de errores con `extractError` de `services/errorUtils.js`
- Modales reutilizables con componente `<Modal>`

## Scheduler (tareas automáticas)

| Tarea | Frecuencia | Descripción |
|-------|-----------|-------------|
| `generarSalidasDelDia` | Diario 05:00 | Genera salidas desde horarios activos del día |
| `limpiarSalidasExpiradasSinAutobus` | Cada 5 min | Soft-delete de salidas PROGRAMADA sin autobús y con hora vencida |

## Códigos de error

| Status | Significado |
|--------|------------|
| 200 | OK |
| 201 | Creado |
| 204 | Eliminado (sin contenido) |
| 400 | Error de validación |
| 401 | No autenticado |
| 403 | Sin permiso |
| 404 | No encontrado |
| 409 | Conflicto (duplicado/integridad) |
| 500 | Error interno |
