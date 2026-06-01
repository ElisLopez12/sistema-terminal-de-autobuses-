package com.elis_lopez.terminalAutobusesBackend.config;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Configuración de seguridad para el entorno de pruebas.
 * <p>
 * Reutiliza la configuración de seguridad de la aplicación ({@link SecurityConfig})
 * con el JWT secret definido en {@code application-test.properties}.
 * No se requiere ninguna sobreescritura adicional — la configuración base
 * funciona correctamente con H2 en memoria.
 */
@TestConfiguration
public class TestSecurityConfig {
    // La configuración de seguridad de la aplicación (SecurityConfig) se
    // aplica automáticamente. Esta clase solo marca un perfil de prueba
    // para que los test slices puedan referenciarla si es necesario.
}
