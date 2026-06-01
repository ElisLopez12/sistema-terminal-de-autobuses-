package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.request.LoginRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.LoginResponse;
import com.elis_lopez.terminalAutobusesBackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la autenticación de usuarios.
 * <p>
 * Expone el endpoint {@code POST /api/v1/auth/login} para iniciar sesión
 * y obtener un token JWT.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Autentica un usuario con sus credenciales y devuelve un token JWT.
     *
     * @param request credenciales del usuario (username y password)
     * @return 200 OK con el token y datos del usuario, o 401 si las credenciales son inválidas
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
