package com.elis_lopez.terminalAutobusesBackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Autobús registrado en un {@link Terminal}.
 * <p>
 * Pertenece a un único terminal (su base). Puede tener un chofer, un colector
 * y una ruta asignados de forma opcional — si no está en servicio activo.
 * <p>
 * El número de unidad (ej. "106") no es único globalmente; pueden existir
 * autobuses con el mismo número en diferentes terminales.
 */
@Entity
@Table(name = "autobuses")
@Getter
@Setter
@NoArgsConstructor
public class Autobus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_unidad", nullable = false, length = 20)
    private String numeroUnidad;

    @Column(nullable = false, length = 20)
    private String matricula;

    @Column(length = 50)
    private String marca;

    @Column(length = 50)
    private String modelo;

    @Column
    private Integer anio;

    @Column(name = "capacidad_pasajeros")
    private Integer capacidadPasajeros;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private Terminal terminal;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chofer_id")
    private Chofer chofer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colector_id")
    private Colector colector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

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
