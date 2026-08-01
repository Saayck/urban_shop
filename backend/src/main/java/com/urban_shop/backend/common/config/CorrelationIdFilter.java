package com.urban_shop.backend.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Asigna un identificador a cada peticion y lo publica en el MDC, de modo que todas las
 * lineas de log de una misma peticion se puedan correlacionar.
 * <p>
 * Respeta el {@code X-Correlation-Id} entrante si el balanceador o el frontend ya lo
 * enviaron, y siempre lo devuelve en la respuesta para que el cliente pueda citarlo al
 * reportar un problema.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final int MAX_LENGTH = 64;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = sanitize(request.getHeader(HEADER));
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Imprescindible: los hilos se reutilizan y el valor se filtraria a otra peticion.
            MDC.remove(MDC_KEY);
        }
    }

    /** No se confia en el valor entrante: podria inyectar saltos de linea en el log. */
    private String sanitize(String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String cleaned = incoming.replaceAll("[^A-Za-z0-9._-]", "");
        if (cleaned.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) : cleaned;
    }
}
