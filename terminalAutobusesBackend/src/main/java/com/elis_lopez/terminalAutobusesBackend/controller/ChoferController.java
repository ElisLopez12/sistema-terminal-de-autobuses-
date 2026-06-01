package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.request.ChoferRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.ChoferResponse;
import com.elis_lopez.terminalAutobusesBackend.service.ChoferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de choferes.
 * <p>
 * Expone operaciones CRUD sobre el recurso {@code /api/v1/choferes},
 * con filtro opcional por terminal.
 */
@RestController
@RequestMapping("/api/v1/choferes")
@RequiredArgsConstructor
public class ChoferController {

    private final ChoferService choferService;

    /**
     * Obtiene todos los choferes, opcionalmente filtrados por terminal.
     *
     * @param terminalId filtro opcional por terminal
     * @param pageable   parámetros de paginación y ordenación
     * @return 200 OK con la página de choferes
     */
    @GetMapping
    public ResponseEntity<Page<ChoferResponse>> findAll(
            @RequestParam(required = false) Long terminalId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        if (terminalId != null) {
            return ResponseEntity.ok(choferService.findByTerminalId(terminalId, pageable));
        }
        return ResponseEntity.ok(choferService.findAll(pageable));
    }

    /**
     * Obtiene un chofer por su ID.
     *
     * @param id identificador del chofer
     * @return 200 OK con el chofer, o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChoferResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(choferService.findById(id));
    }

    /**
     * Crea un nuevo chofer.
     *
     * @param request datos del chofer a crear
     * @return 201 Created con el chofer creado
     */
    @PostMapping
    public ResponseEntity<ChoferResponse> create(@Valid @RequestBody ChoferRequest request) {
        ChoferResponse response = choferService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza un chofer existente.
     *
     * @param id      identificador del chofer a actualizar
     * @param request nuevos datos del chofer
     * @return 200 OK con el chofer actualizado, o 404 si no existe
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChoferResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ChoferRequest request) {
        return ResponseEntity.ok(choferService.update(id, request));
    }

    /**
     * Desactiva (soft delete) un chofer.
     *
     * @param id identificador del chofer a desactivar
     * @return 204 No Content, o 404 si no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        choferService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
