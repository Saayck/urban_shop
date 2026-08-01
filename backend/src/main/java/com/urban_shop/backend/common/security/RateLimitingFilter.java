package com.urban_shop.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urban_shop.backend.common.response.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita por IP los endpoints publicos susceptibles de abuso: intentos de credenciales,
 * altas de cuenta, recuperacion de contrasena y el libro de reclamaciones anonimo.
 * <p>
 * <strong>El contador vive en memoria de cada instancia.</strong> Con N replicas el limite
 * efectivo es N x {@code app.rate-limit.requests-per-minute}. Ajusta la propiedad dividiendo
 * por el numero de replicas, o mueve el contador a Redis si necesitas un limite global exacto.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_SIZE_MILLIS = 60_000;

    /** Cada cuantas peticiones se purgan las ventanas caducadas para que el mapa no crezca sin limite. */
    private static final int CLEANUP_EVERY_N_REQUESTS = 500;

    private static final List<String> RATE_LIMITED_PATTERNS = List.of(
        "/api/auth/login",
        "/api/auth/register-customer",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/api/auth/refresh",
        "/api/store/*/complaints"
    );

    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.requests-per-minute:15}")
    private int maxRequestsPerWindow;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, RequestBucket> ipBuckets = new ConcurrentHashMap<>();
    private final AtomicInteger requestsSinceCleanup = new AtomicInteger();

    private static class RequestBucket {
        final long windowStart;
        final AtomicInteger requestCount;

        RequestBucket(long windowStart) {
            this.windowStart = windowStart;
            this.requestCount = new AtomicInteger(1);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return RATE_LIMITED_PATTERNS.stream().noneMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = extractClientIp(request);
        long now = Instant.now().toEpochMilli();

        RequestBucket bucket = ipBuckets.compute(clientIp, (ip, currentBucket) -> {
            if (currentBucket == null || (now - currentBucket.windowStart) > WINDOW_SIZE_MILLIS) {
                return new RequestBucket(now);
            }
            currentBucket.requestCount.incrementAndGet();
            return currentBucket;
        });

        purgeStaleBucketsPeriodically(now);

        if (bucket.requestCount.get() > maxRequestsPerWindow) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, request.getRequestURI());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiError apiError = ApiError.of(
                HttpStatus.TOO_MANY_REQUESTS,
                "Demasiadas solicitudes. Por favor, intente nuevamente en un minuto.",
                request.getRequestURI()
            );

            objectMapper.writeValue(response.getOutputStream(), apiError);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void purgeStaleBucketsPeriodically(long now) {
        if (requestsSinceCleanup.incrementAndGet() < CLEANUP_EVERY_N_REQUESTS) {
            return;
        }
        requestsSinceCleanup.set(0);
        ipBuckets.entrySet().removeIf(entry -> (now - entry.getValue().windowStart) > WINDOW_SIZE_MILLIS);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
