package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.config.SecurityUtil;
import com.elis_lopez.terminalAutobusesBackend.dto.request.AutobusRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.AutobusResponse;
import com.elis_lopez.terminalAutobusesBackend.exception.ResourceNotFoundException;
import com.elis_lopez.terminalAutobusesBackend.model.*;
import com.elis_lopez.terminalAutobusesBackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión de autobuses.
 * <p>
 * Proporciona operaciones CRUD con soft delete sobre la entidad {@link Autobus},
 * validando que el chofer y el colector pertenezcan al mismo terminal que el autobús,
 * y que no estén asignados a otra unidad.
 * <p>
 * <strong>Reglas de autorización:</strong>
 * <ul>
 *   <li>{@code CENTRAL_ADMIN} — acceso completo a todos los autobuses</li>
 *   <li>{@code TERMINAL_ADMIN} — solo puede ver, crear, actualizar y eliminar
 *       autobuses de su propio terminal</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutobusService {

    private final AutobusRepository autobusRepository;
    private final TerminalRepository terminalRepository;
    private final ChoferRepository choferRepository;
    private final ColectorRepository colectorRepository;
    private final RutaRepository rutaRepository;

    /**
     * Obtiene todos los autobuses registrados.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, solo devuelve
     * los autobuses de su propio terminal.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return página de respuestas de autobuses
     */
    @Transactional(readOnly = true)
    public Page<AutobusResponse> findAll(Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            Long terminalId = SecurityUtil.getCurrentUserTerminalId();
            if (terminalId != null) {
                return autobusRepository.findByTerminalId(terminalId, pageable)
                        .map(AutobusResponse::fromEntity);
            }
            return Page.empty();
        }
        Page<Autobus> autobuses = autobusRepository.findAll(pageable);
        log.info("Obtenidos {} autobuses (total: {})", autobuses.getNumberOfElements(), autobuses.getTotalElements());
        return autobuses.map(AutobusResponse::fromEntity);
    }

    /**
     * Obtiene los autobuses que pertenecen a un terminal específico.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, ignora el
     * {@code terminalId} recibido y fuerza el filtro a su propio terminal.
     *
     * @param terminalId identificador del terminal
     * @param pageable   parámetros de paginación y ordenación
     * @return página de respuestas de autobuses del terminal
     */
    @Transactional(readOnly = true)
    public Page<AutobusResponse> findByTerminalId(Long terminalId, Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            terminalId = SecurityUtil.getCurrentUserTerminalId();
        }
        if (terminalId == null) {
            return Page.empty();
        }
        return autobusRepository.findByTerminalId(terminalId, pageable)
                .map(AutobusResponse::fromEntity);
    }

    /**
     * Busca un autobús por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * autobús pertenezca a su terminal.
     *
     * @param id identificador del autobús
     * @return respuesta del autobús encontrado
     * @throws ResourceNotFoundException si no existe el autobús
     * @throws AccessDeniedException     si el autobús no pertenece al terminal del usuario
     */
    @Transactional(readOnly = true)
    public AutobusResponse findById(Long id) {
        Autobus autobus = autobusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autobus", id));

        if (SecurityUtil.isTerminalAdmin()) {
            Long currentTerminalId = SecurityUtil.getCurrentUserTerminalId();
            if (!autobus.getTerminal().getId().equals(currentTerminalId)) {
                throw new AccessDeniedException("No tienes permiso para ver este autobús");
            }
        }

        return AutobusResponse.fromEntity(autobus);
    }

    /**
     * Crea un nuevo autobús con asignación opcional de chofer, colector y ruta.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, fuerza el terminal
     * al suyo propio.
     *
     * @param request datos del autobús a crear
     * @return respuesta del autobús creado
     */
    @Transactional
    public AutobusResponse create(AutobusRequest request) {
        if (SecurityUtil.isTerminalAdmin()) {
            request.setTerminalId(SecurityUtil.getCurrentUserTerminalId());
        }

        Autobus autobus = new Autobus();
        mapRequestToEntity(request, autobus);

        // Verificar que chofer y colector pertenecen al mismo terminal
        validatePersonnelAssignment(request, autobus);

        Autobus saved = autobusRepository.save(autobus);
        log.info("Autobus creado: id={}, unidad={}", saved.getId(), saved.getNumeroUnidad());
        return AutobusResponse.fromEntity(saved);
    }

    /**
     * Actualiza un autobús existente.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * autobús pertenezca a su terminal y fuerza el terminal al suyo propio.
     *
     * @param id      identificador del autobús a actualizar
     * @param request nuevos datos del autobús
     * @return respuesta del autobús actualizado
     * @throws ResourceNotFoundException si no existe el autobús
     * @throws AccessDeniedException     si el autobús no pertenece al terminal del usuario
     */
    @Transactional
    public AutobusResponse update(Long id, AutobusRequest request) {
        Autobus autobus = autobusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autobus", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!autobus.getTerminal().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para modificar este autobús");
            }
            request.setTerminalId(SecurityUtil.getCurrentUserTerminalId());
        }

        mapRequestToEntity(request, autobus);
        validatePersonnelAssignment(request, autobus);

        Autobus saved = autobusRepository.save(autobus);
        log.info("Autobus actualizado: id={}", saved.getId());
        return AutobusResponse.fromEntity(saved);
    }

    /**
     * Desactiva (soft delete) un autobús por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * autobús pertenezca a su terminal.
     *
     * @param id identificador del autobús a desactivar
     * @throws ResourceNotFoundException si no existe el autobús
     * @throws AccessDeniedException     si el autobús no pertenece al terminal del usuario
     */
    @Transactional
    public void delete(Long id) {
        Autobus autobus = autobusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autobus", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!autobus.getTerminal().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para eliminar este autobús");
            }
        }

        autobus.setActivo(false);
        autobusRepository.save(autobus);
        log.info("Autobus desactivado: id={}", id);
    }

    /**
     * Mapea los campos del request a la entidad Autobus, resolviendo las referencias
     * a Terminal, Chofer, Colector y Ruta desde la base de datos.
     */
    private void mapRequestToEntity(AutobusRequest request, Autobus autobus) {
        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalId()));

        autobus.setNumeroUnidad(request.getNumeroUnidad());
        autobus.setMatricula(request.getMatricula());
        autobus.setMarca(request.getMarca());
        autobus.setModelo(request.getModelo());
        autobus.setAnio(request.getAnio());
        autobus.setCapacidadPasajeros(request.getCapacidadPasajeros());
        autobus.setTerminal(terminal);
        autobus.setActivo(request.isActivo());

        // Chofer
        if (request.getChoferId() != null) {
            Chofer chofer = choferRepository.findById(request.getChoferId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chofer", request.getChoferId()));
            autobus.setChofer(chofer);
        } else {
            autobus.setChofer(null);
        }

        // Colector
        if (request.getColectorId() != null) {
            Colector colector = colectorRepository.findById(request.getColectorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Colector", request.getColectorId()));
            autobus.setColector(colector);
        } else {
            autobus.setColector(null);
        }

        // Ruta
        if (request.getRutaId() != null) {
            Ruta ruta = rutaRepository.findById(request.getRutaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ruta", request.getRutaId()));
            // Verificar que la ruta comparte el mismo terminal de origen
            if (!ruta.getOrigen().getId().equals(terminal.getId())) {
                throw new IllegalArgumentException(
                        "La ruta '" + ruta.getNombre() + "' no pertenece al terminal '" + terminal.getNombre() + "'");
            }
            autobus.setRuta(ruta);
        } else {
            autobus.setRuta(null);
        }
    }

    /**
     * Valida que el chofer y el colector asignados pertenezcan al mismo terminal
     * que el autobús, y que no estén ya asignados a otra unidad activa.
     */
    private void validatePersonnelAssignment(AutobusRequest request, Autobus autobus) {
        Long terminalId = autobus.getTerminal().getId();

        if (request.getChoferId() != null) {
            Chofer chofer = choferRepository.findById(request.getChoferId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chofer", request.getChoferId()));
            if (!chofer.getTerminal().getId().equals(terminalId)) {
                throw new IllegalArgumentException("El chofer no pertenece al mismo terminal");
            }
            // Verificar que no esté asignado a otro autobús
            autobusRepository.findByChoferId(request.getChoferId())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(autobus.getId())) {
                            throw new IllegalStateException(
                                    "El chofer ya está asignado al autobús " + existing.getNumeroUnidad());
                        }
                    });
        }

        if (request.getColectorId() != null) {
            Colector colector = colectorRepository.findById(request.getColectorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Colector", request.getColectorId()));
            if (!colector.getTerminal().getId().equals(terminalId)) {
                throw new IllegalArgumentException("El colector no pertenece al mismo terminal");
            }
            // Verificar que no esté asignado a otro autobús
            autobusRepository.findByColectorId(request.getColectorId())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(autobus.getId())) {
                            throw new IllegalStateException(
                                    "El colector ya está asignado al autobús " + existing.getNumeroUnidad());
                        }
                    });
        }
    }
}
