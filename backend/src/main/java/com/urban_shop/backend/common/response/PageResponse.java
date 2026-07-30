package com.urban_shop.backend.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
            source.getContent(),
            source.getNumber(),
            source.getSize(),
            source.getTotalElements(),
            source.getTotalPages()
        );
    }

    /**
     * Copia la paginacion conservando los metadatos pero sustituyendo el contenido.
     * Util cuando los elementos se enriquecen despues, con una consulta agregada.
     */
    public <R> PageResponse<R> withContent(List<R> replacement) {
        return new PageResponse<>(replacement, page, size, totalElements, totalPages);
    }
}
