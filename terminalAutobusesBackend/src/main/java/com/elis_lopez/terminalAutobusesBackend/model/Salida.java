package com.elis_lopez.terminalAutobusesBackend.model;

import com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Salida individual de un autobús — representa un viaje real.
 * <p>
 * El administrador la crea manualmente (o usando {@link Horario} como plantilla),
 * asigna el autobús y ajusta el estado según la operación real.
 * <p>
 * La hora que se muestra al público es: {@code horaProgramada + retrasoMinutos}.
 */
@Entity
@Table(name = "salidas")
@Getter
@Setter
@NoArgsConstructor
public class Salida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_id")
    private Horario horario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_origen_id", nullable = false)
    private Terminal terminalOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autobus_id")
    private Autobus autobus;

    @Column(name = "hora_programada", nullable = false)
    private LocalDateTime horaProgramada;

    @Column(name = "retraso_minutos", nullable = false)
    private Integer retrasoMinutos = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSalida estado = EstadoSalida.PROGRAMADA;

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
