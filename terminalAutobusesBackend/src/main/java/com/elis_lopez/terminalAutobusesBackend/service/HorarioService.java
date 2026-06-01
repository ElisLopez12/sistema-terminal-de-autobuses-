package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.config.SecurityUtil;
import com.elis_lopez.terminalAutobusesBackend.dto.request.HorarioRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.HorarioResponse;
import com.elis_lopez.terminalAutobusesBackend.exception.ResourceNotFoundException;
import com.elis_lopez.terminalAutobusesBackend.model.Horario;
import com.elis_lopez.terminalAutobusesBackend.model.Ruta;
import com.elis_lopez.terminalAutobusesBackend.model.Terminal;
import com.elis_lopez.terminalAutobusesBackend.model.enums.DiaSemana;
import com.elis_lopez.terminalAutobusesBackend.repository.HorarioRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.RutaRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para la gestión de horarios tipo plantilla.
 * <p>
 * Los horarios definen frecuencias de salida (día, hora inicio, hora fin, intervalo)
 * y sirven como guía para crear {@link com.elis_lopez.terminalAutobusesBackend.model.Salida} manuales.
 * <p>
 * <strong>Reglas de autorización:</strong>
 * <ul>
 *   <li>{@code CENTRAL_ADMIN} — acceso completo a todos los horarios</li>
 *   <li>{@code TERMINAL_ADMIN} — solo puede ver, crear, actualizar y eliminar
 *       horarios de su propio terminal</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final RutaRepository rutaRepository;
    private final TerminalRepository terminalRepository;

    /**
     * Obtiene todos los horarios registrados.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, solo devuelve
     * los horarios de su propio terminal de origen.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return página de respuestas de horarios
     */
    @Transactional(readOnly = true)
    public Page<HorarioResponse> findAll(Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            Long terminalId = SecurityUtil.getCurrentUserTerminalId();
            if (terminalId != null) {
                return horarioRepository.findByTerminalOrigenId(terminalId, pageable)
                        .map(HorarioResponse::fromEntity);
            }
            return Page.empty();
        }
        Page<Horario> horarios = horarioRepository.findAll(pageable);
        log.info("Obtenidos {} horarios (total: {})", horarios.getNumberOfElements(), horarios.getTotalElements());
        return horarios.map(HorarioResponse::fromEntity);
    }

    /**
     * Obtiene los horarios de una ruta específica.
     *
     * @param rutaId   identificador de la ruta
     * @param pageable parámetros de paginación y ordenación
     * @return página de respuestas de horarios de la ruta
     */
    @Transactional(readOnly = true)
    public Page<HorarioResponse> findByRutaId(Long rutaId, Pageable pageable) {
        return horarioRepository.findByRutaId(rutaId, pageable)
                .map(HorarioResponse::fromEntity);
    }

    /**
     * Obtiene los horarios que pertenecen a un terminal de origen específico.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, ignora el
     * {@code terminalId} recibido y fuerza el filtro a su propio terminal.
     *
     * @param terminalId identificador del terminal
     * @param pageable   parámetros de paginación y ordenación
     * @return página de respuestas de horarios del terminal
     */
    @Transactional(readOnly = true)
    public Page<HorarioResponse> findByTerminalOrigenId(Long terminalId, Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            terminalId = SecurityUtil.getCurrentUserTerminalId();
        }
        if (terminalId == null) {
            return Page.empty();
        }
        return horarioRepository.findByTerminalOrigenId(terminalId, pageable)
                .map(HorarioResponse::fromEntity);
    }

    /**
     * Busca un horario por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * horario pertenezca a su terminal de origen.
     *
     * @param id identificador del horario
     * @return respuesta del horario encontrado
     * @throws ResourceNotFoundException si no existe el horario
     * @throws AccessDeniedException     si el horario no pertenece al terminal del usuario
     */
    @Transactional(readOnly = true)
    public HorarioResponse findById(Long id) {
        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario", id));

        if (SecurityUtil.isTerminalAdmin()) {
            Long currentTerminalId = SecurityUtil.getCurrentUserTerminalId();
            if (!horario.getTerminalOrigen().getId().equals(currentTerminalId)) {
                throw new AccessDeniedException("No tienes permiso para ver este horario");
            }
        }

        return HorarioResponse.fromEntity(horario);
    }

    /**
     * Busca horarios activos cuyo día de la semana coincida con el indicado.
     * <p>
     * Utilizado por el scheduler {@link com.elis_lopez.terminalAutobusesBackend.scheduler.SalidaScheduler}
     * para determinar qué horarios aplicar al generar salidas automáticas.
     *
     * @param dia día de la semana a filtrar
     * @return lista de horarios activos para ese día
     */
    @Transactional(readOnly = true)
    public List<Horario> findActivosByDia(DiaSemana dia) {
        return horarioRepository.findByActivoTrueAndDiaSemanaIn(List.of(dia, DiaSemana.TODOS));
    }

    /**
     * Crea un nuevo horario plantilla para una ruta y terminal de origen.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, fuerza el terminal
     * de origen al suyo propio.
     *
     * @param request datos del horario a crear
     * @return respuesta del horario creado
     */
    @Transactional
    public List<HorarioResponse> create(HorarioRequest request) {
        if (SecurityUtil.isTerminalAdmin()) {
            request.setTerminalOrigenId(SecurityUtil.getCurrentUserTerminalId());
        }

        Ruta ruta = rutaRepository.findById(request.getRutaId())
                .orElseThrow(() -> new ResourceNotFoundException("Ruta", request.getRutaId()));
        Terminal terminalOrigen = terminalRepository.findById(request.getTerminalOrigenId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalOrigenId()));

        // Determinar qué días crear
        List<DiaSemana> dias = request.getDiasSemana();
        if (dias == null || dias.isEmpty()) {
            dias = List.of(request.getDiaSemana());
        }

        List<Horario> savedList = new ArrayList<>();
        for (DiaSemana dia : dias) {
            Horario horario = new Horario();
            mapRequestToEntity(request, horario, ruta, terminalOrigen);
            horario.setDiaSemana(dia);
            savedList.add(horarioRepository.save(horario));
        }

        log.info("Creados {} horarios para rutaId={}", savedList.size(), request.getRutaId());
        return savedList.stream().map(HorarioResponse::fromEntity).toList();
    }

    /**
     * Actualiza un horario existente.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * horario pertenezca a su terminal y fuerza el terminal al suyo propio.
     *
     * @param id      identificador del horario a actualizar
     * @param request nuevos datos del horario
     * @return respuesta del horario actualizado
     * @throws ResourceNotFoundException si no existe el horario, la ruta o el terminal
     * @throws AccessDeniedException     si el horario no pertenece al terminal del usuario
     */
    @Transactional
    public HorarioResponse update(Long id, HorarioRequest request) {
        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!horario.getTerminalOrigen().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para modificar este horario");
            }
            request.setTerminalOrigenId(SecurityUtil.getCurrentUserTerminalId());
        }

        Ruta ruta = rutaRepository.findById(request.getRutaId())
                .orElseThrow(() -> new ResourceNotFoundException("Ruta", request.getRutaId()));
        Terminal terminalOrigen = terminalRepository.findById(request.getTerminalOrigenId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalOrigenId()));

        mapRequestToEntity(request, horario, ruta, terminalOrigen);

        Horario saved = horarioRepository.save(horario);
        log.info("Horario actualizado: id={}", saved.getId());
        return HorarioResponse.fromEntity(saved);
    }

    /**
     * Desactiva (soft delete) un horario por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que el
     * horario pertenezca a su terminal de origen.
     *
     * @param id identificador del horario a desactivar
     * @throws ResourceNotFoundException si no existe el horario
     * @throws AccessDeniedException     si el horario no pertenece al terminal del usuario
     */
    @Transactional
    public void delete(Long id) {
        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!horario.getTerminalOrigen().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para eliminar este horario");
            }
        }

        horario.setActivo(false);
        horarioRepository.save(horario);
        log.info("Horario desactivado: id={}", id);
    }

    /**
     * Mapea los campos del request a la entidad Horario.
     */
    private void mapRequestToEntity(HorarioRequest request, Horario horario, Ruta ruta, Terminal terminalOrigen) {
        horario.setRuta(ruta);
        horario.setTerminalOrigen(terminalOrigen);
        horario.setDiaSemana(request.getDiaSemana());
        horario.setHoraInicio(request.getHoraInicio());
        horario.setHoraFin(request.getHoraFin());
        horario.setIntervaloMinutos(request.getIntervaloMinutos());
        horario.setActivo(request.isActivo());
    }
}
