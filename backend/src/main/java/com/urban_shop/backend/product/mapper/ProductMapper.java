package com.urban_shop.backend.product.mapper;

import com.urban_shop.backend.product.dto.response.ProductDetailResponse;
import com.urban_shop.backend.product.dto.response.ProductImageResponse;
import com.urban_shop.backend.product.dto.response.ProductResponse;
import com.urban_shop.backend.product.dto.response.ProductVariantResponse;
import com.urban_shop.backend.product.dto.response.SizeGuideResponse;
import com.urban_shop.backend.product.entity.Product;
import com.urban_shop.backend.product.entity.ProductImage;
import com.urban_shop.backend.product.entity.ProductVariant;
import com.urban_shop.backend.product.entity.SizeGuide;

import java.math.BigDecimal;
import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(
        Product product,
        String brandName,
        String categoryName,
        String mainImageUrl,
        int totalStock
    ) {
        return new ProductResponse(
            product.getId(),
            product.getBrandId(),
            brandName,
            product.getCategoryId(),
            categoryName,
            product.getName(),
            product.getSlug(),
            product.getBasePrice(),
            product.getSalePrice(),
            effectivePrice(product),
            product.getStatus(),
            product.isFeatured(),
            product.isNewProduct(),
            mainImageUrl,
            totalStock,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    public static ProductDetailResponse toDetail(
        Product product,
        String brandName,
        String categoryName,
        List<ProductImage> images,
        List<ProductVariant> variants,
        List<SizeGuide> sizeGuides
    ) {
        int totalStock = variants.stream()
            .filter(ProductVariant::isActive)
            .mapToInt(ProductVariant::getStock)
            .sum();
        return new ProductDetailResponse(
            product.getId(),
            product.getBrandId(),
            brandName,
            product.getCategoryId(),
            categoryName,
            product.getName(),
            product.getSlug(),
            product.getDescription(),
            product.getMaterial(),
            product.getFitType(),
            product.getBasePrice(),
            product.getSalePrice(),
            effectivePrice(product),
            product.getStatus(),
            product.isFeatured(),
            product.isNewProduct(),
            totalStock,
            images.stream().map(ProductMapper::toImage).toList(),
            variants.stream().map(ProductMapper::toVariant).toList(),
            sizeGuides.stream().map(ProductMapper::toSizeGuide).toList(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    public static ProductImageResponse toImage(ProductImage image) {
        return new ProductImageResponse(
            image.getId(),
            image.getImageUrl(),
            image.getAltText(),
            image.getDisplayOrder(),
            image.isMain()
        );
    }

    public static ProductVariantResponse toVariant(ProductVariant variant) {
        return new ProductVariantResponse(
            variant.getId(),
            variant.getSku(),
            variant.getSize(),
            variant.getColor(),
            variant.getColorHex(),
            variant.getStock(),
            variant.getPrice(),
            variant.isActive()
        );
    }

    public static SizeGuideResponse toSizeGuide(SizeGuide guide) {
        return new SizeGuideResponse(
            guide.getId(),
            guide.getSize(),
            guide.getChestCm(),
            guide.getWaistCm(),
            guide.getHipCm(),
            guide.getLengthCm(),
            guide.getShoulderCm(),
            guide.getSleeveCm(),
            guide.getInseamCm(),
            guide.getNotes()
        );
    }

    private static BigDecimal effectivePrice(Product product) {
        return product.getSalePrice() == null ? product.getBasePrice() : product.getSalePrice();
    }
}
