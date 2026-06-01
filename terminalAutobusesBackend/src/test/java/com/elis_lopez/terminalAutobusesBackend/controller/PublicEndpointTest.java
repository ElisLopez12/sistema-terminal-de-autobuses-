package com.elis_lopez.terminalAutobusesBackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para los endpoints públicos ({@code /api/v1/public/**}).
 * <p>
 * Verifica que estos endpoints sean accesibles sin autenticación
 * y devuelvan respuestas correctas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@DisplayName("Endpoints Públicos — Acceso sin autenticación")
class PublicEndpointTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/v1/public/terminales → 200 (sin token)")
    void getTerminales_returns200WithoutAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/public/terminales", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /api/v1/public/rutas → 200 (sin token)")
    void getRutas_returns200WithoutAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/public/rutas", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /api/v1/public/salidas → 200 (sin token)")
    void getSalidas_returns200WithoutAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/public/salidas", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /api/v1/public/salidas → solo retorna PROGRAMADA y ABORDAJE")
    void getSalidas_filtersByProgramadaAndEnCurso() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/public/salidas", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            JsonNode body = objectMapper.readTree(response.getBody());
            if (body.has("content") && body.get("content").isArray()) {
                for (JsonNode salida : body.get("content")) {
                    String estado = salida.get("estado").asText();
                    assertThat(estado)
                            .as("Salida pública debe ser PROGRAMADA o ABORDAJE, pero fue: %s", estado)
                            .isIn("PROGRAMADA", "ABORDAJE");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parseando respuesta", e);
        }
    }

    @Test
    @DisplayName("GET /api/v1/public/rutas/{id} con ID inexistente → 404 (sin token)")
    void getRutaById_withNonExistentId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/public/rutas/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
