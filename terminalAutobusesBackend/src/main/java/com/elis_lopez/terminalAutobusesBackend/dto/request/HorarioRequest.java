package com.elis_lopez.terminalAutobusesBackend.dto.request;

import com.elis_lopez.terminalAutobusesBackend.model.enums.DiaSemana;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

/**
 * DTO de entrada para la creación y actualización de un {@code Horario} plantilla.
 */
@Getter
@Setter
public class HorarioRequest {

    /** ID de la ruta asociada. No puede ser nulo. */
    @NotNull(message = "La ruta es obligatoria")
    private Long rutaId;

    /** ID del terminal de origen. No puede ser nulo. */
    @NotNull(message = "El terminal de origen es obligatorio")
    private Long terminalOrigenId;

    /** Día de la semana en que aplica este horario (usado en edición/un solo día). */
    private DiaSemana diaSemana;

    /** Días de la semana para crear múltiples horarios a la vez (creación únicamente). */
    private List<DiaSemana> diasSemana;

    /** Hora de inicio del intervalo de salidas. No puede ser nulo. Formato HH:mm. */
    @NotNull(message = "La hora de inicio es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    /** Hora de fin del intervalo de salidas. No puede ser nulo. Formato HH:mm. */
    @NotNull(message = "La hora de fin es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    /** Intervalo en minutos entre cada salida. Mínimo 1. */
    @NotNull(message = "El intervalo es obligatorio")
    @Min(value = 1, message = "El intervalo debe ser al menos 1 minuto")
    private Integer intervaloMinutos;

    /** Indica si el horario está activo (por defecto {@code true}). */
    private boolean activo = true;
}
