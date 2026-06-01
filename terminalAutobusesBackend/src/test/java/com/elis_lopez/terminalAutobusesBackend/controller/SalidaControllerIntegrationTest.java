package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.response.LoginResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para los endpoints de salidas ({@code /api/v1/salidas/**}).
 * <p>
 * Verifica los endpoints CRUD existentes y los nuevos: generación automática,
 * asignación de autobús y ajuste de retraso con propagación.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("SalidaController — Pruebas de integración")
class SalidaControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;

    @BeforeAll
    void setUp() {
        // Login como admin CENTRAL_ADMIN
        adminToken = loginAndGetToken("admin", "admin123");
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/v1/salidas/generar-del-dia
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/salidas/generar-del-dia")
    class GenerarDelDiaTests {

        @Test
        @DisplayName("Requiere autenticación — 401 sin token")
        void requiresAuth() {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/salidas/generar-del-dia", null, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Admin autenticado puede invocar generación — 200")
        void adminCanGenerate() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/salidas/generar-del-dia",
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode body = parseBody(response);
            assertThat(body.has("salidasGeneradas")).isTrue();
            assertThat(body.has("horariosProcesados")).isTrue();
            assertThat(body.has("horariosSaltados")).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PUT /api/v1/salidas/{id}/asignar-autobus
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/salidas/{id}/asignar-autobus")
    class AsignarAutobusTests {

        @Test
        @DisplayName("Requiere autenticación — 401 sin token")
        void requiresAuth() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/salidas/1/asignar-autobus",
                    HttpMethod.PUT,
                    new HttpEntity<>(Map.of("autobusId", 1), headers),
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Autobús inexistente — 404")
        void nonExistentAutobus() {
            ResponseEntity<String> response = putWithAuth(
                    "/api/v1/salidas/99999/asignar-autobus",
                    Map.of("autobusId", 99999),
                    adminToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PUT /api/v1/salidas/{id}/ajustar-retraso
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/salidas/{id}/ajustar-retraso")
    class AjustarRetrasoTests {

        @Test
        @DisplayName("Requiere autenticación — 401 sin token")
        void requiresAuth() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/salidas/1/ajustar-retraso",
                    HttpMethod.PUT,
                    new HttpEntity<>(Map.of("retrasoMinutos", 15), headers),
                    String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Salida inexistente — 404")
        void nonExistentSalida() {
            ResponseEntity<String> response = putWithAuth(
                    "/api/v1/salidas/99999/ajustar-retraso",
                    Map.of("retrasoMinutos", 15),
                    adminToken);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES
    // ═══════════════════════════════════════════════════════════

    private String loginAndGetToken(String username, String password) {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                Map.of("username", username, "password", password),
                LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        return response.getBody().getToken();
    }

    private ResponseEntity<String> putWithAuth(String url, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode parseBody(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error parseando JSON de respuesta", e);
        }
    }
}
