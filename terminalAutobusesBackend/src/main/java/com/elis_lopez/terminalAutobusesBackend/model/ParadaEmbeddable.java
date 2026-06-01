package com.elis_lopez.terminalAutobusesBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parada intermedia de una {@link Ruta}, almacenada como colección embebida.
 * <p>
 * A diferencia de la antigua entidad {@code Parada}, esta clase no tiene
 * identidad propia — vive exclusivamente dentro de su ruta padre y se
 * gestiona al crear o editar la ruta.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParadaEmbeddable {

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false)
    private Integer orden;
}
