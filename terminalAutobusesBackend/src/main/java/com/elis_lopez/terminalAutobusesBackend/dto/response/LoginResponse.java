package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de salida con los datos de autenticación de un usuario.
 */
@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String rol;
    private Long terminalId;
    private String terminalNombre;

    /**
     * Construye un DTO a partir de la entidad {@link Usuario} y el token JWT.
     *
     * @param usuario usuario autenticado
     * @param token   token JWT generado
     * @return respuesta de login
     */
    public static LoginResponse from(Usuario usuario, String token) {
        return new LoginResponse(
                token,
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRol().name(),
                usuario.getTerminal() != null ? usuario.getTerminal().getId() : null,
                usuario.getTerminal() != null ? usuario.getTerminal().getNombre() : null
        );
    }
}
