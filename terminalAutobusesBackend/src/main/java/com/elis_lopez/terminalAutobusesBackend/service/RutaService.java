package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.config.SecurityUtil;
import com.elis_lopez.terminalAutobusesBackend.dto.request.RutaRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.RutaResponse;
import com.elis_lopez.terminalAutobusesBackend.exception.ResourceNotFoundException;
import com.elis_lopez.terminalAutobusesBackend.model.ParadaEmbeddable;
import com.elis_lopez.terminalAutobusesBackend.model.Ruta;
import com.elis_lopez.terminalAutobusesBackend.model.Terminal;
import com.elis_lopez.terminalAutobusesBackend.repository.RutaRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión de rutas de autobuses.
 * <p>
 * Proporciona operaciones CRUD con soft delete sobre la entidad {@link Ruta},
 * donde el destino se maneja como texto libre (no es una FK a Terminal).
 * <p>
 * <strong>Reglas de autorización:</strong>
 * <ul>
 *   <li>{@code CENTRAL_ADMIN} — acceso completo a todas las rutas</li>
 *   <li>{@code TERMINAL_ADMIN} — solo puede ver, crear, actualizar y eliminar
 *       rutas que pertenezcan a su terminal de origen</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RutaService {

    private final RutaRepository rutaRepository;
    private final TerminalRepository terminalRepository;

    /**
     * Obtiene todas las rutas registradas.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, solo devuelve
     * las rutas cuyo terminal de origen es el suyo.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return página de respuestas de rutas
     */
    @Transactional(readOnly = true)
    public Page<RutaResponse> findAll(Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            Long terminalId = SecurityUtil.getCurrentUserTerminalId();
            if (terminalId != null) {
                return rutaRepository.findByOrigenId(terminalId, pageable)
                        .map(RutaResponse::fromEntity);
            }
            return Page.empty();
        }
        Page<Ruta> rutas = rutaRepository.findAll(pageable);
        log.info("Obtenidas {} rutas (total: {})", rutas.getNumberOfElements(), rutas.getTotalElements());
        return rutas.map(RutaResponse::fromEntity);
    }

    /**
     * Busca una ruta por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que la
     * ruta pertenezca a su terminal de origen.
     *
     * @param id identificador de la ruta
     * @return respuesta de la ruta encontrada
     * @throws ResourceNotFoundException si no existe la ruta
     * @throws AccessDeniedException     si la ruta no pertenece al terminal del usuario
     */
    @Transactional(readOnly = true)
    public RutaResponse findById(Long id) {
        Ruta ruta = rutaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta", id));
        return RutaResponse.fromEntity(ruta);
    }

    /**
     * Crea una nueva ruta desde un terminal de origen hacia un destino.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, fuerza el terminal
     * de origen al suyo propio.
     *
     * @param request datos de la ruta a crear
     * @return respuesta de la ruta creada
     */
    @Transactional
    public RutaResponse create(RutaRequest request) {
        if (SecurityUtil.isTerminalAdmin()) {
            request.setOrigenId(SecurityUtil.getCurrentUserTerminalId());
        }

        Terminal origen = terminalRepository.findById(request.getOrigenId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getOrigenId()));

        Ruta ruta = new Ruta();
        ruta.setNombre(request.getNombre());
        ruta.setOrigen(origen);
        ruta.setDestinoNombre(request.getDestinoNombre());
        ruta.setDestinoUbicacion(request.getDestinoUbicacion());
        ruta.setDistanciaKm(request.getDistanciaKm());
        ruta.setDuracionEstimadaMin(request.getDuracionEstimadaMin());
        ruta.setActivo(request.isActivo());
        ruta.setParadas(request.getParadas() != null ? request.getParadas() : new ArrayList<>());

        Ruta saved = rutaRepository.save(ruta);
        log.info("Ruta creada: id={}, nombre={}", saved.getId(), saved.getNombre());
        return RutaResponse.fromEntity(saved);
    }

    /**
     * Actualiza una ruta existente.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que la
     * ruta pertenezca a su terminal y fuerza el origen al suyo propio.
     *
     * @param id      identificador de la ruta a actualizar
     * @param request nuevos datos de la ruta
     * @return respuesta de la ruta actualizada
     * @throws ResourceNotFoundException si no existe la ruta o el terminal de origen
     * @throws AccessDeniedException     si la ruta no pertenece al terminal del usuario
     */
    @Transactional
    public RutaResponse update(Long id, RutaRequest request) {
        Ruta ruta = rutaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!ruta.getOrigen().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para modificar esta ruta");
            }
            request.setOrigenId(SecurityUtil.getCurrentUserTerminalId());
        }

        Terminal origen = terminalRepository.findById(request.getOrigenId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getOrigenId()));

        ruta.setNombre(request.getNombre());
        ruta.setOrigen(origen);
        ruta.setDestinoNombre(request.getDestinoNombre());
        ruta.setDestinoUbicacion(request.getDestinoUbicacion());
        ruta.setDistanciaKm(request.getDistanciaKm());
        ruta.setDuracionEstimadaMin(request.getDuracionEstimadaMin());
        ruta.setActivo(request.isActivo());
        if (request.getParadas() != null) {
            ruta.getParadas().clear();
            ruta.getParadas().addAll(request.getParadas());
        }

        Ruta saved = rutaRepository.save(ruta);
        log.info("Ruta actualizada: id={}", saved.getId());
        return RutaResponse.fromEntity(saved);
    }

    /**
     * Desactiva (soft delete) una ruta por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que la
     * ruta pertenezca a su terminal de origen.
     *
     * @param id identificador de la ruta a desactivar
     * @throws ResourceNotFoundException si no existe la ruta
     * @throws AccessDeniedException     si la ruta no pertenece al terminal del usuario
     */
    @Transactional
    public void delete(Long id) {
        Ruta ruta = rutaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!ruta.getOrigen().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para eliminar esta ruta");
            }
        }

        ruta.setActivo(false);
        rutaRepository.save(ruta);
        log.info("Ruta desactivada: id={}", id);
    }
}
