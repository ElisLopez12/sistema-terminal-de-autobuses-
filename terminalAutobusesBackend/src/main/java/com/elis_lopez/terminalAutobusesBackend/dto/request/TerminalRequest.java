package com.elis_lopez.terminalAutobusesBackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de entrada para la creación y actualización de un {@code Terminal}.
 */
@Getter
@Setter
public class TerminalRequest {

    /** Nombre del terminal. No puede estar vacío. */
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /** Dirección o ubicación geográfica del terminal. */
    private String ubicacion;

    /** Indica si el terminal está activo (por defecto {@code true}). */
    private boolean activo = true;
}
