package com.urban_shop.backend.product.service;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.product.dto.request.ProductCreateRequest;
import com.urban_shop.backend.product.dto.request.ProductImageRequest;
import com.urban_shop.backend.product.dto.request.ProductUpdateRequest;
import com.urban_shop.backend.product.dto.request.ProductVariantCreateRequest;
import com.urban_shop.backend.product.dto.request.ProductVariantUpdateRequest;
import com.urban_shop.backend.product.dto.request.SizeGuideRequest;
import com.urban_shop.backend.product.dto.response.ProductDetailResponse;
import com.urban_shop.backend.product.dto.response.ProductImageResponse;
import com.urban_shop.backend.product.dto.response.ProductResponse;
import com.urban_shop.backend.product.dto.response.ProductVariantResponse;
import com.urban_shop.backend.product.dto.response.SizeGuideResponse;
import com.urban_shop.backend.product.entity.ProductStatus;

import java.util.UUID;

public interface ProductService {

    ProductDetailResponse create(UUID tenantId, ProductCreateRequest request);

    PageResponse<ProductResponse> listAdmin(
        UUID tenantId,
        ProductStatus status,
        UUID brandId,
        UUID categoryId,
        int page,
        int size
    );

    ProductDetailResponse getAdmin(UUID tenantId, UUID productId);

    ProductDetailResponse update(UUID tenantId, UUID productId, ProductUpdateRequest request);

    ProductDetailResponse updateStatus(UUID tenantId, UUID productId, ProductStatus status);

    ProductImageResponse addImage(UUID tenantId, UUID productId, ProductImageRequest request);

    void deleteImage(UUID tenantId, UUID productId, UUID imageId);

    ProductVariantResponse addVariant(UUID tenantId, UUID productId, ProductVariantCreateRequest request);

    ProductVariantResponse updateVariant(
        UUID tenantId,
        UUID productId,
        UUID variantId,
        ProductVariantUpdateRequest request
    );

    SizeGuideResponse addSizeGuide(UUID tenantId, UUID productId, SizeGuideRequest request);

    SizeGuideResponse updateSizeGuide(
        UUID tenantId,
        UUID productId,
        UUID guideId,
        SizeGuideRequest request
    );

    void deleteSizeGuide(UUID tenantId, UUID productId, UUID guideId);

    PageResponse<ProductResponse> listPublic(
        String tenantSlug,
        UUID brandId,
        UUID categoryId,
        int page,
        int size
    );

    ProductDetailResponse getPublic(String tenantSlug, String productSlug);
}
