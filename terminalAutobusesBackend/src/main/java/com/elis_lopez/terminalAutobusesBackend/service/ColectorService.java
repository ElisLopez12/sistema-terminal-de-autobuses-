package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.config.SecurityUtil;
import com.elis_lopez.terminalAutobusesBackend.dto.request.ColectorRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.ColectorResponse;
import com.elis_lopez.terminalAutobusesBackend.exception.ResourceNotFoundException;
import com.elis_lopez.terminalAutobusesBackend.model.Colector;
import com.elis_lopez.terminalAutobusesBackend.model.Terminal;
import com.elis_lopez.terminalAutobusesBackend.repository.AutobusRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.ColectorRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión de colectores.
 * <p>
 * Proporciona operaciones CRUD con soft delete sobre la entidad {@link Colector},
 * asegurando que cada colector pertenezca a un {@link Terminal} específico.
 * <p>
 * <strong>Reglas de autorización:</strong>
 * <ul>
 *   <li>{@code CENTRAL_ADMIN} — acceso completo a todos los colectores</li>
 *   <li>{@code TERMINAL_ADMIN} — solo puede ver, crear, actualizar y eliminar
 *       colectores de su propio terminal</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ColectorService {

    private final ColectorRepository colectorRepository;
    private final TerminalRepository terminalRepository;
    private final AutobusRepository autobusRepository;

    /**
     * Obtiene todos los colectores registrados.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, solo devuelve
     * los colectores de su propio terminal.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return página de respuestas de colectores
     */
    @Transactional(readOnly = true)
    public Page<ColectorResponse> findAll(Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            Long terminalId = SecurityUtil.getCurrentUserTerminalId();
            if (terminalId != null) {
                return colectorRepository.findByTerminalId(terminalId, pageable)
                        .map(this::enrichWithAutobus);
            }
            return Page.empty();
        }
        Page<Colector> colectores = colectorRepository.findAll(pageable);
        log.info("Obtenidos {} colectores (total: {})", colectores.getNumberOfElements(), colectores.getTotalElements());
        return colectores.map(this::enrichWithAutobus);
    }

    /**
     * Obtiene los colectores que pertenecen a un terminal específico.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, ignora el
     * {@code terminalId} recibido y fuerza el filtro a su propio terminal.
     *
     * @param terminalId identificador del terminal
     * @param pageable   parámetros de paginación y ordenación
     * @return página de respuestas de colectores del terminal
     */
    @Transactional(readOnly = true)
    public Page<ColectorResponse> findByTerminalId(Long terminalId, Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            terminalId = SecurityUtil.getCurrentUserTerminalId();
        }
        if (terminalId == null) {
            return Page.empty();
        }
        return colectorRepository.findByTerminalId(terminalId, pageable)
                .map(this::enrichWithAutobus);
    }

    /**
     * Busca un colector por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * colector pertenezca a su terminal.
     *
     * @param id identificador del colector
     * @return respuesta del colector encontrado
     * @throws ResourceNotFoundException si no existe el colector
     * @throws AccessDeniedException     si el colector no pertenece al terminal del usuario
     */
    @Transactional(readOnly = true)
    public ColectorResponse findById(Long id) {
        Colector colector = colectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colector", id));

        if (SecurityUtil.isTerminalAdmin()) {
            Long currentTerminalId = SecurityUtil.getCurrentUserTerminalId();
            if (!colector.getTerminal().getId().equals(currentTerminalId)) {
                throw new AccessDeniedException("No tienes permiso para ver este colector");
            }
        }

        return enrichWithAutobus(colector);
    }

    /**
     * Crea un nuevo colector asignado a un terminal.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, fuerza el terminal
     * al suyo propio.
     *
     * @param request datos del colector a crear
     * @return respuesta del colector creado
     */
    @Transactional
    public ColectorResponse create(ColectorRequest request) {
        if (SecurityUtil.isTerminalAdmin()) {
            request.setTerminalId(SecurityUtil.getCurrentUserTerminalId());
        }

        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalId()));

        Colector colector = new Colector();
        mapRequestToEntity(request, colector, terminal);

        Colector saved = colectorRepository.save(colector);
        log.info("Colector creado: id={}, nombre={} {}", saved.getId(), saved.getNombre(), saved.getApellido());
        return enrichWithAutobus(saved);
    }

    /**
     * Actualiza un colector existente.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * colector pertenezca a su terminal y fuerza el terminal al suyo propio.
     *
     * @param id      identificador del colector a actualizar
     * @param request nuevos datos del colector
     * @return respuesta del colector actualizado
     * @throws ResourceNotFoundException si no existe el colector o el terminal
     * @throws AccessDeniedException     si el colector no pertenece al terminal del usuario
     */
    @Transactional
    public ColectorResponse update(Long id, ColectorRequest request) {
        Colector colector = colectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colector", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!colector.getTerminal().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para modificar este colector");
            }
            request.setTerminalId(SecurityUtil.getCurrentUserTerminalId());
        }

        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalId()));

        mapRequestToEntity(request, colector, terminal);

        Colector saved = colectorRepository.save(colector);
        log.info("Colector actualizado: id={}", saved.getId());
        return enrichWithAutobus(saved);
    }

    /**
     * Desactiva (soft delete) un colector por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * colector pertenezca a su terminal.
     *
     * @param id identificador del colector a desactivar
     * @throws ResourceNotFoundException si no existe el colector
     * @throws AccessDeniedException     si el colector no pertenece al terminal del usuario
     */
    @Transactional
    public void delete(Long id) {
        Colector colector = colectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colector", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!colector.getTerminal().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para eliminar este colector");
            }
        }

        colector.setActivo(false);
        colectorRepository.save(colector);
        log.info("Colector desactivado: id={}", id);
    }

    /**
     * Mapea los campos del request a la entidad Colector.
     */
    /**
     * Construye un {@link ColectorResponse} enriqueciendo con el autobús asignado.
     */
    private ColectorResponse enrichWithAutobus(Colector colector) {
        ColectorResponse resp = ColectorResponse.fromEntity(colector);
        autobusRepository.findByColectorId(colector.getId())
                .ifPresent(a -> resp.setAutobusAsignado(
                        "U" + a.getNumeroUnidad() + " - " + a.getMatricula()
                ));
        return resp;
    }

    /**
     * Mapea los campos del request a la entidad Colector.
     */
    private void mapRequestToEntity(ColectorRequest request, Colector colector, Terminal terminal) {
        colector.setNombre(request.getNombre());
        colector.setApellido(request.getApellido());
        colector.setCedula(request.getCedula());
        colector.setTelefono(request.getTelefono());
        colector.setFechaNacimiento(request.getFechaNacimiento());
        colector.setDireccion(request.getDireccion());
        colector.setTerminal(terminal);
        colector.setActivo(request.isActivo());
    }
}
