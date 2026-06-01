package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.config.SecurityUtil;
import com.elis_lopez.terminalAutobusesBackend.dto.request.ChoferRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.ChoferResponse;
import com.elis_lopez.terminalAutobusesBackend.exception.ResourceNotFoundException;
import com.elis_lopez.terminalAutobusesBackend.model.Chofer;
import com.elis_lopez.terminalAutobusesBackend.model.Terminal;
import com.elis_lopez.terminalAutobusesBackend.repository.AutobusRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.ChoferRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión de choferes.
 * <p>
 * Proporciona operaciones CRUD con soft delete sobre la entidad {@link Chofer},
 * asegurando que cada chofer pertenezca a un {@link Terminal} específico.
 * <p>
 * <strong>Reglas de autorización:</strong>
 * <ul>
 *   <li>{@code CENTRAL_ADMIN} — acceso completo a todos los choferes</li>
 *   <li>{@code TERMINAL_ADMIN} — solo puede ver, crear, actualizar y eliminar
 *       choferes de su propio terminal</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChoferService {

    private final ChoferRepository choferRepository;
    private final TerminalRepository terminalRepository;
    private final AutobusRepository autobusRepository;

    /**
     * Obtiene todos los choferes registrados.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, solo devuelve
     * los choferes de su propio terminal.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return página de respuestas de choferes
     */
    @Transactional(readOnly = true)
    public Page<ChoferResponse> findAll(Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            Long terminalId = SecurityUtil.getCurrentUserTerminalId();
            if (terminalId != null) {
                return choferRepository.findByTerminalId(terminalId, pageable)
                        .map(this::enrichWithAutobus);
            }
            return Page.empty();
        }
        Page<Chofer> choferes = choferRepository.findAll(pageable);
        log.info("Obtenidos {} choferes (total: {})", choferes.getNumberOfElements(), choferes.getTotalElements());
        return choferes.map(this::enrichWithAutobus);
    }

    /**
     * Obtiene los choferes que pertenecen a un terminal específico.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, ignora el
     * {@code terminalId} recibido y fuerza el filtro a su propio terminal.
     *
     * @param terminalId identificador del terminal
     * @param pageable   parámetros de paginación y ordenación
     * @return página de respuestas de choferes del terminal
     */
    @Transactional(readOnly = true)
    public Page<ChoferResponse> findByTerminalId(Long terminalId, Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            terminalId = SecurityUtil.getCurrentUserTerminalId();
        }
        if (terminalId == null) {
            return Page.empty();
        }
        return choferRepository.findByTerminalId(terminalId, pageable)
                .map(this::enrichWithAutobus);
    }

    /**
     * Busca un chofer por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * chofer pertenezca a su terminal.
     *
     * @param id identificador del chofer
     * @return respuesta del chofer encontrado
     * @throws ResourceNotFoundException si no existe el chofer
     * @throws AccessDeniedException     si el chofer no pertenece al terminal del usuario
     */
    @Transactional(readOnly = true)
    public ChoferResponse findById(Long id) {
        Chofer chofer = choferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", id));

        if (SecurityUtil.isTerminalAdmin()) {
            Long currentTerminalId = SecurityUtil.getCurrentUserTerminalId();
            if (!chofer.getTerminal().getId().equals(currentTerminalId)) {
                throw new AccessDeniedException("No tienes permiso para ver este chofer");
            }
        }

        return enrichWithAutobus(chofer);
    }

    /**
     * Crea un nuevo chofer asignado a un terminal.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, fuerza el terminal
     * al suyo propio.
     *
     * @param request datos del chofer a crear
     * @return respuesta del chofer creado
     */
    @Transactional
    public ChoferResponse create(ChoferRequest request) {
        if (SecurityUtil.isTerminalAdmin()) {
            request.setTerminalId(SecurityUtil.getCurrentUserTerminalId());
        }

        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalId()));

        Chofer chofer = new Chofer();
        mapRequestToEntity(request, chofer, terminal);

        Chofer saved = choferRepository.save(chofer);
        log.info("Chofer creado: id={}, nombre={} {}", saved.getId(), saved.getNombre(), saved.getApellido());
        return enrichWithAutobus(saved);
    }

    /**
     * Actualiza un chofer existente.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * chofer pertenezca a su terminal y fuerza el terminal al suyo propio.
     *
     * @param id      identificador del chofer a actualizar
     * @param request nuevos datos del chofer
     * @return respuesta del chofer actualizado
     * @throws ResourceNotFoundException si no existe el chofer o el terminal
     * @throws AccessDeniedException     si el chofer no pertenece al terminal del usuario
     */
    @Transactional
    public ChoferResponse update(Long id, ChoferRequest request) {
        Chofer chofer = choferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!chofer.getTerminal().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para modificar este chofer");
            }
            request.setTerminalId(SecurityUtil.getCurrentUserTerminalId());
        }

        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalId()));

        mapRequestToEntity(request, chofer, terminal);

        Chofer saved = choferRepository.save(chofer);
        log.info("Chofer actualizado: id={}", saved.getId());
        return enrichWithAutobus(saved);
    }

    /**
     * Desactiva (soft delete) un chofer por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * chofer pertenezca a su terminal.
     *
     * @param id identificador del chofer a desactivar
     * @throws ResourceNotFoundException si no existe el chofer
     * @throws AccessDeniedException     si el chofer no pertenece al terminal del usuario
     */
    @Transactional
    public void delete(Long id) {
        Chofer chofer = choferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!chofer.getTerminal().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para eliminar este chofer");
            }
        }

        chofer.setActivo(false);
        choferRepository.save(chofer);
        log.info("Chofer desactivado: id={}", id);
    }

    /**
     * Mapea los campos del request a la entidad Chofer.
     */
    /**
     * Construye un {@link ChoferResponse} enriqueciendo con el autobús asignado.
     */
    private ChoferResponse enrichWithAutobus(Chofer chofer) {
        ChoferResponse resp = ChoferResponse.fromEntity(chofer);
        autobusRepository.findByChoferId(chofer.getId())
                .ifPresent(a -> resp.setAutobusAsignado(
                        "U" + a.getNumeroUnidad() + " - " + a.getMatricula()
                ));
        return resp;
    }

    /**
     * Mapea los campos del request a la entidad Chofer.
     */
    private void mapRequestToEntity(ChoferRequest request, Chofer chofer, Terminal terminal) {
        chofer.setNombre(request.getNombre());
        chofer.setApellido(request.getApellido());
        chofer.setCedula(request.getCedula());
        chofer.setTelefono(request.getTelefono());
        chofer.setFechaNacimiento(request.getFechaNacimiento());
        chofer.setDireccion(request.getDireccion());
        chofer.setTerminal(terminal);
        chofer.setActivo(request.isActivo());
    }
}
