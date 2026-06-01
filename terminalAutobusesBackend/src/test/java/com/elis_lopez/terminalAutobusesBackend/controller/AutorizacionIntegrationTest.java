package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.request.AutobusRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.request.LoginRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.request.TerminalRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.request.UsuarioRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.LoginResponse;
import com.elis_lopez.terminalAutobusesBackend.model.enums.RolUsuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración que verifican las reglas de autorización
 * para los roles {@code CENTRAL_ADMIN} y {@code TERMINAL_ADMIN}.
 * <p>
 * Escenarios probados:
 * <ul>
 *   <li>Terminales: CRUD completo para CENTRAL_ADMIN; solo lectura para TERMINAL_ADMIN</li>
 *   <li>Autobuses: CENTRAL_ADMIN ve todos; TERMINAL_ADMIN solo los de su terminal</li>
 *   <li>Usuarios: CENTRAL_ADMIN crea cualquier rol; TERMINAL_ADMIN solo TERMINAL_ADMIN en su terminal</li>
 *   <li>Paginación: respuestas con content, totalElements, totalPages, number, size</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@DisplayName("Autorización — Pruebas de reglas por rol (CENTRAL_ADMIN / TERMINAL_ADMIN)")
class AutorizacionIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Tokens y datos semilla ──
    private String adminToken;
    private String terminalAdminToken;
    private Long terminalAId;
    private Long terminalBId;
    private Long terminalAdminUserId;

    // ── Constantes ──
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";
    private static final String TERMINAL_ADMIN_USER = "termadmin_a";
    private static final String TERMINAL_ADMIN_PASS = "password123";
    private static final String TERMINAL_A_NAME = "Terminal Alpha";
    private static final String TERMINAL_B_NAME = "Terminal Beta";

    // ═══════════════════════════════════════════════════════════
    // SETUP
    // ═══════════════════════════════════════════════════════════

    /**
     * Prepara los datos semilla antes de ejecutar las pruebas:
     * <ol>
     *   <li>Inicia sesión como admin (creado por {@code seedAdminUser})</li>
     *   <li>Crea Terminal A y Terminal B</li>
     *   <li>Crea un TERMINAL_ADMIN para Terminal A</li>
     *   <li>Inicia sesión como el TERMINAL_ADMIN creado</li>
     * </ol>
     */
    @BeforeAll
    void setUp() {
        // 1. Login como admin CENTRAL_ADMIN
        adminToken = loginAndGetToken(ADMIN_USER, ADMIN_PASS);

        // 2. Crear Terminal A
        terminalAId = createTerminal(TERMINAL_A_NAME, "Ubicación A");

        // 3. Crear Terminal B
        terminalBId = createTerminal(TERMINAL_B_NAME, "Ubicación B");

        // 4. Crear TERMINAL_ADMIN para Terminal A
        terminalAdminUserId = createTerminalAdminUser(TERMINAL_ADMIN_USER, TERMINAL_ADMIN_PASS, terminalAId);

        // 5. Login como TERMINAL_ADMIN
        terminalAdminToken = loginAndGetToken(TERMINAL_ADMIN_USER, TERMINAL_ADMIN_PASS);
    }

    // ═══════════════════════════════════════════════════════════
    // TERMINAL — CENTRAL_ADMIN
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("CENTRAL_ADMIN puede leer todos los terminales → 200")
    void centralAdmin_canReadAllTerminals() {
        ResponseEntity<String> response = getWithAuth("/api/v1/terminales", adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = parseBody(response);
        assertThat(body.get("totalElements").asInt()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("CENTRAL_ADMIN puede crear un terminal → 201")
    void centralAdmin_canCreateTerminal() {
        TerminalRequest request = new TerminalRequest();
        request.setNombre("Terminal Temp");
        request.setUbicacion("Temp");

        ResponseEntity<String> response = postWithAuth("/api/v1/terminales", request, adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = parseBody(response);
        assertThat(body.get("nombre").asText()).isEqualTo("Terminal Temp");
    }

    @Test
    @DisplayName("CENTRAL_ADMIN puede actualizar un terminal → 200")
    void centralAdmin_canUpdateTerminal() {
        TerminalRequest request = new TerminalRequest();
        request.setNombre("Terminal Alpha Actualizado");
        request.setUbicacion("Nueva Ubicación");

        ResponseEntity<String> response = putWithAuth("/api/v1/terminales/" + terminalAId, request, adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = parseBody(response);
        assertThat(body.get("nombre").asText()).isEqualTo("Terminal Alpha Actualizado");
    }

    @Test
    @DisplayName("CENTRAL_ADMIN puede eliminar (soft delete) un terminal → 204")
    void centralAdmin_canDeleteTerminal() {
        // Crear un terminal temporal para eliminar
        Long tempId = createTerminal("Temp Delete", "X");

        ResponseEntity<Void> response = deleteWithAuth("/api/v1/terminales/" + tempId, adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ═══════════════════════════════════════════════════════════
    // TERMINAL — TERMINAL_ADMIN
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("TERMINAL_ADMIN puede leer su terminal → 200")
    void terminalAdmin_canReadOwnTerminal() {
        ResponseEntity<String> response = getWithAuth("/api/v1/terminales/" + terminalAId, terminalAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = parseBody(response);
        assertThat(body.get("nombre").asText()).isEqualTo(TERMINAL_A_NAME);
    }

    @Test
    @DisplayName("TERMINAL_ADMIN no puede crear un terminal → 403")
    void terminalAdmin_cannotCreateTerminal() {
        TerminalRequest request = new TerminalRequest();
        request.setNombre("Intento Creación");
        request.setUbicacion("X");

        ResponseEntity<String> response = postWithAuth("/api/v1/terminales", request, terminalAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("TERMINAL_ADMIN no puede actualizar otro terminal → 403")
    void terminalAdmin_cannotUpdateOtherTerminal() {
        TerminalRequest request = new TerminalRequest();
        request.setNombre("Hackeado");
        request.setUbicacion("Hack");

        ResponseEntity<String> response = putWithAuth("/api/v1/terminales/" + terminalBId, request, terminalAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("TERMINAL_ADMIN no puede eliminar un terminal → 403")
    void terminalAdmin_cannotDeleteTerminal() {
        ResponseEntity<Void> response = deleteWithAuth("/api/v1/terminales/" + terminalBId, terminalAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ═══════════════════════════════════════════════════════════
    // AUTOBUS — CENTRAL_ADMIN
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("CENTRAL_ADMIN puede crear un autobús en cualquier terminal → 201")
    void centralAdmin_canCreateAutobusInAnyTerminal() {
        AutobusRequest request = buildAutobusRequest(terminalAId, "CA-001", "ABC-123");

        ResponseEntity<String> response = postWithAuth("/api/v1/autobuses", request, adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = parseBody(response);
        assertThat(body.get("numeroUnidad").asText()).isEqualTo("CA-001");
        assertThat(body.get("terminalId").asLong()).isEqualTo(terminalAId);
    }

    @Test
    @DisplayName("CENTRAL_ADMIN puede leer TODOS los autobuses → 200 (paginated)")
    void centralAdmin_canReadAllAutobuses() {
        // Crear autobuses en ambos terminales
        createAutobus(terminalAId, "ALL-001", "X1", adminToken);
        createAutobus(terminalBId, "ALL-002", "X2", adminToken);

        ResponseEntity<String> response = getWithAuth("/api/v1/autobuses", adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = parseBody(response);
        assertThat(body.get("totalElements").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(body.get("content").isArray()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════
    // AUTOBUS — TERMINAL_ADMIN
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("TERMINAL_ADMIN puede crear autobús — fuerza su terminal aunque envíe otro → 201")
    void terminalAdmin_createsAutobus_forcedToOwnTerminal() {
        // Intenta crear un autobús para Terminal B, pero el sistema lo fuerza a Terminal A
        AutobusRequest request = buildAutobusRequest(terminalBId, "TA-001", "FORCE-001");

        ResponseEntity<String> response = postWithAuth("/api/v1/autobuses", request, terminalAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = parseBody(response);
        assertThat(body.get("numeroUnidad").asText()).isEqualTo("TA-001");
        // El terminal debe ser A (el suyo), no B
        assertThat(body.get("terminalId").asLong()).isEqualTo(terminalAId);
    }

    @Test
    @DisplayName("TERMINAL_ADMIN solo ve autobuses de su terminal → 200")
    void terminalAdmin_onlySeesOwnTerminalAutobuses() {
        // Crear un autobús en Terminal A (admin lo ve) y otro en Terminal B (no lo ve)
        createAutobus(terminalAId, "OWN-001", "OWN-1", adminToken);
        createAutobus(terminalBId, "OWN-002", "OWN-2", adminToken);

        ResponseEntity<String> response = getWithAuth("/api/v1/autobuses", terminalAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = parseBody(response);
        // Solo debe ver los de Terminal A
        for (JsonNode bus : body.get("content")) {
            assertThat(bus.get("terminalId").asLong()).isEqualTo(terminalAId);
        }
    }

    @Test
    @DisplayName("TERMINAL_ADMIN no puede actualizar autobús de otro terminal → 403")
    void terminalAdmin_cannotUpdateOtherTerminalAutobus() {
        // Crear autobús en Terminal B como admin
        Long busBId = createAutobus(terminalBId, "OTH-001", "OTH-1", adminToken);

        AutobusRequest updateRequest = buildAutobusRequest(terminalBId, "OTH-001-UPD", "OTH-UPD");

        ResponseEntity<String> response = putWithAuth(
                "/api/v1/autobuses/" + busBId, updateRequest, terminalAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ═══════════════════════════════════════════════════════════
    // USUARIO
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("CENTRAL_ADMIN puede crear usuario con cualquier rol → 201")
    void centralAdmin_canCreateUserWithAnyRole() {
        UsuarioRequest request = new UsuarioRequest();
        request.setUsername("new_central_" + System.currentTimeMillis());
        request.setPassword("password123");
        request.setRol(RolUsuario.CENTRAL_ADMIN);
        request.setTerminalId(null);

        ResponseEntity<String> response = postWithAuth("/api/v1/usuarios", request, adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = parseBody(response);
        assertThat(body.get("rol").asText()).isEqualTo("CENTRAL_ADMIN");
    }

    @Test
    @DisplayName("TERMINAL_ADMIN no puede crear un CENTRAL_ADMIN → 403")
    void terminalAdmin_cannotCreateCentralAdmin() {
        UsuarioRequest request = new UsuarioRequest();
        request.setUsername("hacker_" + System.currentTimeMillis());
        request.setPassword("password123");
        request.setRol(RolUsuario.CENTRAL_ADMIN);
        request.setTerminalId(terminalAId);

        ResponseEntity<String> response = postWithAuth("/api/v1/usuarios", request, terminalAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("TERMINAL_ADMIN puede crear TERMINAL_ADMIN para su terminal → 201")
    void terminalAdmin_canCreateTerminalAdminForOwnTerminal() {
        UsuarioRequest request = new UsuarioRequest();
        request.setUsername("term_sub_" + System.currentTimeMillis());
        request.setPassword("password123");
        request.setRol(RolUsuario.TERMINAL_ADMIN);
        request.setTerminalId(terminalAId);

        ResponseEntity<String> response = postWithAuth("/api/v1/usuarios", request, terminalAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = parseBody(response);
        assertThat(body.get("rol").asText()).isEqualTo("TERMINAL_ADMIN");
        assertThat(body.get("terminalId").asLong()).isEqualTo(terminalAId);
    }

    // ═══════════════════════════════════════════════════════════
    // PAGINACIÓN
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/v1/autobuses?page=0&size=1 retorna respuesta paginada")
    void autobusList_returnsPaginatedResponse() {
        // Crear suficientes autobuses para probar paginación
        createAutobus(terminalAId, "PAG-001", "PAG-1", adminToken);
        createAutobus(terminalAId, "PAG-002", "PAG-2", adminToken);
        createAutobus(terminalAId, "PAG-003", "PAG-3", adminToken);

        ResponseEntity<String> response = getWithAuth(
                "/api/v1/autobuses?page=0&size=1", adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = parseBody(response);

        assertThat(body.has("content")).isTrue();
        assertThat(body.get("content").isArray()).isTrue();
        assertThat(body.get("content").size()).isLessThanOrEqualTo(1);
        assertThat(body.has("totalElements")).isTrue();
        assertThat(body.get("totalElements").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(body.has("totalPages")).isTrue();
        assertThat(body.get("totalPages").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(body.has("number")).isTrue();
        assertThat(body.get("number").asInt()).isEqualTo(0);
        assertThat(body.has("size")).isTrue();
        assertThat(body.get("size").asInt()).isEqualTo(1);
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES
    // ═══════════════════════════════════════════════════════════

    /**
     * Inicia sesión con las credenciales dadas y devuelve el token JWT.
     */
    private String loginAndGetToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();

        return response.getBody().getToken();
    }

    /**
     * Crea un terminal y devuelve su ID.
     */
    private Long createTerminal(String nombre, String ubicacion) {
        TerminalRequest request = new TerminalRequest();
        request.setNombre(nombre);
        request.setUbicacion(ubicacion);

        ResponseEntity<String> response = postWithAuth("/api/v1/terminales", request, adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = parseBody(response);
        return body.get("id").asLong();
    }

    /**
     * Crea un usuario TERMINAL_ADMIN y devuelve su ID.
     */
    private Long createTerminalAdminUser(String username, String password, Long terminalId) {
        UsuarioRequest request = new UsuarioRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRol(RolUsuario.TERMINAL_ADMIN);
        request.setTerminalId(terminalId);

        ResponseEntity<String> response = postWithAuth("/api/v1/usuarios", request, adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = parseBody(response);
        return body.get("id").asLong();
    }

    /**
     * Construye un {@link AutobusRequest} con los datos dados.
     */
    private AutobusRequest buildAutobusRequest(Long terminalId, String numeroUnidad, String matricula) {
        AutobusRequest request = new AutobusRequest();
        request.setNumeroUnidad(numeroUnidad);
        request.setMatricula(matricula);
        request.setMarca("Test");
        request.setModelo("Test Model");
        request.setAnio(2024);
        request.setCapacidadPasajeros(40);
        request.setTerminalId(terminalId);
        return request;
    }

    /**
     * Crea un autobús y devuelve su ID.
     */
    private Long createAutobus(Long terminalId, String numeroUnidad, String matricula, String token) {
        AutobusRequest request = buildAutobusRequest(terminalId, numeroUnidad, matricula);

        ResponseEntity<String> response = postWithAuth("/api/v1/autobuses", request, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = parseBody(response);
        return body.get("id").asLong();
    }

    // ── HTTP Helpers ───────────────────────────────────────────

    private ResponseEntity<String> getWithAuth(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> postWithAuth(String url, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> putWithAuth(String url, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<Void> deleteWithAuth(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);
    }

    /**
     * Parsea el cuerpo de la respuesta como {@link JsonNode}.
     */
    private JsonNode parseBody(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error parseando JSON de respuesta", e);
        }
    }
}
