package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.request.HorarioRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.HorarioResponse;
import com.elis_lopez.terminalAutobusesBackend.service.HorarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de horarios plantilla.
 * <p>
 * Expone operaciones CRUD sobre el recurso {@code /api/v1/horarios},
 * con filtros opcionales por ruta y terminal de origen.
 */
@RestController
@RequestMapping("/api/v1/horarios")
@RequiredArgsConstructor
public class HorarioController {

    private final HorarioService horarioService;

    /**
     * Obtiene todos los horarios, opcionalmente filtrados por ruta o terminal.
     *
     * @param rutaId     filtro opcional por ruta
     * @param terminalId filtro opcional por terminal de origen
     * @param pageable   parámetros de paginación y ordenación
     * @return 200 OK con la página de horarios
     */
    @GetMapping
    public ResponseEntity<Page<HorarioResponse>> findAll(
            @RequestParam(required = false) Long rutaId,
            @RequestParam(required = false) Long terminalId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        if (rutaId != null) {
            return ResponseEntity.ok(horarioService.findByRutaId(rutaId, pageable));
        }
        if (terminalId != null) {
            return ResponseEntity.ok(horarioService.findByTerminalOrigenId(terminalId, pageable));
        }
        return ResponseEntity.ok(horarioService.findAll(pageable));
    }

    /**
     * Obtiene un horario por su ID.
     *
     * @param id identificador del horario
     * @return 200 OK con el horario, o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<HorarioResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(horarioService.findById(id));
    }

    /**
     * Crea un nuevo horario plantilla.
     *
     * @param request datos del horario a crear
     * @return 201 Created con el horario creado
     */
    @PostMapping
    public ResponseEntity<List<HorarioResponse>> create(@Valid @RequestBody HorarioRequest request) {
        List<HorarioResponse> responses = horarioService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Actualiza un horario existente.
     *
     * @param id      identificador del horario a actualizar
     * @param request nuevos datos del horario
     * @return 200 OK con el horario actualizado, o 404 si no existe
     */
    @PutMapping("/{id}")
    public ResponseEntity<HorarioResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.ok(horarioService.update(id, request));
    }

    /**
     * Desactiva (soft delete) un horario.
     *
     * @param id identificador del horario a desactivar
     * @return 204 No Content, o 404 si no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        horarioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
