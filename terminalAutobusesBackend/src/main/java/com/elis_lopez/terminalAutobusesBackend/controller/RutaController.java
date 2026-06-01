package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.request.RutaRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.RutaResponse;
import com.elis_lopez.terminalAutobusesBackend.service.RutaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de rutas de autobuses.
 * <p>
 * Expone operaciones CRUD sobre el recurso {@code /api/v1/rutas}.
 */
@RestController
@RequestMapping("/api/v1/rutas")
@RequiredArgsConstructor
public class RutaController {

    private final RutaService rutaService;

    /**
     * Obtiene todas las rutas registradas.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return 200 OK con la página de rutas
     */
    @GetMapping
    public ResponseEntity<Page<RutaResponse>> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(rutaService.findAll(pageable));
    }

    /**
     * Obtiene una ruta por su ID.
     *
     * @param id identificador de la ruta
     * @return 200 OK con la ruta, o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<RutaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(rutaService.findById(id));
    }

    /**
     * Crea una nueva ruta.
     *
     * @param request datos de la ruta a crear
     * @return 201 Created con la ruta creada
     */
    @PostMapping
    public ResponseEntity<RutaResponse> create(@Valid @RequestBody RutaRequest request) {
        RutaResponse response = rutaService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza una ruta existente.
     *
     * @param id      identificador de la ruta a actualizar
     * @param request nuevos datos de la ruta
     * @return 200 OK con la ruta actualizada, o 404 si no existe
     */
    @PutMapping("/{id}")
    public ResponseEntity<RutaResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody RutaRequest request) {
        return ResponseEntity.ok(rutaService.update(id, request));
    }

    /**
     * Desactiva (soft delete) una ruta.
     *
     * @param id identificador de la ruta a desactivar
     * @return 204 No Content, o 404 si no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rutaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
