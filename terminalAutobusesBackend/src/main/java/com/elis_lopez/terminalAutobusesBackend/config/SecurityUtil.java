package com.elis_lopez.terminalAutobusesBackend.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utilidad para obtener información del usuario autenticado desde el
 * {@link SecurityContextHolder}.
 * <p>
 * Proporciona métodos estáticos para acceder al ID de usuario, rol,
 * terminalId y verificaciones de rol ({@code CENTRAL_ADMIN} / {@code TERMINAL_ADMIN})
 * desde cualquier componente de la aplicación.
 */
@Component
public class SecurityUtil {

    private SecurityUtil() {
        // Clase de utilidad — no se instancia
    }

    /**
     * Obtiene el ID del usuario autenticado desde el contexto de seguridad.
     *
     * @return ID del usuario, o {@code null} si no hay autenticación
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }

    /**
     * Obtiene el rol del usuario autenticado desde el contexto de seguridad.
     *
     * @return nombre del rol (ej. "CENTRAL_ADMIN"), o {@code null} si no hay autenticación
     */
    public static String getCurrentUserRol() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    /**
     * Obtiene el ID del terminal al que pertenece el usuario autenticado,
     * extrayéndolo del mapa de detalles de la autenticación.
     * <p>
     * El {@code terminalId} se inyecta en los detalles durante el filtro JWT
     * ({@link JwtAuthFilter}) a partir del claim del token.
     *
     * @return ID del terminal, o {@code null} si el usuario no tiene terminal asociado
     */
    public static Long getCurrentUserTerminalId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object details = auth.getDetails();
        if (details instanceof Map<?, ?> map) {
            Object terminalId = map.get("terminalId");
            if (terminalId instanceof Long) {
                return (Long) terminalId;
            }
        }
        return null;
    }

    /**
     * Verifica si el usuario autenticado tiene el rol {@code CENTRAL_ADMIN}.
     *
     * @return {@code true} si el usuario es administrador central
     */
    public static boolean isCentralAdmin() {
        String rol = getCurrentUserRol();
        return "CENTRAL_ADMIN".equals(rol);
    }

    /**
     * Verifica si el usuario autenticado tiene el rol {@code TERMINAL_ADMIN}.
     *
     * @return {@code true} si el usuario es administrador de terminal
     */
    public static boolean isTerminalAdmin() {
        String rol = getCurrentUserRol();
        return "TERMINAL_ADMIN".equals(rol);
    }
}
