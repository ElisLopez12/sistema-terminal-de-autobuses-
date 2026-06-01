package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.request.AutobusRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.AutobusResponse;
import com.elis_lopez.terminalAutobusesBackend.service.AutobusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de autobuses.
 * <p>
 * Expone operaciones CRUD sobre el recurso {@code /api/v1/autobuses},
 * con filtro opcional por terminal.
 */
@RestController
@RequestMapping("/api/v1/autobuses")
@RequiredArgsConstructor
public class AutobusController {

    private final AutobusService autobusService;

    /**
     * Obtiene todos los autobuses, opcionalmente filtrados por terminal.
     *
     * @param terminalId filtro opcional por terminal
     * @param pageable   parámetros de paginación y ordenación
     * @return 200 OK con la página de autobuses
     */
    @GetMapping
    public ResponseEntity<Page<AutobusResponse>> findAll(
            @RequestParam(required = false) Long terminalId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        if (terminalId != null) {
            return ResponseEntity.ok(autobusService.findByTerminalId(terminalId, pageable));
        }
        return ResponseEntity.ok(autobusService.findAll(pageable));
    }

    /**
     * Obtiene un autobús por su ID.
     *
     * @param id identificador del autobús
     * @return 200 OK con el autobús, o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<AutobusResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(autobusService.findById(id));
    }

    /**
     * Crea un nuevo autobús.
     *
     * @param request datos del autobús a crear
     * @return 201 Created con el autobús creado
     */
    @PostMapping
    public ResponseEntity<AutobusResponse> create(@Valid @RequestBody AutobusRequest request) {
        AutobusResponse response = autobusService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza un autobús existente.
     *
     * @param id      identificador del autobús a actualizar
     * @param request nuevos datos del autobús
     * @return 200 OK con el autobús actualizado, o 404 si no existe
     */
    @PutMapping("/{id}")
    public ResponseEntity<AutobusResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody AutobusRequest request) {
        return ResponseEntity.ok(autobusService.update(id, request));
    }

    /**
     * Desactiva (soft delete) un autobús.
     *
     * @param id identificador del autobús a desactivar
     * @return 204 No Content, o 404 si no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        autobusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
