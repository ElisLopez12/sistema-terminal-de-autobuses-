package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.config.SecurityUtil;
import com.elis_lopez.terminalAutobusesBackend.dto.request.TerminalRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.TerminalResponse;
import com.elis_lopez.terminalAutobusesBackend.exception.ResourceNotFoundException;
import com.elis_lopez.terminalAutobusesBackend.model.Terminal;
import com.elis_lopez.terminalAutobusesBackend.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para la gestión de terminales de autobuses.
 * <p>
 * Proporciona operaciones CRUD con soft delete sobre la entidad {@link Terminal}.
 * <p>
 * <strong>Reglas de autorización:</strong>
 * <ul>
 *   <li>{@code CENTRAL_ADMIN} — acceso completo a todos los terminales</li>
 *   <li>{@code TERMINAL_ADMIN} — solo lectura de su propio terminal;
 *       las operaciones de creación, actualización y eliminación están denegadas</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TerminalService {

    private final TerminalRepository terminalRepository;

    /**
     * Obtiene todos los terminales activos e inactivos.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, solo devuelve
     * el terminal al que pertenece.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return página de respuestas de terminales
     */
    @Transactional(readOnly = true)
    public Page<TerminalResponse> findAll(Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            Long terminalId = SecurityUtil.getCurrentUserTerminalId();
            if (terminalId != null) {
                Terminal terminal = terminalRepository.findById(terminalId)
                        .orElseThrow(() -> new ResourceNotFoundException("Terminal", terminalId));
                return new PageImpl<>(List.of(TerminalResponse.fromEntity(terminal)), pageable, 1);
            }
            return Page.empty();
        }
        Page<Terminal> terminales = terminalRepository.findAll(pageable);
        log.info("Obtenidas {} terminales (total: {})", terminales.getNumberOfElements(), terminales.getTotalElements());
        return terminales.map(TerminalResponse::fromEntity);
    }

    /**
     * Busca un terminal por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, solo puede ver
     * su propio terminal.
     *
     * @param id identificador del terminal
     * @return respuesta del terminal encontrado
     * @throws ResourceNotFoundException si no existe el terminal
     * @throws AccessDeniedException     si el {@code TERMINAL_ADMIN} intenta ver otro terminal
     */
    @Transactional(readOnly = true)
    public TerminalResponse findById(Long id) {
        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", id));

        if (SecurityUtil.isTerminalAdmin()) {
            Long currentTerminalId = SecurityUtil.getCurrentUserTerminalId();
            if (!terminal.getId().equals(currentTerminalId)) {
                throw new AccessDeniedException("No tienes permiso para ver este terminal");
            }
        }

        return TerminalResponse.fromEntity(terminal);
    }

    /**
     * Crea un nuevo terminal.
     * <p>
     * Solo {@code CENTRAL_ADMIN} puede crear terminales. Los {@code TERMINAL_ADMIN}
     * no tienen permiso para esta operación.
     *
     * @param request datos del terminal a crear
     * @return respuesta del terminal creado
     * @throws AccessDeniedException si el usuario autenticado es {@code TERMINAL_ADMIN}
     */
    @Transactional
    public TerminalResponse create(TerminalRequest request) {
        if (SecurityUtil.isTerminalAdmin()) {
            throw new AccessDeniedException("No tienes permiso para crear terminales");
        }

        Terminal terminal = new Terminal();
        terminal.setNombre(request.getNombre());
        terminal.setUbicacion(request.getUbicacion());
        terminal.setActivo(request.isActivo());

        Terminal saved = terminalRepository.save(terminal);
        log.info("Terminal creado: id={}, nombre={}", saved.getId(), saved.getNombre());
        return TerminalResponse.fromEntity(saved);
    }

    /**
     * Actualiza un terminal existente.
     * <p>
     * Solo {@code CENTRAL_ADMIN} puede actualizar terminales. Los {@code TERMINAL_ADMIN}
     * no tienen permiso para esta operación.
     *
     * @param id      identificador del terminal a actualizar
     * @param request nuevos datos del terminal
     * @return respuesta del terminal actualizado
     * @throws ResourceNotFoundException si no existe el terminal
     * @throws AccessDeniedException     si el usuario autenticado es {@code TERMINAL_ADMIN}
     */
    @Transactional
    public TerminalResponse update(Long id, TerminalRequest request) {
        if (SecurityUtil.isTerminalAdmin()) {
            throw new AccessDeniedException("No tienes permiso para actualizar terminales");
        }

        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", id));

        terminal.setNombre(request.getNombre());
        terminal.setUbicacion(request.getUbicacion());
        terminal.setActivo(request.isActivo());

        Terminal saved = terminalRepository.save(terminal);
        log.info("Terminal actualizado: id={}", saved.getId());
        return TerminalResponse.fromEntity(saved);
    }

    /**
     * Desactiva (soft delete) un terminal por su ID.
     * <p>
     * Solo {@code CENTRAL_ADMIN} puede eliminar terminales. Los {@code TERMINAL_ADMIN}
     * no tienen permiso para esta operación.
     *
     * @param id identificador del terminal a desactivar
     * @throws ResourceNotFoundException si no existe el terminal
     * @throws AccessDeniedException     si el usuario autenticado es {@code TERMINAL_ADMIN}
     */
    @Transactional
    public void delete(Long id) {
        if (SecurityUtil.isTerminalAdmin()) {
            throw new AccessDeniedException("No tienes permiso para eliminar terminales");
        }

        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", id));
        terminal.setActivo(false);
        terminalRepository.save(terminal);
        log.info("Terminal desactivado: id={}", id);
    }
}
