package com.elis_lopez.terminalAutobusesBackend.controller;

import com.elis_lopez.terminalAutobusesBackend.dto.request.AsignarAutobusRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.request.AjustarRetrasoRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.request.CambiarEstadoRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.request.SalidaRequest;
import com.elis_lopez.terminalAutobusesBackend.dto.response.SalidaResponse;
import com.elis_lopez.terminalAutobusesBackend.scheduler.SalidaScheduler;
import com.elis_lopez.terminalAutobusesBackend.service.SalidaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la gestión de salidas (viajes individuales).
 * <p>
 * Expone operaciones CRUD sobre el recurso {@code /api/v1/salidas},
 * con filtros opcionales por ruta y terminal de origen. Además, expone
 * endpoints para generación automática, asignación de autobús y ajuste
 * de retraso con propagación.
 */
@RestController
@RequestMapping("/api/v1/salidas")
@RequiredArgsConstructor
public class SalidaController {

    private final SalidaService salidaService;
    private final SalidaScheduler salidaScheduler;

    /**
     * Obtiene todas las salidas, opcionalmente filtradas por ruta o terminal.
     *
     * @param rutaId     filtro opcional por ruta
     * @param terminalId filtro opcional por terminal de origen
     * @param pageable   parámetros de paginación y ordenación
     * @return 200 OK con la página de salidas
     */
    @GetMapping
    public ResponseEntity<Page<SalidaResponse>> findAll(
            @RequestParam(required = false) Long rutaId,
            @RequestParam(required = false) Long terminalId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        if (rutaId != null) {
            return ResponseEntity.ok(salidaService.findByRutaId(rutaId, pageable));
        }
        if (terminalId != null) {
            return ResponseEntity.ok(salidaService.findByTerminalOrigenId(terminalId, pageable));
        }
        return ResponseEntity.ok(salidaService.findAll(pageable));
    }

    /**
     * Obtiene una salida por su ID.
     *
     * @param id identificador de la salida
     * @return 200 OK con la salida, o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<SalidaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(salidaService.findById(id));
    }

    /**
     * Crea una nueva salida.
     *
     * @param request datos de la salida a crear
     * @return 201 Created con la salida creada
     */
    @PostMapping
    public ResponseEntity<SalidaResponse> create(@Valid @RequestBody SalidaRequest request) {
        SalidaResponse response = salidaService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza una salida existente.
     *
     * @param id      identificador de la salida a actualizar
     * @param request nuevos datos de la salida
     * @return 200 OK con la salida actualizada, o 404 si no existe
     */
    @PutMapping("/{id}")
    public ResponseEntity<SalidaResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody SalidaRequest request) {
        return ResponseEntity.ok(salidaService.update(id, request));
    }

    /**
     * Cambia el estado de una salida sin modificar el resto de sus datos.
     *
     * @param id      identificador de la salida
     * @param request cuerpo con el nuevo estado
     * @return 200 OK con la salida actualizada
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<SalidaResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(salidaService.cambiarEstado(id, request.getEstado(), request.getHoraReal()));
    }

    /**
     * Desactiva (soft delete) una salida.
     *
     * @param id identificador de la salida a desactivar
     * @return 204 No Content, o 404 si no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salidaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Genera salidas automáticas para el día actual a partir de los
     * horarios activos.
     * <p>
     * Invoca la misma lógica que el scheduler programado a las 05:00.
     * Útil para pruebas o recuperación si el scheduler no se ejecutó.
     *
     * @return 200 OK con mapa de resultados (salidasGeneradas, horariosProcesados, horariosSaltados)
     */
    @PostMapping("/generar-del-dia")
    public ResponseEntity<Map<String, Integer>> generarDelDia() {
        Map<String, Integer> resultado = salidaScheduler.generarSalidasDelDia();
        return ResponseEntity.ok(resultado);
    }

    /**
     * Asigna un autobús a una salida PROGRAMADA.
     * <p>
     * Valida que el autobús esté activo, tenga chofer, pertenezca al mismo
     * terminal que la salida y no tenga otra salida PROGRAMADA o EN_CURSO
     * el mismo día.
     *
     * @param id      identificador de la salida
     * @param request cuerpo con el ID del autobús
     * @return 200 OK con la salida actualizada, o 400/409 según validación
     */
    @PutMapping("/{id}/asignar-autobus")
    public ResponseEntity<SalidaResponse> asignarAutobus(
            @PathVariable Long id,
            @Valid @RequestBody AsignarAutobusRequest request) {
        SalidaResponse response = salidaService.asignarAutobus(id, request.getAutobusId(), request.isSobreescribirRuta());
        return ResponseEntity.ok(response);
    }

    /**
     * Ajusta el retraso de una salida y propaga el mismo retraso a las
     * salidas siguientes PROGRAMADAS de la misma ruta.
     * <p>
     * El retraso debe ser >= 0. No modifica salidas EN_CURSO, COMPLETADA
     * o CANCELADA.
     *
     * @param id      identificador de la salida
     * @param request cuerpo con los minutos de retraso
     * @return 200 OK con la salida actualizada
     */
    @PutMapping("/{id}/ajustar-retraso")
    public ResponseEntity<SalidaResponse> ajustarRetraso(
            @PathVariable Long id,
            @Valid @RequestBody AjustarRetrasoRequest request) {
        SalidaResponse response = salidaService.ajustarRetrasoConPropagacion(id, request.getRetrasoMinutos());
        return ResponseEntity.ok(response);
    }
}
