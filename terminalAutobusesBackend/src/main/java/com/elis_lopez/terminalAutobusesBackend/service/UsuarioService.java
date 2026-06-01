package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.config.SecurityUtil;
import com.elis_lopez.terminalAutobusesBackend.dto.request.UsuarioRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.UsuarioResponse;
import com.elis_lopez.terminalAutobusesBackend.exception.ResourceNotFoundException;
import com.elis_lopez.terminalAutobusesBackend.model.Terminal;
import com.elis_lopez.terminalAutobusesBackend.model.Usuario;
import com.elis_lopez.terminalAutobusesBackend.model.enums.RolUsuario;
import com.elis_lopez.terminalAutobusesBackend.repository.TerminalRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Servicio para la gestión de usuarios del sistema.
 * <p>
 * Proporciona operaciones CRUD con soft delete sobre la entidad {@link Usuario},
 * incluyendo la asignación opcional a un {@link Terminal}.
 * <p>
 * <strong>Reglas de autorización:</strong>
 * <ul>
 *   <li>{@code CENTRAL_ADMIN} — acceso completo a todos los usuarios</li>
 *   <li>{@code TERMINAL_ADMIN} — solo puede ver/editar su propio perfil,
 *       y solo puede crear usuarios con rol {@code TERMINAL_ADMIN}
 *       asignados a su propio terminal</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TerminalRepository terminalRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Obtiene todos los usuarios registrados.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, solo devuelve
     * su propio usuario.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return página de respuestas de usuarios
     */
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> findAll(Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            Long currentUserId = SecurityUtil.getCurrentUserId();
            if (currentUserId != null) {
                Usuario usuario = usuarioRepository.findById(currentUserId)
                        .orElseThrow(() -> new ResourceNotFoundException("Usuario", currentUserId));
                return new PageImpl<>(List.of(UsuarioResponse.fromEntity(usuario)), pageable, 1);
            }
            return Page.empty();
        }
        Page<Usuario> usuarios = usuarioRepository.findAll(pageable);
        log.info("Obtenidos {} usuarios (total: {})", usuarios.getNumberOfElements(), usuarios.getTotalElements());
        return usuarios.map(UsuarioResponse::fromEntity);
    }

    /**
     * Busca un usuario por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, solo puede ver
     * su propio perfil.
     *
     * @param id identificador del usuario
     * @return respuesta del usuario encontrado
     * @throws ResourceNotFoundException si no existe el usuario
     * @throws AccessDeniedException     si el {@code TERMINAL_ADMIN} intenta ver otro usuario
     */
    @Transactional(readOnly = true)
    public UsuarioResponse findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        if (SecurityUtil.isTerminalAdmin()) {
            Long currentUserId = SecurityUtil.getCurrentUserId();
            if (!Objects.equals(usuario.getId(), currentUserId)) {
                throw new AccessDeniedException("No tienes permiso para ver este usuario");
            }
        }

        return UsuarioResponse.fromEntity(usuario);
    }

    /**
     * Crea un nuevo usuario con asignación opcional a un terminal.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}:
     * <ul>
     *   <li>Solo puede crear usuarios con rol {@code TERMINAL_ADMIN}</li>
     *   <li>El terminal asignado debe ser el suyo propio</li>
     * </ul>
     *
     * @param request datos del usuario a crear
     * @return respuesta del usuario creado
     * @throws AccessDeniedException si las validaciones de rol o terminal no se cumplen
     */
    @Transactional
    public UsuarioResponse create(UsuarioRequest request) {
        // Validaciones para TERMINAL_ADMIN
        if (SecurityUtil.isTerminalAdmin()) {
            Long currentTerminalId = SecurityUtil.getCurrentUserTerminalId();
            // No puede crear CENTRAL_ADMIN
            if (request.getRol() == RolUsuario.CENTRAL_ADMIN) {
                throw new AccessDeniedException("No tienes permiso para crear administradores centrales");
            }
            // Debe asignar a su propio terminal
            if (!Objects.equals(request.getTerminalId(), currentTerminalId)) {
                throw new AccessDeniedException("Solo puedes crear usuarios en tu propio terminal");
            }
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(request.getRol());

        if (request.getTerminalId() != null) {
            Terminal terminal = terminalRepository.findById(request.getTerminalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalId()));
            usuario.setTerminal(terminal);
        }

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuario creado: id={}, username={}", saved.getId(), saved.getUsername());
        return UsuarioResponse.fromEntity(saved);
    }

    /**
     * Actualiza un usuario existente.
     * <p>
     * Solo {@code CENTRAL_ADMIN} puede actualizar usuarios. Los {@code TERMINAL_ADMIN}
     * no tienen permiso para esta operación.
     *
     * @param id      identificador del usuario a actualizar
     * @param request nuevos datos del usuario
     * @return respuesta del usuario actualizado
     * @throws ResourceNotFoundException si no existe el usuario o el terminal asignado
     * @throws AccessDeniedException     si el usuario autenticado es {@code TERMINAL_ADMIN}
     */
    @Transactional
    public UsuarioResponse update(Long id, UsuarioRequest request) {
        if (SecurityUtil.isTerminalAdmin()) {
            throw new AccessDeniedException("No tienes permiso para actualizar usuarios");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(request.getRol());

        if (request.getTerminalId() != null) {
            Terminal terminal = terminalRepository.findById(request.getTerminalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalId()));
            usuario.setTerminal(terminal);
        } else {
            usuario.setTerminal(null);
        }

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuario actualizado: id={}", saved.getId());
        return UsuarioResponse.fromEntity(saved);
    }

    /**
     * Desactiva (soft delete) un usuario por su ID.
     * <p>
     * Solo {@code CENTRAL_ADMIN} puede eliminar usuarios. Los {@code TERMINAL_ADMIN}
     * no tienen permiso para esta operación.
     *
     * @param id identificador del usuario a desactivar
     * @throws ResourceNotFoundException si no existe el usuario
     * @throws AccessDeniedException     si el usuario autenticado es {@code TERMINAL_ADMIN}
     */
    @Transactional
    public void delete(Long id) {
        if (SecurityUtil.isTerminalAdmin()) {
            throw new AccessDeniedException("No tienes permiso para eliminar usuarios");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuario desactivado: id={}", id);
    }
}
