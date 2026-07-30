package com.urban_shop.backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas (purga de tokens) y la ejecucion asincrona
 * (envio de correos transaccionales fuera del hilo de la peticion).
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}
