# Sistema de Gestión de Terminales de Autobuses — Backend

## Stack

- **Spring Boot** 4.0.6 (WebMVC + Data JPA)
- **Java** 21
- **MySQL** 8.0.46 (schema: `terminal`)
- **Lombok**, **Hibernate** 7.2, **Tomcat** 11
- **Maven** wrapper (./mvnw)

## Arquitectura

### Regla de pertenencia (la más importante)

Cada autobús, chofer y colector pertenece a **UN solo terminal** (su base). El admin de ese terminal gestiona sus activos. Si un autobús de Maracay viaja a San Juan, el admin de San Juan **NO** gestiona esa unidad — solo ve rutas y salidas que **salen de San Juan**.

### Decisiones de modelo

| Decisión | Elección |
|---|---|
| FK Autobús ↔ Chofer/Colector | FK en **Autobús** (chofer_id, colector_id nullable). El bus "tiene" tripulación. |
| Destino de ruta | **Texto libre** (`destinoNombre`), no FK a Terminal. Puede ser una plaza, universidad, etc. |
| Horario vs Salida | **Horario** = plantilla guía (frecuencia, días). **Salida** = viaje individual creado manualmente por el admin. |
| Hora pública | `Salida.horaProgramada + retrasoMinutos` |
| Borrado | **Soft delete** — todas las entidades tienen `activo` (boolean) y nunca se borran físicamente. |
| Timestamps | `created_at` / `updated_at` manejados con `@PrePersist` / `@PreUpdate` en cada entidad. |
| Zona horaria | `America/Caracas` en MySQL, Hibernate, Jackson y JVM. |

### Estructura de paquetes

```
com.elis_lopez.terminalAutobusesBackend/
├── TerminalAutobusesBackendApplication.java
├── controller/       ← @RestController, endpoints /api/v1/...
├── dto/
│   ├── request/      ← DTOs de entrada con validaciones (@NotBlank, etc.)
│   └── response/     ← DTOs de salida con fromEntity() factory
├── exception/        ← GlobalExceptionHandler + ResourceNotFoundException
├── model/            ← Entidades JPA (9 entidades + PersonaBase abstracta)
│   └── enums/        ← RolUsuario, DiaSemana, EstadoSalida
├── repository/       ← Spring Data JPA interfaces
└── service/          ← Lógica de negocio con constructor injection
```

### Entidades (9)

| Entidad | Tabla | PK | Relaciones clave |
|---|---|---|---|
| Terminal | `terminales` | id | — |
| Usuario | `usuarios` | id | ManyToOne → Terminal (nullable) |
| Autobus | `autobuses` | id | ManyToOne → Terminal; OneToOne → Chofer, Colector (nullable); ManyToOne → Ruta (nullable) |
| Chofer | `choferes` | id | ManyToOne → Terminal (extends PersonaBase) |
| Colector | `colectores` | id | ManyToOne → Terminal (extends PersonaBase) |
| Ruta | `rutas` | id | ManyToOne → Terminal (origen) |
| Parada | `paradas` | id | ManyToOne → Ruta |
| Horario | `horarios` | id | ManyToOne → Ruta, Terminal (origen) |
| Salida | `salidas` | id | ManyToOne → Ruta, Horario (nullable), Terminal, Autobus (nullable) |

### API Endpoints

Base: `/api/v1/`

| Recurso | Endpoints | Filtros |
|---|---|---|
| Auth | `POST /auth/login` | — |
| Terminales | `GET/POST/PUT/DELETE /terminales` | — |
| Usuarios | `GET/POST/PUT/DELETE /usuarios` | — |
| Autobuses | `GET/POST/PUT/DELETE /autobuses` | `?terminalId=` |
| Choferes | `GET/POST/PUT/DELETE /choferes` | `?terminalId=` |
| Colectores | `GET/POST/PUT/DELETE /colectores` | `?terminalId=` |
| Rutas | `GET/POST/PUT/DELETE /rutas` | — |
| Paradas | `GET/POST/PUT/DELETE /paradas` | `?rutaId=` |
| Horarios | `GET/POST/PUT/DELETE /horarios` | `?rutaId=&terminalId=` |
| Salidas | `GET/POST/PUT/DELETE /salidas` | `?rutaId=&terminalId=` |
| Público | `GET /public/rutas`, `/public/rutas/{id}`, `/public/salidas`, `/public/terminales` | `?rutaId=&origenId=` |

### Seguridad (JWT + Spring Security)

| Concepto | Detalle |
|---|---|
| Autenticación | JWT HS256 con 24h de expiración |
| Passwords | BCrypt con `PasswordEncoder` |
| Sesiones | Stateless (no se usa HttpSession) |
| Endpoints públicos | `/api/v1/public/**` y `/api/v1/auth/**` — sin token |
| Endpoints protegidos | Todo lo demás — requieren `Authorization: Bearer <token>` |
| Admin inicial | Se crea automáticamente al arrancar si no existe: `admin:admin123` |
| Roles | `CENTRAL_ADMIN` — acceso completo. `TERMINAL_ADMIN` — restringido a su terminal |
| Errores | 401 si falta token, 403 si el rol no tiene permiso |

La configuración de rutas públicas vs protegidas está en `config/SecurityConfig.java`, en el método `securityFilterChain()`. El filtro JWT (`JwtAuthFilter`) omite explícitamente las rutas `/api/v1/public/` y `/api/v1/auth/` para no procesar tokens innecesariamente.

### CORS

`@CrossOrigin(origins = "*")` en todos los controladores. Ajustar en producción.

### Base de datos

- Host: `localhost:3306`
- Database: `terminal`
- User: `root`
- DDL: `spring.jpa.hibernate.ddl-auto=update` (Hibernate gestiona el schema)

### Convenciones de código

- **Constructor injection** con `@RequiredArgsConstructor` + campos `private final`
- Servicios con `@Service`, `@Transactional`, `@Slf4j`
- Controladores con `@RestController`, `@RequestMapping`, `ResponseEntity`
- DTOs de request con `jakarta.validation` (@NotBlank, @NotNull, @Size, etc.)
- DTOs de response con método estático `fromEntity(Entity)`
- `@Valid` en `@RequestBody` de los endpoints POST/PUT
- Manejo global de errores con `@ControllerAdvice`
- Excepciones unchecked: `ResourceNotFoundException` → 404, validación → 400, integridad → 409
- **Documentación**: toda clase pública debe tener Javadoc explicando su propósito. Todo método público debe tener `@param` y `@return`. Las validaciones `@NotBlank` deben incluir `message=`. El código de seguridad (filtros, config) debe tener comentarios inline explicando cada bloque de reglas.
