package com.elis_lopez.terminalAutobusesBackend.config;

import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuración de tareas programadas (scheduling).
 * <p>
 * Habilita {@code @Scheduled} en la aplicación. La zona horaria
 * ({@code America/Caracas}) se configura mediante el atributo
 * {@code zone} en las anotaciones {@code @Scheduled} de cada tarea.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * {@link ThreadPoolTaskScheduler} con un pool de 2 hilos para
     * ejecutar tareas programadas.
     *
     * @return el scheduler configurado
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        return new ThreadPoolTaskSchedulerBuilder()
                .poolSize(2)
                .threadNamePrefix("scheduled-")
                .build();
    }
}
