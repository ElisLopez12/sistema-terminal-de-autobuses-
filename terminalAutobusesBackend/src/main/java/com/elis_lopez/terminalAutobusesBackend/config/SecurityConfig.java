package com.elis_lopez.terminalAutobusesBackend.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.elis_lopez.terminalAutobusesBackend.model.enums.RolUsuario;
import com.elis_lopez.terminalAutobusesBackend.model.Usuario;
import com.elis_lopez.terminalAutobusesBackend.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

import java.util.List;

/**
 * Configuración de seguridad de la aplicación.
 * <p>
 * Define el filtro JWT, política de sesiones sin estado (stateless),
 * deshabilita CSRF, configura CORS y define las reglas de autorización
 * para los endpoints públicos y privados.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UsuarioRepository usuarioRepository;

    /**
     * Configura la cadena de filtros de seguridad HTTP.
     * <p>
     * Define qué rutas son públicas y cuáles requieren autenticación,
     * configura CORS, deshabilita CSRF (API REST sin estado), y establece
     * el filtro JWT antes del filtro de autenticación por defecto.
     *
     * @param http builder de seguridad HTTP
     * @return cadena de filtros configurada
     * @throws Exception si ocurre un error en la configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ──────────────────────────────────────────────
                // CORS: permite peticiones desde cualquier origen
                // (ajustar en producción a orígenes específicos)
                // ──────────────────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ──────────────────────────────────────────────
                // CSRF: deshabilitado porque usamos JWT stateless.
                // Las APIs REST no necesitan protección CSRF
                // cuando no usan cookies de sesión.
                // ──────────────────────────────────────────────
                .csrf(csrf -> csrf.disable())

                // ──────────────────────────────────────────────
                // Sesiones STATELESS: Spring Security NO crea
                // HttpSession. Cada request lleva su propio token
                // JWT, no hay estado en el servidor.
                // ──────────────────────────────────────────────
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ──────────────────────────────────────────────
                // REGLAS DE ACCESO (se evalúan EN ORDEN)
                // ──────────────────────────────────────────────
                // 1. /api/v1/public/** → público, sin token
                //    Ej: GET /api/v1/public/rutas, /public/terminales
                // 2. /api/v1/auth/**   → público, sin token
                //    Ej: POST /api/v1/auth/login
                // 3. /h2-console/**    → público (debug, solo desarrollo)
                // 4. CUALQUIER OTRA RUTA → requiere token JWT
                // ──────────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )

                // ──────────────────────────────────────────────
                // Manejo de errores de autenticación/autorización
                // ──────────────────────────────────────────────
                .exceptionHandling(ex -> ex
                        // 401 — No hay token o el token es inválido
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(
                                    "{\"timestamp\":\"" + java.time.LocalDateTime.now() + "\","
                                            + "\"status\":401,"
                                            + "\"error\":\"Unauthorized\","
                                            + "\"message\":\"Token de autenticación requerido\"}"
                            );
                        })
                        // 403 — Token válido pero el rol no tiene permiso
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write(
                                    "{\"timestamp\":\"" + java.time.LocalDateTime.now() + "\","
                                            + "\"status\":403,"
                                            + "\"error\":\"Forbidden\","
                                            + "\"message\":\"No tienes permiso para acceder a este recurso.\"}"
                            );
                        })
                )

                // ──────────────────────────────────────────────
                // Headers: necesario para que el H2 Console
                // funcione con frames (solo desarrollo)
                // ──────────────────────────────────────────────
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // ──────────────────────────────────────────────
                // Filtro JWT: se ejecuta ANTES del filtro de
                // autenticación por defecto. Extrae el token del
                // header Authorization y configura el contexto
                // de seguridad si es válido.
                // ──────────────────────────────────────────────
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Bean para la codificación de contraseñas con BCrypt.
     *
     * @return codificador BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean que expone el administrador de autenticación de Spring Security.
     *
     * @param authConfig configuración de autenticación
     * @return administrador de autenticación
     * @throws Exception si ocurre un error al obtener el administrador
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Inicializa un usuario administrador por defecto si no existe ninguno.
     * <p>
     * Esto permite el primer ingreso al sistema. La contraseña debe cambiarse
     * después del primer inicio de sesión.
     *
     * @return runner que crea el admin inicial
     */
    @Bean
    public CommandLineRunner seedAdminUser(PasswordEncoder encoder) {
        return args -> {
            Usuario admin = usuarioRepository.findByUsername("admin").orElse(null);
            if (admin == null) {
                admin = new Usuario();
                admin.setUsername("admin");
                admin.setActivo(true);
                log.info("ADMIN INICIAL CREADO — usuario: admin / contraseña: admin123");
            } else {
                log.info("ADMIN EXISTENTE — se actualiza contraseña");
            }
            admin.setPassword(encoder.encode("admin123"));
            admin.setRol(RolUsuario.CENTRAL_ADMIN);
            usuarioRepository.save(admin);
        };
    }

    /**
     * Configuración CORS global que permite cualquier origen.
     *
     * @return fuente de configuración CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
