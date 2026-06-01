package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.config.JwtUtil;
import com.elis_lopez.terminalAutobusesBackend.dto.request.LoginRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.LoginResponse;
import com.elis_lopez.terminalAutobusesBackend.model.Usuario;
import com.elis_lopez.terminalAutobusesBackend.model.enums.RolUsuario;
import com.elis_lopez.terminalAutobusesBackend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de autenticación {@link AuthService}.
 * <p>
 * Verifica los escenarios de login: credenciales válidas, usuario inexistente,
 * contraseña incorrecta y usuario inactivo.
 * <p>
 * Se utiliza Mockito para simular las dependencias externas (repositorio,
 * codificador de contraseñas y generador JWT).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Pruebas unitarias de autenticación")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private Usuario activeUser;
    private Usuario inactiveUser;
    private LoginRequest validRequest;
    private LoginRequest invalidPasswordRequest;
    private LoginRequest invalidUsernameRequest;

    @BeforeEach
    void setUp() {
        activeUser = new Usuario();
        activeUser.setId(1L);
        activeUser.setUsername("testuser");
        activeUser.setPassword("encodedPassword");
        activeUser.setRol(RolUsuario.TERMINAL_ADMIN);
        activeUser.setActivo(true);

        inactiveUser = new Usuario();
        inactiveUser.setId(2L);
        inactiveUser.setUsername("inactiveuser");
        inactiveUser.setPassword("encodedPassword");
        inactiveUser.setRol(RolUsuario.TERMINAL_ADMIN);
        inactiveUser.setActivo(false);

        validRequest = new LoginRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("correctPassword");

        invalidPasswordRequest = new LoginRequest();
        invalidPasswordRequest.setUsername("testuser");
        invalidPasswordRequest.setPassword("wrongPassword");

        invalidUsernameRequest = new LoginRequest();
        invalidUsernameRequest.setUsername("nonexistent");
        invalidUsernameRequest.setPassword("anyPassword");
    }

    @Test
    @DisplayName("Credenciales válidas → retorna LoginResponse con token JWT")
    void validCredentials_returnsLoginResponseWithToken() {
        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(activeUser)).thenReturn("jwt-token-123");

        LoginResponse response = authService.login(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getRol()).isEqualTo("TERMINAL_ADMIN");

        verify(usuarioRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("correctPassword", "encodedPassword");
        verify(jwtUtil).generateToken(activeUser);
    }

    @Test
    @DisplayName("Username inexistente → lanza BadCredentialsException")
    void invalidUsername_throwsBadCredentialsException() {
        when(usuarioRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(invalidUsernameRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuarioRepository).findByUsername("nonexistent");
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }

    @Test
    @DisplayName("Contraseña incorrecta → lanza BadCredentialsException")
    void invalidPassword_throwsBadCredentialsException() {
        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(invalidPasswordRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("Usuario inactivo → lanza BadCredentialsException")
    void inactiveUser_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("inactiveuser");
        request.setPassword("anyPassword");

        when(usuarioRepository.findByUsername("inactiveuser")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuarioRepository).findByUsername("inactiveuser");
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }
}
