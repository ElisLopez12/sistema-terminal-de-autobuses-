# 🚌 Sistema de Gestión de Terminales de Autobuses

Sistema web full-stack para la gestión de terminales de autobuses, rutas, horarios, salidas, choferes y colectores.

## Stack

| Capa | Tecnología |
|---|---|
| Frontend | React 19 + Vite 8 + Tailwind CSS v4 |
| Backend | Spring Boot 4.0.6 + Java 21 |
| Base de datos | MySQL 8 |
| Autenticación | JWT (stateless) |
| Contenedores | Docker + Docker Compose |

## Cómo ejecutar

```bash
docker compose up -d
```

| Servicio | URL |
|---|---|
| Página pública | http://localhost:3000 |
| Panel admin | http://localhost:3000/login |
| API | http://localhost:8080/api/v1 |

**Usuario inicial:** `admin` / `admin123`

## Funcionalidades

- Gestión de terminales, rutas, autobuses, choferes, colectores, horarios y salidas
- Generación automática de salidas diarias a partir de horarios plantilla
- Control de acceso por roles: `CENTRAL_ADMIN` (todo el sistema) y `TERMINAL_ADMIN` (restringido a su terminal)
- Soft delete en todas las entidades
- API REST con paginación y filtros
- Página pública de consulta de rutas y salidas

## Desarrollo

```bash
# Backend
cd terminalAutobusesBackend && ./mvnw spring-boot:run

# Frontend
cd terminalAutobusesFrontend && pnpm dev
```
