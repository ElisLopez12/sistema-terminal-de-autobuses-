package com.elis_lopez.terminalAutobusesBackend.dto.request;

import com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de entrada para la creación y actualización de una {@code Salida}.
 */
@Getter
@Setter
public class SalidaRequest {

    /** ID de la ruta del viaje. No puede ser nulo. */
    @NotNull(message = "La ruta es obligatoria")
    private Long rutaId;

    /** ID del horario plantilla asociado (opcional). */
    private Long horarioId;

    /** ID del terminal de origen. No puede ser nulo. Debe coincidir con el origen de la ruta. */
    @NotNull(message = "El terminal de origen es obligatorio")
    private Long terminalOrigenId;

    /** ID del autobús asignado (opcional). Debe estar activo. */
    private Long autobusId;

    /** Fecha y hora programada de salida. No puede ser nulo. Formato yyyy-MM-dd'T'HH:mm. */
    @NotNull(message = "La hora programada es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horaProgramada;

    /** Retraso en minutos (por defecto 0). */
    private Integer retrasoMinutos = 0;

    /** Estado de la salida (por defecto PROGRAMADA). */
    private EstadoSalida estado = EstadoSalida.PROGRAMADA;

    /** Indica si la salida está activa (por defecto {@code true}). */
    private boolean activo = true;
}
