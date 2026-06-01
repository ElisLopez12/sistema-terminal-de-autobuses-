package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.Salida;
import com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de salida con los datos de una {@code Salida} (viaje individual).
 */
@Getter
@Setter
@AllArgsConstructor
public class SalidaResponse {

    private Long id;
    private Long rutaId;
    private String rutaNombre;
    private Long horarioId;
    private Long terminalOrigenId;
    private String terminalOrigenNombre;
    private Long autobusId;
    private String autobusNumeroUnidad;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horaProgramada;

    private Integer retrasoMinutos;
    private EstadoSalida estado;
    private boolean activo;

    /**
     * Hora real de salida, calculada como {@code horaProgramada + retrasoMinutos}.
     * Es la hora que se muestra al público.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horaReal;

    /**
     * Indica si la salida tiene un autobús asignado ({@code true} si {@code autobusId != null}).
     */
    private boolean tieneAutobus;

    /**
     * Construye un DTO a partir de la entidad {@link Salida}.
     */
    public static SalidaResponse fromEntity(Salida salida) {
        LocalDateTime horaReal = salida.getHoraProgramada().plusMinutes(
                salida.getRetrasoMinutos() != null ? salida.getRetrasoMinutos() : 0
        );
        boolean tieneAutobus = salida.getAutobus() != null;
        return new SalidaResponse(
                salida.getId(),
                salida.getRuta().getId(),
                salida.getRuta().getNombre(),
                salida.getHorario() != null ? salida.getHorario().getId() : null,
                salida.getTerminalOrigen().getId(),
                salida.getTerminalOrigen().getNombre(),
                salida.getAutobus() != null ? salida.getAutobus().getId() : null,
                salida.getAutobus() != null ? salida.getAutobus().getNumeroUnidad() : null,
                salida.getHoraProgramada(),
                salida.getRetrasoMinutos(),
                salida.getEstado(),
                salida.isActivo(),
                horaReal,
                tieneAutobus
        );
    }
}
