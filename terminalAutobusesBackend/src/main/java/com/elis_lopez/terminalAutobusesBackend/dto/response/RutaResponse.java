package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.Ruta;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO de salida con los datos de una {@code Ruta}.
 */
@Getter
@Setter
@AllArgsConstructor
public class RutaResponse {

    private Long id;
    private String nombre;
    private Long origenId;
    private String origenNombre;
    private String destinoNombre;
    private String destinoUbicacion;
    private Double distanciaKm;
    private Integer duracionEstimadaMin;
    private boolean activo;
    private List<ParadaResponse> paradas;

    /**
     * Construye un DTO a partir de la entidad {@link Ruta}.
     * <p>
     * Las paradas se obtienen de la colección embebida {@code ruta.paradas}
     * y se incluyen siempre en la respuesta.
     */
    public static RutaResponse fromEntity(Ruta ruta) {
        List<ParadaResponse> paradaResponses = ruta.getParadas() != null
                ? ruta.getParadas().stream()
                        .map(p -> ParadaResponse.fromEmbeddable(p, ruta.getId(), ruta.getNombre()))
                        .toList()
                : List.of();

        return new RutaResponse(
                ruta.getId(),
                ruta.getNombre(),
                ruta.getOrigen().getId(),
                ruta.getOrigen().getNombre(),
                ruta.getDestinoNombre(),
                ruta.getDestinoUbicacion(),
                ruta.getDistanciaKm(),
                ruta.getDuracionEstimadaMin(),
                ruta.isActivo(),
                paradaResponses
        );
    }
}
