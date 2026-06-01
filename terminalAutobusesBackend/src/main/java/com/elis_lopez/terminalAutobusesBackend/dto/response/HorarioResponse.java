package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.Horario;
import com.elis_lopez.terminalAutobusesBackend.model.enums.DiaSemana;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * DTO de salida con los datos de un {@code Horario} plantilla.
 */
@Getter
@Setter
@AllArgsConstructor
public class HorarioResponse {

    private Long id;
    private Long rutaId;
    private String rutaNombre;
    private Long terminalOrigenId;
    private String terminalOrigenNombre;
    private DiaSemana diaSemana;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    private Integer intervaloMinutos;
    private boolean activo;

    /**
     * Construye un DTO a partir de la entidad {@link Horario}.
     */
    public static HorarioResponse fromEntity(Horario horario) {
        return new HorarioResponse(
                horario.getId(),
                horario.getRuta().getId(),
                horario.getRuta().getNombre(),
                horario.getTerminalOrigen().getId(),
                horario.getTerminalOrigen().getNombre(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getIntervaloMinutos(),
                horario.isActivo()
        );
    }
}
