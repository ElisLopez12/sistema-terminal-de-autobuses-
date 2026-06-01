package com.elis_lopez.terminalAutobusesBackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para asignar un autobús a una salida.
 * <p>
 * {@code autobusId} es obligatorio y debe corresponder a un autobús activo,
 * con chofer asignado y del mismo terminal que la salida.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsignarAutobusRequest {

    @NotNull(message = "El ID del autobús es obligatorio")
    private Long autobusId;

    /** Si {@code true}, reasigna la ruta del autobús aunque esté en otra. */
    private boolean sobreescribirRuta;
}
