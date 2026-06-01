package com.elis_lopez.terminalAutobusesBackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filtro JWT que intercepta cada petición HTTP para validar el token
 * de autenticación presente en el header {@code Authorization: Bearer <token>}.
 * <p>
 * Omite los endpoints públicos ({@code /api/v1/public/**}) y de autenticación
 * ({@code /api/v1/auth/**}).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * Determina si este filtro debe omitirse para la request actual.
     * <p>
     * Las rutas públicas y de autenticación NO necesitan procesar tokens JWT,
     * por lo que el filtro las saltea completamente por performance y claridad.
     * <p>
     * Esto complementa la regla {@code .permitAll()} de SecurityConfig:
     * acá saltamos el filtro, en SecurityConfig permitimos el acceso sin auth.
     *
     * @param request petición HTTP entrante
     * @return {@code true} si el filtro debe omitirse
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/public/") || path.startsWith("/api/v1/auth/");
    }

    /**
     * Procesa cada request que NO fue omitida por {@link #shouldNotFilter}.
     * <p>
     * Flujo:
     * <ol>
     *   <li>Extrae el header {@code Authorization}</li>
     *   <li>Si no existe o no es {@code Bearer ...}, continúa la cadena sin autenticar</li>
     *   <li>Valida el token JWT (firma + expiración)</li>
     *   <li>Si es válido, extrae userId y rol, crea un {@code Authentication}
     *       y lo setea en el {@code SecurityContextHolder}</li>
     *   <li>Continúa la cadena de filtros</li>
     * </ol>
     * <p>
     * Si el token es inválido, simplemente NO se setea el contexto de seguridad.
     * Luego, Spring Security ve que no hay autenticación y devuelve 401.
     *
     * @param request     petición HTTP entrante
     * @param response    respuesta HTTP
     * @param filterChain cadena de filtros a continuar
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ── Paso 1: Extraer el header Authorization ──
        String header = request.getHeader("Authorization");

        // ── Paso 2: Si no hay header o no es Bearer, seguir sin autenticar ──
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Paso 3: Extraer el token (después de "Bearer ") ──
        String token = header.substring(7);

        // ── Paso 4: Validar el token y configurar autenticación ──
        if (jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.extractUserId(token);
            String rol = jwtUtil.extractRol(token);

            // Crea la lista de autoridades (roles) que Spring Security usará
            // para las validaciones de @PreAuthorize / hasRole()
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + rol)
            );

            // Extrae el terminalId del token (puede ser null para CENTRAL_ADMIN)
            Long terminalId = jwtUtil.extractTerminalId(token);

            // Crea un mapa con los detalles del usuario autenticado para que
            // SecurityUtil pueda acceder a ellos más tarde
            Map<String, Object> details = new HashMap<>();
            details.put("userId", userId);
            details.put("rol", rol);
            details.put("terminalId", terminalId);

            // Crea el token de autenticación con userId como principal,
            // sin credenciales (el token ya fue validado), y los roles
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // Guarda los detalles (userId, rol, terminalId) en la autenticación
            // para que SecurityUtil.getCurrentUserTerminalId() pueda leerlos
            authentication.setDetails(details);

            // Lo guarda en el contexto de seguridad para toda la request
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // Si el token NO es válido: no se setea SecurityContext → Spring Security
        // interpreta que no hay autenticación → authenticationEntryPoint → 401

        // ── Paso 5: Continuar la cadena (llegue o no al SecurityContext) ──
        filterChain.doFilter(request, response);
    }
}
