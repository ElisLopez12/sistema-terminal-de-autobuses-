package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.request.TerminalRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.TerminalResponse;
import com.elis_lopez.terminalAutobusesBackend.service.TerminalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de terminales de autobuses.
 * <p>
 * Expone operaciones CRUD sobre el recurso {@code /api/v1/terminales}.
 */
@RestController
@RequestMapping("/api/v1/terminales")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalService terminalService;

    /**
     * Obtiene todos los terminales.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return 200 OK con la página de terminales
     */
    @GetMapping
    public ResponseEntity<Page<TerminalResponse>> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(terminalService.findAll(pageable));
    }

    /**
     * Obtiene un terminal por su ID.
     *
     * @param id identificador del terminal
     * @return 200 OK con el terminal, o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<TerminalResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(terminalService.findById(id));
    }

    /**
     * Crea un nuevo terminal.
     *
     * @param request datos del terminal a crear
     * @return 201 Created con el terminal creado
     */
    @PostMapping
    public ResponseEntity<TerminalResponse> create(@Valid @RequestBody TerminalRequest request) {
        TerminalResponse response = terminalService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza un terminal existente.
     *
     * @param id      identificador del terminal a actualizar
     * @param request nuevos datos del terminal
     * @return 200 OK con el terminal actualizado, o 404 si no existe
     */
    @PutMapping("/{id}")
    public ResponseEntity<TerminalResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody TerminalRequest request) {
        return ResponseEntity.ok(terminalService.update(id, request));
    }

    /**
     * Desactiva (soft delete) un terminal.
     *
     * @param id identificador del terminal a desactivar
     * @return 204 No Content, o 404 si no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        terminalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
