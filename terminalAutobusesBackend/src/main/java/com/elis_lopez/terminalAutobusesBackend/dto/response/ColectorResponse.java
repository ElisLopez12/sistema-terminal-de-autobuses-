package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.Colector;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO de salida con los datos de un {@code Colector},
 * incluyendo el autobús al que está asignado (si aplica).
 */
@Getter
@Setter
@AllArgsConstructor
public class ColectorResponse {

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
     * Construye un DTO a partir de la entidad {@link Colector}, sin autobús.
     */
    public static ColectorResponse fromEntity(Colector colector) {
        return new ColectorResponse(
                colector.getId(),
                colector.getNombre(),
                colector.getApellido(),
                colector.getCedula(),
                colector.getTelefono(),
                colector.getFechaNacimiento(),
                colector.getDireccion(),
                colector.getTerminal().getId(),
                colector.getTerminal().getNombre(),
                colector.isActivo(),
                null
        );
    }
}
