package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.request.ColectorRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.ColectorResponse;
import com.elis_lopez.terminalAutobusesBackend.service.ColectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de colectores.
 * <p>
 * Expone operaciones CRUD sobre el recurso {@code /api/v1/colectores},
 * con filtro opcional por terminal.
 */
@RestController
@RequestMapping("/api/v1/colectores")
@RequiredArgsConstructor
public class ColectorController {

    private final ColectorService colectorService;

    /**
     * Obtiene todos los colectores, opcionalmente filtrados por terminal.
     *
     * @param terminalId filtro opcional por terminal
     * @param pageable   parámetros de paginación y ordenación
     * @return 200 OK con la página de colectores
     */
    @GetMapping
    public ResponseEntity<Page<ColectorResponse>> findAll(
            @RequestParam(required = false) Long terminalId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        if (terminalId != null) {
            return ResponseEntity.ok(colectorService.findByTerminalId(terminalId, pageable));
        }
        return ResponseEntity.ok(colectorService.findAll(pageable));
    }

    /**
     * Obtiene un colector por su ID.
     *
     * @param id identificador del colector
     * @return 200 OK con el colector, o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<ColectorResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(colectorService.findById(id));
    }

    /**
     * Crea un nuevo colector.
     *
     * @param request datos del colector a crear
     * @return 201 Created con el colector creado
     */
    @PostMapping
    public ResponseEntity<ColectorResponse> create(@Valid @RequestBody ColectorRequest request) {
        ColectorResponse response = colectorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza un colector existente.
     *
     * @param id      identificador del colector a actualizar
     * @param request nuevos datos del colector
     * @return 200 OK con el colector actualizado, o 404 si no existe
     */
    @PutMapping("/{id}")
    public ResponseEntity<ColectorResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody ColectorRequest request) {
        return ResponseEntity.ok(colectorService.update(id, request));
    }

    /**
     * Desactiva (soft delete) un colector.
     *
     * @param id identificador del colector a desactivar
     * @return 204 No Content, o 404 si no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        colectorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
