package com.elis_lopez.terminalAutobusesBackend.model.enums;
/**
 * Estado de una salida (viaje individual de un autobús).
 */
public enum EstadoSalida {
    PROGRAMADA,
    ABORDAJE,
    EN_RUTA,    // antes EN_CURSO / COMPLETADA — salió del terminal
    CANCELADA,
    EN_ESPERA   // legacy — no se asigna desde UI
}
