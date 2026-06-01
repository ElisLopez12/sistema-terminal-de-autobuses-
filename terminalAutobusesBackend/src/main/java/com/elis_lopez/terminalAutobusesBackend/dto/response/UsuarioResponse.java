package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.Usuario;
import com.elis_lopez.terminalAutobusesBackend.model.enums.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de salida con los datos de un {@code Usuario}.
 */
@Getter
@Setter
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String username;
    private RolUsuario rol;
    private Long terminalId;
    private String terminalNombre;
    private boolean activo;

    /**
     * Construye un DTO a partir de la entidad {@link Usuario}.
     */
    public static UsuarioResponse fromEntity(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRol(),
                usuario.getTerminal() != null ? usuario.getTerminal().getId() : null,
                usuario.getTerminal() != null ? usuario.getTerminal().getNombre() : null,
                usuario.isActivo()
        );
    }
}
