package com.elis_lopez.terminalAutobusesBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Clase base abstracta con campos comunes para {@link Chofer} y {@link Colector}.
 * <p>
 * No tiene tabla propia — cada subclase concreta mapea sus propios campos a su tabla.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class PersonaBase {

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private boolean activo = true;
}
