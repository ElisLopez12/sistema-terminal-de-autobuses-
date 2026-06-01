package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.response.*;
import com.elis_lopez.terminalAutobusesBackend.model.Horario;
import com.elis_lopez.terminalAutobusesBackend.model.Ruta;
import com.elis_lopez.terminalAutobusesBackend.model.Salida;
import com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida;
import com.elis_lopez.terminalAutobusesBackend.repository.HorarioRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.RutaRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.SalidaRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/**
 * Controlador REST con endpoints públicos (sin autenticación).
 * <p>
 * Expone recursos de consulta sobre {@code /api/v1/public} para que
 * aplicaciones externas (apps móviles, webs) puedan consultar rutas,
 * salidas y terminales activos.
 */
@RestController
@RequestMapping("/api/v1/public")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PublicController {

    private final RutaRepository rutaRepository;
    private final HorarioRepository horarioRepository;
    private final SalidaRepository salidaRepository;
    private final TerminalRepository terminalRepository;

    /**
     * Devuelve todas las rutas activas con sus paradas.
     * <p>
     * Si se especifica {@code origenId}, filtra las rutas cuyo terminal de
     * origen coincida con el ID proporcionado.
     *
     * @param origenId filtro opcional por terminal de origen
     * @param pageable parámetros de paginación y ordenación
     * @return 200 OK con la página de rutas activas y sus paradas
     */
    @GetMapping("/rutas")
    public ResponseEntity<Page<RutaConParadasResponse>> getRutas(
            @RequestParam(required = false) Long origenId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<RutaConParadasResponse> responses;
        if (origenId != null) {
            responses = rutaRepository.findByActivoTrueAndOrigenId(origenId, pageable)
                    .map(ruta -> RutaConParadasResponse.fromEntity(ruta, List.of()));
        } else {
            responses = rutaRepository.findByActivoTrue(pageable)
                    .map(ruta -> RutaConParadasResponse.fromEntity(ruta, List.of()));
        }

        return ResponseEntity.ok(responses);
    }

    /**
     * Devuelve una ruta activa con sus paradas y horarios.
     *
     * @param id identificador de la ruta
     * @return 200 OK con la ruta, paradas y horarios; 404 si no existe o está inactiva
     */
    @GetMapping("/rutas/{id}")
    public ResponseEntity<RutaConParadasResponse> getRuta(@PathVariable Long id) {
        Ruta ruta = rutaRepository.findById(id)
                .orElse(null);

        if (ruta == null || !ruta.isActivo()) {
            return ResponseEntity.notFound().build();
        }

        List<Horario> horarios = horarioRepository.findByRutaId(ruta.getId()).stream()
                .filter(Horario::isActivo)
                .toList();

        return ResponseEntity.ok(RutaConParadasResponse.fromEntity(ruta, horarios));
    }

    /**
     * Devuelve salidas activas en estado PROGRAMADA o ABORDAJE, ordenadas por
     * horaProgramada ascendente.
     * <p>
     * Muestra solo salidas relevantes al público: las programadas (futuras) y
     * las que están en abordaje. Excluye {@code EN_RUTA} y {@code CANCELADA}.
     * Filtros opcionales: {@code rutaId}, {@code origenId} (terminalId).
     *
     * @param rutaId   filtro opcional por ruta
     * @param origenId filtro opcional por terminal de origen
     * @param pageable parámetros de paginación y ordenación
     * @return 200 OK con la página de salidas activas (PROGRAMADA o ABORDAJE)
     */
    @GetMapping("/salidas")
    public ResponseEntity<Page<SalidaResponse>> getSalidas(
            @RequestParam(required = false) Long rutaId,
            @RequestParam(required = false) Long origenId,
            @PageableDefault(size = 20, sort = "horaProgramada") Pageable pageable) {

        List<EstadoSalida> estadosVisibles = List.of(EstadoSalida.PROGRAMADA, EstadoSalida.ABORDAJE);
        Page<Salida> salidas;

        if (rutaId != null) {
            salidas = salidaRepository.findByActivoTrueAndEstadoInAndRutaIdOrderByHoraProgramadaAsc(
                    estadosVisibles, rutaId, pageable);
        } else if (origenId != null) {
            salidas = salidaRepository.findByActivoTrueAndEstadoInAndTerminalOrigenIdOrderByHoraProgramadaAsc(
                    estadosVisibles, origenId, pageable);
        } else {
            salidas = salidaRepository.findByActivoTrueAndEstadoInOrderByHoraProgramadaAsc(
                    estadosVisibles, pageable);
        }

        // Ordenar por horaReal (programada + retraso) dentro de la página
        List<SalidaResponse> sortedContent = salidas.getContent().stream()
                .map(SalidaResponse::fromEntity)
                .sorted(Comparator.<SalidaResponse, java.time.LocalDateTime>comparing(SalidaResponse::getHoraReal)
                        .thenComparing(SalidaResponse::getHoraProgramada))
                .toList();

        return ResponseEntity.ok(new PageImpl<>(sortedContent, PageRequest.of(
                salidas.getNumber(), salidas.getSize()), salidas.getTotalElements()));
    }

    /**
     * Devuelve todos los terminales activos.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return 200 OK con la página de terminales activos
     */
    @GetMapping("/terminales")
    public ResponseEntity<Page<TerminalResponse>> getTerminales(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<TerminalResponse> responses = terminalRepository.findByActivoTrue(pageable)
                .map(TerminalResponse::fromEntity);

        return ResponseEntity.ok(responses);
    }
}
