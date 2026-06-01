package com.elis_lopez.terminalAutobusesBackend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para ajustar el retraso de una salida.
 * <p>
 * El retraso se expresa en minutos y se propaga a las salidas
 * siguientes PROGRAMADAS de la misma ruta.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AjustarRetrasoRequest {

    @NotNull(message = "Los minutos de retraso son obligatorios")
    @Min(value = 0, message = "El retraso no puede ser negativo")
    private Integer retrasoMinutos;
}
