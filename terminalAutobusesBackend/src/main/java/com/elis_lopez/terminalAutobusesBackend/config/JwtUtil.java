package com.elis_lopez.terminalAutobusesBackend.config;

import com.elis_lopez.terminalAutobusesBackend.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilidad para la generación y validación de tokens JWT.
 * <p>
 * Utiliza el algoritmo HS256 con una clave secreta configurada externamente.
 */
@Component
public class JwtUtil {

    private static final long EXPIRATION_MS = 86_400_000; // 24 horas

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un token JWT para el usuario dado.
     * <p>
     * Incluye claims: subject (userId), username, rol y terminalId (nullable).
     *
     * @param user usuario autenticado
     * @return token JWT firmado
     */
    public String generateToken(Usuario user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("rol", user.getRol().name())
                .claim("terminalId", user.getTerminal() != null ? user.getTerminal().getId() : null)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida un token JWT verificando su firma y vigencia.
     *
     * @param token token JWT a validar
     * @return {@code true} si el token es válido, {@code false} en caso contrario
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrae el ID de usuario del token JWT.
     *
     * @param token token JWT
     * @return ID del usuario (subject)
     */
    public Long extractUserId(String token) {
        return Long.parseLong(extractClaims(token).getSubject());
    }

    /**
     * Extrae el rol del usuario del token JWT.
     *
     * @param token token JWT
     * @return nombre del rol
     */
    public String extractRol(String token) {
        return extractClaims(token).get("rol", String.class);
    }

    /**
     * Extrae el ID del terminal asociado al usuario desde el token JWT.
     * <p>
     * El {@code terminalId} puede ser nulo para usuarios {@code CENTRAL_ADMIN}
     * que no están vinculados a ningún terminal.
     *
     * @param token token JWT
     * @return ID del terminal, o {@code null} si el usuario no tiene terminal
     */
    public Long extractTerminalId(String token) {
        return extractClaims(token).get("terminalId", Long.class);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
