package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.Terminal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de salida con los datos de un {@code Terminal}.
 */
@Getter
@Setter
@AllArgsConstructor
public class TerminalResponse {

    private Long id;
    private String nombre;
    private String ubicacion;
    private boolean activo;

    /**
     * Construye un DTO a partir de la entidad {@link Terminal}.
     */
    public static TerminalResponse fromEntity(Terminal terminal) {
        return new TerminalResponse(
                terminal.getId(),
                terminal.getNombre(),
                terminal.getUbicacion(),
                terminal.isActivo()
        );
    }
}
