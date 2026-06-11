package com.urban_shop.backend.inventory.service;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.inventory.dto.request.InventoryMovementRequest;
import com.urban_shop.backend.inventory.dto.response.InventoryMovementResponse;
import com.urban_shop.backend.inventory.entity.InventoryMovement;
import com.urban_shop.backend.inventory.entity.InventoryMovementType;
import com.urban_shop.backend.inventory.repository.InventoryMovementRepository;
import com.urban_shop.backend.product.entity.Product;
import com.urban_shop.backend.product.entity.ProductStatus;
import com.urban_shop.backend.product.entity.ProductVariant;
import com.urban_shop.backend.product.repository.ProductRepository;
import com.urban_shop.backend.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMovementRepository movementRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public InventoryMovementResponse create(UUID tenantId, InventoryMovementRequest request) {
        ProductVariant variant = requireVariantForUpdate(tenantId, request.variantId());
        int delta = calculateDelta(request.movementType(), request.quantity());
        int newStock;
        try {
            newStock = Math.addExact(variant.getStock(), delta);
        } catch (ArithmeticException ex) {
            throw new BusinessException("El movimiento excede el rango permitido de stock");
        }
        if (newStock < 0) {
            throw new BusinessException("Stock insuficiente para realizar el movimiento");
        }

        variant.setStock(newStock);
        variantRepository.save(variant);
        InventoryMovement movement = saveMovement(
            tenantId,
            variant.getId(),
            request.movementType(),
            delta,
            request.reason()
        );
        synchronizeProductStockStatus(tenantId, variant.getProductId());
        return toResponse(movement, newStock);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryMovementResponse> list(UUID tenantId, int page, int size) {
        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<InventoryMovementResponse> result = movementRepository.findAllByTenantId(tenantId, pageable)
            .map(movement -> toResponse(movement, null));
        return PageResponse.from(result);
    }

    @Override
    @Transactional
    public void recordInitialStock(UUID tenantId, UUID variantId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        ProductVariant variant = requireVariantForUpdate(tenantId, variantId);
        variant.setStock(quantity);
        variantRepository.save(variant);
        saveMovement(
            tenantId,
            variantId,
            InventoryMovementType.ENTRY,
            quantity,
            "Stock inicial de variante"
        );
        synchronizeProductStockStatus(tenantId, variant.getProductId());
    }

    private int calculateDelta(InventoryMovementType type, int quantity) {
        if (quantity == 0) {
            throw new BusinessException("La cantidad del movimiento no puede ser cero");
        }
        return switch (type) {
            case ENTRY, RETURN -> requirePositive(quantity);
            case EXIT, SALE -> -requirePositive(quantity);
            case ADJUSTMENT -> quantity;
        };
    }

    private int requirePositive(int quantity) {
        if (quantity < 0) {
            throw new BusinessException("La cantidad debe ser positiva para este tipo de movimiento");
        }
        return quantity;
    }

    private ProductVariant requireVariantForUpdate(UUID tenantId, UUID variantId) {
        return variantRepository.findByTenantIdAndIdForUpdate(tenantId, variantId)
            .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada"));
    }

    private InventoryMovement saveMovement(
        UUID tenantId,
        UUID variantId,
        InventoryMovementType type,
        int quantity,
        String reason
    ) {
        InventoryMovement movement = new InventoryMovement();
        movement.setTenantId(tenantId);
        movement.setVariantId(variantId);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        return movementRepository.save(movement);
    }

    private void synchronizeProductStockStatus(UUID tenantId, UUID productId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        int totalStock = variantRepository.findAllByProductIdOrderBySizeAscColorAsc(productId).stream()
            .filter(ProductVariant::isActive)
            .mapToInt(ProductVariant::getStock)
            .sum();

        if (product.getStatus() == ProductStatus.ACTIVE && totalStock == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
            productRepository.save(product);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK && totalStock > 0) {
            product.setStatus(ProductStatus.ACTIVE);
            productRepository.save(product);
        }
    }

    private InventoryMovementResponse toResponse(InventoryMovement movement, Integer stockAfter) {
        return new InventoryMovementResponse(
            movement.getId(),
            movement.getVariantId(),
            movement.getMovementType(),
            movement.getQuantity(),
            stockAfter,
            movement.getReason(),
            movement.getCreatedAt()
        );
    }
}
