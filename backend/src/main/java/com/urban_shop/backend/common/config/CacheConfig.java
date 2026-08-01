package com.urban_shop.backend.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Caché de lectura del escaparate.
 * <p>
 * Sustituye a {@code ConcurrentMapCacheManager}, que no tenía ni TTL ni tope de entradas:
 * la clave de {@code storeProducts} incluye filtros y paginación, así que un rastreador
 * recorriendo el catálogo generaba entradas ilimitadas que nunca caducaban.
 * <p>
 * El TTL cumple además una segunda función: la invalidación por {@code @CacheEvict} solo
 * afecta a la JVM local, de modo que con varias instancias una podría servir catálogo
 * obsoleto. Con caducidad por escritura esa ventana queda acotada al TTL en lugar de ser
 * indefinida. Si se despliega con varias réplicas y esa ventana no es aceptable, basta con
 * cambiar este {@code CacheManager} por uno respaldado en Redis: el resto del código usa la
 * abstracción de caché de Spring y no necesita tocarse.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final List<String> CACHE_NAMES = List.of(
        "storeProducts",
        "storeCategories",
        "storeTemplates"
    );

    @Value("${app.cache.ttl-seconds:120}")
    private long ttlSeconds;

    @Value("${app.cache.max-entries:2000}")
    private long maxEntries;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(CACHE_NAMES);
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxEntries)
                .recordStats()
        );
        return cacheManager;
    }
}
