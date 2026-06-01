package com.elis_lopez.terminalAutobusesBackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO de entrada para la creación y actualización de un {@code Chofer}.
 */
@Getter
@Setter
public class ChoferRequest {

    /** Nombre del chofer. No puede estar vacío. */
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /** Apellido del chofer. No puede estar vacío. */
    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    /** Cédula de identidad. No puede estar vacía. */
    @NotBlank(message = "La cédula es obligatoria")
    private String cedula;

    /** Número de teléfono. No puede estar vacío. */
    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    /** Fecha de nacimiento (opcional). */
    private LocalDate fechaNacimiento;

    /** Dirección de residencia (opcional). */
    private String direccion;

    /** ID del terminal al que pertenece el chofer. No puede ser nulo. */
    @NotNull(message = "El terminal es obligatorio")
    private Long terminalId;

    /** Indica si el chofer está activo (por defecto {@code true}). */
    private boolean activo = true;
}
