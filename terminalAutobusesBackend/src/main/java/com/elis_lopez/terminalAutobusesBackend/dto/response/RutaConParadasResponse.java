package com.elis_lopez.terminalAutobusesBackend.dto.response;

import com.elis_lopez.terminalAutobusesBackend.model.Horario;
import com.elis_lopez.terminalAutobusesBackend.model.Ruta;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO de salida que encapsula una {@code Ruta} junto con sus paradas y horarios.
 * <p>
 * Utilizado principalmente por los endpoints públicos para ofrecer una vista
 * completa de la ruta en una sola respuesta.
 */
@Getter
@Setter
@AllArgsConstructor
public class RutaConParadasResponse {

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
    private List<HorarioResponse> horarios;

    /**
     * Construye un DTO compuesto a partir de una {@link Ruta} y sus horarios.
     * Las paradas se obtienen de la colección embebida {@code ruta.paradas}.
     */
    public static RutaConParadasResponse fromEntity(Ruta ruta, List<Horario> horarios) {
        List<ParadaResponse> paradaResponses = ruta.getParadas() != null
                ? ruta.getParadas().stream()
                        .map(p -> ParadaResponse.fromEmbeddable(p, ruta.getId(), ruta.getNombre()))
                        .toList()
                : List.of();
        List<HorarioResponse> horarioResponses = horarios.stream()
                .map(HorarioResponse::fromEntity)
                .toList();

        return new RutaConParadasResponse(
                ruta.getId(),
                ruta.getNombre(),
                ruta.getOrigen().getId(),
                ruta.getOrigen().getNombre(),
                ruta.getDestinoNombre(),
                ruta.getDestinoUbicacion(),
                ruta.getDistanciaKm(),
                ruta.getDuracionEstimadaMin(),
                ruta.isActivo(),
                paradaResponses,
                horarioResponses
        );
    }
}
