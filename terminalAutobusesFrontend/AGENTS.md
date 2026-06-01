# Terminal de Autobuses — Frontend (React + Vite)

## Backend API

La API del backend está documentada en `../terminalAutobusesBackend/API.md`. Leer ese archivo **antes** de hacer cualquier petición.

Resumen rápido:

- **Base URL**: `http://localhost:8080/api/v1`
- **Auth**: `POST /api/v1/auth/login` con `{ username, password }` → devuelve JWT
- **Headers**: todas las requests a endpoints protegidos llevan `Authorization: Bearer <token>`
- **Paginación**: todos los listados aceptan `?page=0&size=20&sort=id,asc`
- **Admin inicial**: `admin` / `admin123` (rol `CENTRAL_ADMIN`)

## Roles

- `CENTRAL_ADMIN` — acceso completo (crear terminales, admins de terminal, etc.)
- `TERMINAL_ADMIN` — solo ve/gestiona recursos de SU terminal

## Stack del frontend

- React + Vite + JavaScript
- Para peticiones HTTP: axios
- Para manejo de estado: useState / useContext + AuthContext (JWT en localStorage)
- Para estilos: Tailwind CSS v4
- Para autenticación: JWT via axios interceptor; AuthContext con persistencia en localStorage

## Desarrollo

```bash
pnpm dev        # http://localhost:5173
pnpm build      # compila a dist/
```

Asegurarse de que el backend esté corriendo (`./mvnw spring-boot:run` en la carpeta del backend).

## Responsive design

- Todas las vistas deben ser completamente responsive (mobile-first con Tailwind).
- El sidebar del admin se oculta en mobile y se abre como overlay con un botón de hamburguesa.
- Las tablas usan `overflow-x-auto` en mobile para scroll horizontal cuando sea necesario.
