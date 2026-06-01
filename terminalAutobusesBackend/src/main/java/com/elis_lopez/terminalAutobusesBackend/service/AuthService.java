package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.config.JwtUtil;
import com.elis_lopez.terminalAutobusesBackend.dto.request.LoginRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.LoginResponse;
import com.elis_lopez.terminalAutobusesBackend.model.Usuario;
import com.elis_lopez.terminalAutobusesBackend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación de usuarios.
 * <p>
 * Valida las credenciales contra la base de datos y genera un token JWT
 * para las sesiones autenticadas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Autentica un usuario con username y contraseña.
     * <p>
     * Verifica que el usuario exista, esté activo y la contraseña coincida.
     * Si la autenticación es exitosa, genera y devuelve un token JWT.
     *
     * @param request credenciales de inicio de sesión
     * @return respuesta con token JWT y datos del usuario
     * @throws BadCredentialsException si el usuario no existe, está inactivo o la contraseña es incorrecta
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Intento de login con username inexistente: {}", request.getUsername());
                    return new BadCredentialsException("Credenciales inválidas");
                });

        if (!usuario.isActivo()) {
            log.warn("Intento de login de usuario desactivado: id={}", usuario.getId());
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("Contraseña incorrecta para usuario: id={}", usuario.getId());
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String token = jwtUtil.generateToken(usuario);
        log.info("Login exitoso: id={}, username={}", usuario.getId(), usuario.getUsername());

        return LoginResponse.from(usuario, token);
    }
}
