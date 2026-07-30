package com.urban_shop.backend.review.service;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.review.dto.request.ReviewCreateRequest;
import com.urban_shop.backend.review.dto.request.ReviewModerationRequest;
import com.urban_shop.backend.review.dto.request.ReviewUpdateRequest;
import com.urban_shop.backend.review.dto.response.ReviewResponse;
import com.urban_shop.backend.review.dto.response.ReviewSummary;

import java.util.UUID;

public interface ReviewService {

    /** Solo puede resenar quien compro el producto y ya lo recibio (pedido DELIVERED). */
    ReviewResponse create(UUID tenantId, UUID customerId, ReviewCreateRequest request);

    ReviewResponse update(UUID tenantId, UUID customerId, UUID reviewId, ReviewUpdateRequest request);

    void delete(UUID tenantId, UUID customerId, UUID reviewId);

    PageResponse<ReviewResponse> listMine(UUID tenantId, UUID customerId, int page, int size);

    /** Resenas visibles de un producto, para la tienda publica. */
    PageResponse<ReviewResponse> listPublic(String tenantSlug, String productSlug, int page, int size);

    ReviewSummary summaryPublic(String tenantSlug, String productSlug);

    PageResponse<ReviewResponse> listAdmin(UUID tenantId, int page, int size);

    /** Oculta o restaura una resena sin borrarla, dejando constancia del motivo. */
    ReviewResponse moderate(UUID tenantId, UUID reviewId, ReviewModerationRequest request);
}
