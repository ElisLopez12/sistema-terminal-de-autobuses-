package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.config.SecurityUtil;
import com.elis_lopez.terminalAutobusesBackend.dto.request.SalidaRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.SalidaResponse;
import com.elis_lopez.terminalAutobusesBackend.exception.ResourceNotFoundException;
import com.elis_lopez.terminalAutobusesBackend.model.*;
import com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida;
import com.elis_lopez.terminalAutobusesBackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para la gestión de salidas de autobuses.
 * <p>
 * Las salidas representan viajes individuales creados manualmente por el admin.
 * Cada salida tiene una hora programada y puede tener retrasos, estado y
 * asignación opcional de horario plantilla y autobús.
 * <p>
 * <strong>Reglas de autorización:</strong>
 * <ul>
 *   <li>{@code CENTRAL_ADMIN} — acceso completo a todas las salidas</li>
 *   <li>{@code TERMINAL_ADMIN} — solo puede ver, crear, actualizar y eliminar
 *       salidas de su propio terminal de origen</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalidaService {

    private final SalidaRepository salidaRepository;
    private final RutaRepository rutaRepository;
    private final HorarioRepository horarioRepository;
    private final TerminalRepository terminalRepository;
    private final AutobusRepository autobusRepository;

    /**
     * Obtiene todas las salidas registradas.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, solo devuelve
     * las salidas de su propio terminal de origen.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return página de respuestas de salidas
     */
    @Transactional(readOnly = true)
    public Page<SalidaResponse> findAll(Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            Long terminalId = SecurityUtil.getCurrentUserTerminalId();
            if (terminalId != null) {
                return salidaRepository.findByTerminalOrigenId(terminalId, pageable)
                        .map(SalidaResponse::fromEntity);
            }
            return Page.empty();
        }
        Page<Salida> salidas = salidaRepository.findByActivoTrue(pageable);
        log.info("Obtenidas {} salidas (total: {})", salidas.getNumberOfElements(), salidas.getTotalElements());
        return salidas.map(SalidaResponse::fromEntity);
    }

    /**
     * Obtiene las salidas de una ruta específica.
     *
     * @param rutaId   identificador de la ruta
     * @param pageable parámetros de paginación y ordenación
     * @return página de respuestas de salidas de la ruta
     */
    @Transactional(readOnly = true)
    public Page<SalidaResponse> findByRutaId(Long rutaId, Pageable pageable) {
        return salidaRepository.findByRutaId(rutaId, pageable)
                .map(SalidaResponse::fromEntity);
    }

    /**
     * Obtiene las salidas que salen desde un terminal de origen específico.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, ignora el
     * {@code terminalId} recibido y fuerza el filtro a su propio terminal.
     *
     * @param terminalId identificador del terminal
     * @param pageable   parámetros de paginación y ordenación
     * @return página de respuestas de salidas del terminal
     */
    @Transactional(readOnly = true)
    public Page<SalidaResponse> findByTerminalOrigenId(Long terminalId, Pageable pageable) {
        if (SecurityUtil.isTerminalAdmin()) {
            terminalId = SecurityUtil.getCurrentUserTerminalId();
        }
        if (terminalId == null) {
            return Page.empty();
        }
        return salidaRepository.findByTerminalOrigenId(terminalId, pageable)
                .map(SalidaResponse::fromEntity);
    }

    /**
     * Busca una salida por su ID.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que la
     * salida pertenezca a su terminal de origen.
     *
     * @param id identificador de la salida
     * @return respuesta de la salida encontrada
     * @throws ResourceNotFoundException si no existe la salida
     * @throws AccessDeniedException     si la salida no pertenece al terminal del usuario
     */
    @Transactional(readOnly = true)
    public SalidaResponse findById(Long id) {
        Salida salida = salidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));

        if (SecurityUtil.isTerminalAdmin()) {
            Long currentTerminalId = SecurityUtil.getCurrentUserTerminalId();
            if (!salida.getTerminalOrigen().getId().equals(currentTerminalId)) {
                throw new AccessDeniedException("No tienes permiso para ver esta salida");
            }
        }

        return SalidaResponse.fromEntity(salida);
    }

    /**
     * Crea una nueva salida validando que el terminal de origen coincida con la ruta.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, fuerza el terminal
     * de origen al suyo propio.
     *
     * @param request datos de la salida a crear
     * @return respuesta de la salida creada
     * @throws IllegalArgumentException si el terminal de origen no coincide con la ruta
     * @throws IllegalStateException    si el autobús asignado no está activo
     */
    @Transactional
    public SalidaResponse create(SalidaRequest request) {
        if (SecurityUtil.isTerminalAdmin()) {
            request.setTerminalOrigenId(SecurityUtil.getCurrentUserTerminalId());
        }

        Ruta ruta = rutaRepository.findById(request.getRutaId())
                .orElseThrow(() -> new ResourceNotFoundException("Ruta", request.getRutaId()));
        Terminal terminalOrigen = terminalRepository.findById(request.getTerminalOrigenId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalOrigenId()));

        // Verificar que el terminal de origen de la salida coincide con el origen de la ruta
        if (!ruta.getOrigen().getId().equals(terminalOrigen.getId())) {
            throw new IllegalArgumentException(
                    "El terminal de origen de la salida no coincide con el origen de la ruta");
        }

        Salida salida = new Salida();
        salida.setRuta(ruta);
        salida.setTerminalOrigen(terminalOrigen);
        salida.setHoraProgramada(request.getHoraProgramada());
        salida.setRetrasoMinutos(request.getRetrasoMinutos() != null ? request.getRetrasoMinutos() : 0);
        salida.setEstado(request.getEstado() != null ? request.getEstado() : EstadoSalida.PROGRAMADA);
        salida.setActivo(request.isActivo());

        // Horario opcional
        if (request.getHorarioId() != null) {
            Horario horario = horarioRepository.findById(request.getHorarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Horario", request.getHorarioId()));
            salida.setHorario(horario);
        }

        // Autobús opcional — verificar que existe, está activo y tiene chofer
        if (request.getAutobusId() != null) {
            Autobus autobus = autobusRepository.findById(request.getAutobusId())
                    .orElseThrow(() -> new ResourceNotFoundException("Autobus", request.getAutobusId()));
            if (!autobus.isActivo()) {
                throw new IllegalStateException("El autobús no está activo");
            }
            if (autobus.getChofer() == null) {
                throw new IllegalArgumentException("El autobús no tiene un chofer asignado");
            }
            salida.setAutobus(autobus);
        }

        Salida saved = salidaRepository.save(salida);
        log.info("Salida creada: id={}, rutaId={}", saved.getId(), saved.getRuta().getId());
        return SalidaResponse.fromEntity(saved);
    }

    /**
     * Actualiza una salida existente.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que la
     * salida pertenezca a su terminal y fuerza el terminal al suyo propio.
     *
     * @param id      identificador de la salida a actualizar
     * @param request nuevos datos de la salida
     * @return respuesta de la salida actualizada
     * @throws ResourceNotFoundException si no existe la salida o alguna referencia
     * @throws IllegalStateException    si el autobús asignado no está activo
     * @throws AccessDeniedException    si la salida no pertenece al terminal del usuario
     */
    @Transactional
    public SalidaResponse update(Long id, SalidaRequest request) {
        Salida salida = salidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!salida.getTerminalOrigen().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para modificar esta salida");
            }
            request.setTerminalOrigenId(SecurityUtil.getCurrentUserTerminalId());
        }

        LocalDateTime oldHora = salida.getHoraProgramada();

        Ruta ruta = rutaRepository.findById(request.getRutaId())
                .orElseThrow(() -> new ResourceNotFoundException("Ruta", request.getRutaId()));
        Terminal terminalOrigen = terminalRepository.findById(request.getTerminalOrigenId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.getTerminalOrigenId()));

        salida.setRuta(ruta);
        salida.setTerminalOrigen(terminalOrigen);
        salida.setHoraProgramada(request.getHoraProgramada());
        salida.setRetrasoMinutos(request.getRetrasoMinutos() != null ? request.getRetrasoMinutos() : 0);
        salida.setEstado(request.getEstado() != null ? request.getEstado() : EstadoSalida.PROGRAMADA);
        salida.setActivo(request.isActivo());

        if (request.getHorarioId() != null) {
            Horario horario = horarioRepository.findById(request.getHorarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Horario", request.getHorarioId()));
            salida.setHorario(horario);
        } else {
            salida.setHorario(null);
        }

        if (request.getAutobusId() != null) {
            Autobus autobus = autobusRepository.findById(request.getAutobusId())
                    .orElseThrow(() -> new ResourceNotFoundException("Autobus", request.getAutobusId()));
            if (!autobus.isActivo()) {
                throw new IllegalStateException("El autobús no está activo");
            }
            if (autobus.getChofer() == null) {
                throw new IllegalArgumentException("El autobús no tiene un chofer asignado");
            }
            salida.setAutobus(autobus);
        } else {
            salida.setAutobus(null);
        }

        Salida saved = salidaRepository.save(salida);

        // Propagar cambio de hora a salidas siguientes de la misma ruta
        if (oldHora != null && saved.getRuta() != null
                && (saved.getEstado() == EstadoSalida.PROGRAMADA || saved.getEstado() == EstadoSalida.ABORDAJE)) {
            int delta = (int) Duration.between(oldHora, saved.getHoraProgramada()).toMinutes();
            if (delta != 0) {
                propagarDiferencia(saved, delta);
                log.info("Hora propagada: {} min desde salida {}", delta, id);
            }
        }

        return SalidaResponse.fromEntity(saved);
    }

    private static final ZoneId ZONA_VENEZUELA = ZoneId.of("America/Caracas");

    /**
     * Cambia el estado de una salida sin modificar otros datos.
     * <p>
     * Si el estado es {@code EN_RUTA}, calcula automáticamente el retraso
     * ({@code ahora - horaProgramada}) y lo propaga a las salidas siguientes
     * de la misma ruta.
     * <p>
     * Si el usuario es {@code TERMINAL_ADMIN}, verifica que la salida
     * pertenezca a su terminal.
     *
     * @param id          identificador de la salida
     * @param nuevoEstado nuevo estado a asignar
     * @param horaReal    hora real de la salida (opcional — si es null se usa la hora del servidor)
     * @return respuesta de la salida actualizada
     * @throws ResourceNotFoundException si la salida no existe
     */
    @Transactional
    public SalidaResponse cambiarEstado(Long id, EstadoSalida nuevoEstado, LocalDateTime horaReal) {
        Salida salida = salidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!salida.getTerminalOrigen().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para modificar esta salida");
            }
        }

        salida.setEstado(nuevoEstado);

        // Auto-detectar retraso al marcar EN_RUTA
        if (nuevoEstado == EstadoSalida.EN_RUTA) {
            LocalDateTime ahora = horaReal != null ? horaReal : LocalDateTime.now(ZONA_VENEZUELA);
            int retrasoCalculado = (int) Duration.between(salida.getHoraProgramada(), ahora).toMinutes();
            if (retrasoCalculado > 0) {
                log.info("Retraso auto-detectado: {} minutos para salida {}", retrasoCalculado, id);
                // NO setear retrasoMinutos acá — ajustarRetrasoConPropagacion lo hace
                // y calcula la diferencia internamente contra el valor anterior
                salidaRepository.save(salida); // persiste el cambio de estado
                return ajustarRetrasoConPropagacion(id, retrasoCalculado);
            }
        }

        Salida saved = salidaRepository.save(salida);
        log.info("Estado de salida {} cambiado a {}", id, nuevoEstado);
        return SalidaResponse.fromEntity(saved);
    }

    /**
     * Asigna un autobús a una salida PROGRAMADA.
     * <p>
     * Valida que:
     * <ul>
     *   <li>La salida exista y esté en estado {@code PROGRAMADA}</li>
     *   <li>El autobús exista y esté activo</li>
     *   <li>El autobús tenga un chofer asignado</li>
     *   <li>El autobús pertenezca al mismo terminal que la salida</li>
     *   <li>El autobús no tenga otra salida PROGRAMADA, ABORDAJE o EN_RUTA el mismo día</li>
     *   <li>Si {@code sobreescribirRuta} es {@code false} (default) y el autobús
     *       está asignado a otra ruta, lanza {@link IllegalArgumentException}</li>
     * </ul>
     * <p>
     * Si {@code sobreescribirRuta} es {@code true}, la ruta del autobús se actualiza
     * automáticamente a la ruta de la salida.
     *
     * @param salidaId          ID de la salida
     * @param autobusId         ID del autobús a asignar
     * @param sobreescribirRuta si {@code true}, reasigna la ruta del autobús
     * @return respuesta de la salida actualizada
     * @throws ResourceNotFoundException si la salida o el autobús no existen
     * @throws IllegalStateException    si la salida no está PROGRAMADA, o el autobús está inactivo/ocupado
     * @throws IllegalArgumentException si el autobús es de otro terminal, no tiene chofer,
     *                                  o está asignado a otra ruta y no se permite sobreescribir
     */
    @Transactional
    public SalidaResponse asignarAutobus(Long salidaId, Long autobusId, boolean sobreescribirRuta) {
        Salida salida = salidaRepository.findById(salidaId)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", salidaId));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!salida.getTerminalOrigen().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para asignar autobús a esta salida");
            }
        }

        if (salida.getEstado() != EstadoSalida.PROGRAMADA) {
            throw new IllegalStateException("Solo se puede asignar autobús a salidas PROGRAMADA");
        }

        Autobus autobus = autobusRepository.findById(autobusId)
                .orElseThrow(() -> new ResourceNotFoundException("Autobus", autobusId));

        if (!autobus.isActivo()) {
            throw new IllegalStateException("El autobús no está activo");
        }

        if (autobus.getChofer() == null) {
            throw new IllegalArgumentException("El autobús no tiene un chofer asignado");
        }

        if (!autobus.getTerminal().getId().equals(salida.getTerminalOrigen().getId())) {
            throw new IllegalArgumentException(
                    "El autobús pertenece a otro terminal y no puede asignarse a esta salida");
        }

        // Validar ruta del autobús contra la ruta de la salida
        if (autobus.getRuta() != null
                && salida.getRuta() != null
                && !autobus.getRuta().getId().equals(salida.getRuta().getId())) {
            if (sobreescribirRuta) {
                autobus.setRuta(salida.getRuta());
                autobusRepository.save(autobus);
                log.info("Ruta del autobús {} reasignada a ruta {}", autobusId, salida.getRuta().getId());
            } else {
                throw new IllegalArgumentException(
                        "El autobús está asignado a la ruta " + autobus.getRuta().getNombre()
                        + " y la salida es de la ruta " + salida.getRuta().getNombre()
                        + ". Usá sobreescribirRuta=true para reasignarlo.");
            }
        }

        // Verificar conflicto horario: el autobús no debe tener otra salida PROGRAMADA, ABORDAJE o EN_RUTA el mismo día
        LocalDateTime inicioDia = salida.getHoraProgramada().toLocalDate().atStartOfDay();
        LocalDateTime finDia = salida.getHoraProgramada().toLocalDate().plusDays(1).atStartOfDay();
        long conflictos = salidaRepository.countConflictByAutobusAndFecha(
                autobusId,
                List.of(EstadoSalida.PROGRAMADA, EstadoSalida.ABORDAJE, EstadoSalida.EN_RUTA),
                inicioDia,
                finDia,
                salidaId
        );
        if (conflictos > 0) {
            throw new IllegalStateException("El autobús ya está ocupado en otra salida para este día");
        }

        salida.setAutobus(autobus);

        Salida saved = salidaRepository.save(salida);
        log.info("Autobús {} asignado a salida {}", autobusId, salidaId);
        return SalidaResponse.fromEntity(saved);
    }

    /**
     * Ajusta el retraso de una salida y propaga el mismo retraso a las
     * salidas siguientes de la misma ruta el mismo día.
     * <p>
     * La propagación solo afecta a salidas en estado {@code PROGRAMADA} o {@code ABORDAJE}.
     * Salidas en {@code EN_RUTA} o {@code CANCELADA} no se modifican. El retraso debe ser >= 0.
     *
     * @param id             ID de la salida a retrasar
     * @param retrasoMinutos minutos de retraso (>= 0)
     * @return respuesta de la salida actualizada
     * @throws ResourceNotFoundException si la salida no existe
     */
    @Transactional
    public SalidaResponse ajustarRetrasoConPropagacion(Long id, int retrasoMinutos) {
        Salida salida = salidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!salida.getTerminalOrigen().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para ajustar el retraso de esta salida");
            }
        }

        int retrasoAnterior = salida.getRetrasoMinutos() != null ? salida.getRetrasoMinutos() : 0;
        int diferencia = retrasoMinutos - retrasoAnterior;

        salida.setRetrasoMinutos(retrasoMinutos);

        if (diferencia != 0) {
            propagarDiferencia(salida, diferencia);
        } else {
            salidaRepository.save(salida);
        }

        return SalidaResponse.fromEntity(salida);
    }

    /**
     * Propaga un delta (diferencia en minutos) a las salidas siguientes de la
     * misma ruta, desplazando su {@code horaProgramada}.
     * <p>
     * Solo afecta a salidas en estado {@code PROGRAMADA} o {@code ABORDAJE}.
     * <p>
     * Si la lista incluye más de una salida, persiste todas de una vez con
     * {@code saveAll}; en caso contrario ya fue persistida por el invocante.
     *
     * @param salida     salida origen (ya modificada, sin persistir)
     * @param diferencia minutos a sumar/restar a las salidas siguientes
     */
    private void propagarDiferencia(Salida salida, int diferencia) {
        List<Salida> siguientes = salidaRepository
                .findByRutaIdAndHoraProgramadaAfterOrderByHoraProgramadaAsc(
                        salida.getRuta().getId(), salida.getHoraProgramada());

        if (siguientes.isEmpty()) return;

        List<Salida> aGuardar = new ArrayList<>();
        aGuardar.add(salida);

        for (Salida destino : siguientes) {
            if (destino.getEstado() == EstadoSalida.PROGRAMADA
                    || destino.getEstado() == EstadoSalida.ABORDAJE) {
                destino.setHoraProgramada(destino.getHoraProgramada().plusMinutes(diferencia));
                aGuardar.add(destino);
            }
        }

        if (aGuardar.size() > 1) {
            salidaRepository.saveAll(aGuardar);
            log.info("Diferencia propagada: {} min desde salida {} a {} salidas siguientes",
                    diferencia, salida.getId(), aGuardar.size() - 1);
        }
    }

    /**
     * Elimina físicamente una salida por su ID.
     * <p>
     * A diferencia de otras entidades del sistema, las salidas se borran
     * realmente de la base de datos para permitir limpieza de datos de prueba.
     * <p>
     * Si el usuario autenticado es {@code TERMINAL_ADMIN}, verifica que la
     * salida pertenezca a su terminal de origen.
     *
     * @param id identificador de la salida a eliminar
     * @throws ResourceNotFoundException si no existe la salida
     * @throws AccessDeniedException     si la salida no pertenece al terminal del usuario
     */
    @Transactional
    public void delete(Long id) {
        Salida salida = salidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));

        if (SecurityUtil.isTerminalAdmin()) {
            if (!salida.getTerminalOrigen().getId().equals(SecurityUtil.getCurrentUserTerminalId())) {
                throw new AccessDeniedException("No tienes permiso para eliminar esta salida");
            }
        }

        salidaRepository.delete(salida);
        log.info("Salida eliminada: id={}", id);
    }
}
