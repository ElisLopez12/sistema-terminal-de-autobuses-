package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.ParadaEmbeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de salida con los datos de una {@code Parada}.
 */
@Getter
@Setter
@AllArgsConstructor
public class ParadaResponse {

    private Long id;
    private Long rutaId;
    private String rutaNombre;
    private String nombre;
    private Integer orden;

    /**
     * Construye un DTO a partir de un {@link ParadaEmbeddable} en el contexto
     * de una ruta.
     */
    public static ParadaResponse fromEmbeddable(ParadaEmbeddable p, Long rutaId, String rutaNombre) {
        return new ParadaResponse(
                null,
                rutaId,
                rutaNombre,
                p.getNombre(),
                p.getOrden()
        );
    }
}
