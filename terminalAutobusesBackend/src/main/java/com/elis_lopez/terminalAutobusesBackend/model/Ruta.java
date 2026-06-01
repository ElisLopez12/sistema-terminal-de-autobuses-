package com.elis_lopez.terminalAutobusesBackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ruta de autobús entre un terminal de origen y un destino.
 * <p>
 * El destino puede ser cualquier ubicación (otro terminal, plaza, universidad, etc.),
 * por eso se modela con texto libre en lugar de una FK a {@link Terminal}.
 */
@Entity
@Table(name = "rutas")
@Getter
@Setter
@NoArgsConstructor
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_id", nullable = false)
    private Terminal origen;

    @Column(name = "destino_nombre", nullable = false, length = 200)
    private String destinoNombre;

    @Column(name = "destino_ubicacion", length = 255)
    private String destinoUbicacion;

    @Column(name = "distancia_km")
    private Double distanciaKm;

    @Column(name = "duracion_estimada_min")
    private Integer duracionEstimadaMin;

    @ElementCollection
    @CollectionTable(name = "ruta_paradas", joinColumns = @JoinColumn(name = "ruta_id"))
    @OrderBy("orden")
    private List<ParadaEmbeddable> paradas = new ArrayList<>();

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
