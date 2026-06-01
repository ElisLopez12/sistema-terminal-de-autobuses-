package com.elis_lopez.terminalAutobusesBackend.dto.request;

import com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de entrada para cambiar el estado de una salida.
 * <p>
 * Solo requiere el nuevo estado; el resto de campos de la salida
 * no se modifican.
 * <p>
 * Cuando el estado es {@code EN_RUTA}, se puede enviar opcionalmente
 * {@code horaReal} para que el sistema calcule automáticamente el retraso
 * y lo propague a las salidas siguientes de la misma ruta.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambiarEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoSalida estado;

    /**
     * Hora real en que ocurrió el cambio (opcional).
     * Se usa solo cuando {@code estado == EN_RUTA} para auto-calcular retraso.
     */
    private LocalDateTime horaReal;
}
