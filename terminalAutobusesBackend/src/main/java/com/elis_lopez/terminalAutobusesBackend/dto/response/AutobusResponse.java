package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.Autobus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de salida con los datos completos de un {@code Autobus},
 * incluyendo información desnormalizada del terminal, chofer, colector y ruta.
 */
@Getter
@Setter
@AllArgsConstructor
public class AutobusResponse {

    private Long id;
    private String numeroUnidad;
    private String matricula;
    private String marca;
    private String modelo;
    private Integer anio;
    private Integer capacidadPasajeros;
    private Long terminalId;
    private String terminalNombre;
    private Long choferId;
    private String choferNombre;
    private Long colectorId;
    private String colectorNombre;
    private Long rutaId;
    private String rutaNombre;
    private boolean activo;

    /**
     * Construye un DTO a partir de la entidad {@link Autobus},
     * concatenando nombre y apellido de chofer y colector cuando existen.
     */
    public static AutobusResponse fromEntity(Autobus autobus) {
        String choferNombre = autobus.getChofer() != null
                ? autobus.getChofer().getNombre() + " " + autobus.getChofer().getApellido()
                : null;
        String colectorNombre = autobus.getColector() != null
                ? autobus.getColector().getNombre() + " " + autobus.getColector().getApellido()
                : null;

        return new AutobusResponse(
                autobus.getId(),
                autobus.getNumeroUnidad(),
                autobus.getMatricula(),
                autobus.getMarca(),
                autobus.getModelo(),
                autobus.getAnio(),
                autobus.getCapacidadPasajeros(),
                autobus.getTerminal().getId(),
                autobus.getTerminal().getNombre(),
                autobus.getChofer() != null ? autobus.getChofer().getId() : null,
                choferNombre,
                autobus.getColector() != null ? autobus.getColector().getId() : null,
                colectorNombre,
                autobus.getRuta() != null ? autobus.getRuta().getId() : null,
                autobus.getRuta() != null ? autobus.getRuta().getNombre() : null,
                autobus.isActivo()
        );
    }
}
