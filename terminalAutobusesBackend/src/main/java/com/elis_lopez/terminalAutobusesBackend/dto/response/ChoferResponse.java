package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.Chofer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO de salida con los datos de un {@code Chofer},
 * incluyendo el autobús al que está asignado (si aplica).
 */
@Getter
@Setter
@AllArgsConstructor
public class ChoferResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String cedula;
    private String telefono;
    private LocalDate fechaNacimiento;
    private String direccion;
    private Long terminalId;
    private String terminalNombre;
    private boolean activo;
    private String autobusAsignado;

    /**
     * Construye un DTO a partir de la entidad {@link Chofer}, sin autobús.
     */
    public static ChoferResponse fromEntity(Chofer chofer) {
        return new ChoferResponse(
                chofer.getId(),
                chofer.getNombre(),
                chofer.getApellido(),
                chofer.getCedula(),
                chofer.getTelefono(),
                chofer.getFechaNacimiento(),
                chofer.getDireccion(),
                chofer.getTerminal().getId(),
                chofer.getTerminal().getNombre(),
                chofer.isActivo(),
                null
        );
    }
}
