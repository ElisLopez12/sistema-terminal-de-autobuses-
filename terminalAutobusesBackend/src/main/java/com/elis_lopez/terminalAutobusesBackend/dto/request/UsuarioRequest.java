package com.elis_lopez.terminalAutobusesBackend.dto.request;

import com.elis_lopez.terminalAutobusesBackend.model.enums.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de entrada para la creación y actualización de un {@code Usuario}.
 */
@Getter
@Setter
public class UsuarioRequest {

    /** Nombre de usuario único. No puede estar vacío. */
    @NotBlank(message = "El username es obligatorio")
    private String username;

    /** Contraseña del usuario. Mínimo 6 caracteres. */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    /** Rol asignado al usuario (ADMIN, OPERADOR, etc.). No puede ser nulo. */
    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    /** ID del terminal al que pertenece el usuario (opcional). */
    private Long terminalId;
}
