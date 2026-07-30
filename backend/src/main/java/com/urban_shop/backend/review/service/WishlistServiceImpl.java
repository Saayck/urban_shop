package com.urban_shop.backend.review.service;

import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.product.entity.Product;
import com.urban_shop.backend.product.entity.ProductImage;
import com.urban_shop.backend.product.entity.ProductStatus;
import com.urban_shop.backend.product.repository.ProductImageRepository;
import com.urban_shop.backend.product.repository.ProductRepository;
import com.urban_shop.backend.review.dto.response.WishlistItemResponse;
import com.urban_shop.backend.review.entity.WishlistItem;
import com.urban_shop.backend.review.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private static final String PRODUCT_NOT_FOUND = "Producto no encontrado";

    private final WishlistItemRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> list(UUID tenantId, UUID customerId) {
        List<WishlistItem> items =
            wishlistRepository.findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId);
        if (items.isEmpty()) {
            return List.of();
        }

        // Productos e imagenes en dos consultas, no una por item.
        List<UUID> productIds = items.stream().map(WishlistItem::getProductId).toList();
        Map<UUID, Product> products = productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<UUID, String> mainImages = imageRepository
            .findAllByProductIdInOrderByDisplayOrderAscCreatedAtAsc(productIds).stream()
            .collect(Collectors.toMap(
                ProductImage::getProductId,
                ProductImage::getImageUrl,
                (first, second) -> first
            ));

        return items.stream()
            .map(item -> toResponse(item, products.get(item.getProductId()), mainImages.get(item.getProductId())))
            .toList();
    }

    @Override
    @Transactional
    public WishlistItemResponse add(UUID tenantId, UUID customerId, UUID productId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND));

        WishlistItem item = wishlistRepository
            .findByTenantIdAndCustomerIdAndProductId(tenantId, customerId, productId)
            .orElseGet(() -> {
                WishlistItem created = new WishlistItem();
                created.setTenantId(tenantId);
                created.setCustomerId(customerId);
                created.setProductId(productId);
                return wishlistRepository.save(created);
            });

        return toResponse(item, product, mainImageUrl(productId));
    }

    @Override
    @Transactional
    public void remove(UUID tenantId, UUID customerId, UUID productId) {
        wishlistRepository.findByTenantIdAndCustomerIdAndProductId(tenantId, customerId, productId)
            .ifPresent(wishlistRepository::delete);
    }

    private String mainImageUrl(UUID productId) {
        return imageRepository.findAllByProductIdOrderByDisplayOrderAscCreatedAtAsc(productId).stream()
            .min(Comparator.comparing(ProductImage::isMain).reversed())
            .map(ProductImage::getImageUrl)
            .orElse(null);
    }

    private WishlistItemResponse toResponse(WishlistItem item, Product product, String imageUrl) {
        boolean available = product != null && product.getStatus() == ProductStatus.ACTIVE;
        return new WishlistItemResponse(
            item.getId(),
            item.getProductId(),
            product == null ? null : product.getName(),
            product == null ? null : product.getSlug(),
            product == null ? null : currentPrice(product),
            imageUrl,
            available,
            item.getCreatedAt()
        );
    }

    private java.math.BigDecimal currentPrice(Product product) {
        return product.getSalePrice() == null ? product.getBasePrice() : product.getSalePrice();
    }
}
