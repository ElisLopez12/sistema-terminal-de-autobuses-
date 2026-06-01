package com.elis_lopez.terminalAutobusesBackend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de entrada para la creación y actualización de un {@code Autobus}.
 */
@Getter
@Setter
public class AutobusRequest {

    /** Número de unidad del autobús. No puede estar vacío. */
    @NotBlank(message = "El número de unidad es obligatorio")
    private String numeroUnidad;

    /** Matrícula/placa del autobús. No puede estar vacía. */
    @NotBlank(message = "La matrícula es obligatoria")
    private String matricula;

    /** Marca del autobús (opcional). */
    private String marca;

    /** Modelo del autobús (opcional). */
    private String modelo;

    /** Año de fabricación. Debe ser mayor o igual a 1900. */
    @Min(value = 1900, message = "El año debe ser mayor o igual a 1900")
    private Integer anio;

    /** Capacidad máxima de pasajeros. Debe ser al menos 1. */
    @Min(value = 1, message = "La capacidad debe ser al menos 1")
    private Integer capacidadPasajeros;

    /** ID del terminal al que pertenece el autobús. No puede ser nulo. */
    @NotNull(message = "El terminal es obligatorio")
    private Long terminalId;

    /** ID del chofer asignado (opcional). */
    private Long choferId;

    /** ID del colector asignado (opcional). */
    private Long colectorId;

    /** ID de la ruta asignada (opcional). Debe pertenecer al mismo terminal. */
    private Long rutaId;

    /** Indica si el autobús está activo (por defecto {@code true}). */
    private boolean activo = true;
}
