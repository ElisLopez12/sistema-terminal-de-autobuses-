package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.request.UsuarioRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.UsuarioResponse;
import com.elis_lopez.terminalAutobusesBackend.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de usuarios del sistema.
 * <p>
 * Expone operaciones CRUD sobre el recurso {@code /api/v1/usuarios}.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Obtiene todos los usuarios registrados.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return 200 OK con la página de usuarios
     */
    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(usuarioService.findAll(pageable));
    }

    /**
     * Obtiene un usuario por su ID.
     *
     * @param id identificador del usuario
     * @return 200 OK con el usuario, o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.findById(id));
    }

    /**
     * Crea un nuevo usuario.
     *
     * @param request datos del usuario a crear
     * @return 201 Created con el usuario creado
     */
    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse response = usuarioService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza un usuario existente.
     *
     * @param id      identificador del usuario a actualizar
     * @param request nuevos datos del usuario
     * @return 200 OK con el usuario actualizado, o 404 si no existe
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.update(id, request));
    }

    /**
     * Desactiva (soft delete) un usuario.
     *
     * @param id identificador del usuario a desactivar
     * @return 204 No Content, o 404 si no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        usuarioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
