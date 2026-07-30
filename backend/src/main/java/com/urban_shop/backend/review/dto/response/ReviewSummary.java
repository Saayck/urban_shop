package com.urban_shop.backend.review.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Agregado de valoraciones visibles de un producto. */
public record ReviewSummary(long totalReviews, BigDecimal averageRating) {

    /** Constructor que usa la consulta JPQL: avg() devuelve Double. */
    public ReviewSummary(long totalReviews, Double averageRating) {
        this(
            totalReviews,
            averageRating == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP)
        );
    }
}
