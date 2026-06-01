package com.elis_lopez.terminalAutobusesBackend.dto.request;

import com.elis_lopez.terminalAutobusesBackend.model.ParadaEmbeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO de entrada para la creación y actualización de una {@code Ruta}.
 */
@Getter
@Setter
public class RutaRequest {

    /** Nombre descriptivo de la ruta. No puede estar vacío. */
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /** ID del terminal de origen. No puede ser nulo. */
    @NotNull(message = "El terminal de origen es obligatorio")
    private Long origenId;

    /** Nombre del destino (texto libre: puede ser una plaza, universidad, etc.). No puede estar vacío. */
    @NotBlank(message = "El nombre del destino es obligatorio")
    private String destinoNombre;

    /** Ubicación o dirección del destino (opcional). */
    private String destinoUbicacion;

    /** Distancia en kilómetros (opcional). */
    private Double distanciaKm;

    /** Duración estimada del viaje en minutos (opcional). */
    private Integer duracionEstimadaMin;

    /** Indica si la ruta está activa (por defecto {@code true}). */
    private boolean activo = true;

    /** Lista de paradas intermedias de la ruta. */
    private List<ParadaEmbeddable> paradas;
}
