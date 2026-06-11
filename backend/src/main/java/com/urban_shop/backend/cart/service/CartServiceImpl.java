package com.urban_shop.backend.cart.service;

import com.urban_shop.backend.cart.dto.request.CartItemCreateRequest;
import com.urban_shop.backend.cart.dto.request.CartItemUpdateRequest;
import com.urban_shop.backend.cart.dto.response.CartItemResponse;
import com.urban_shop.backend.cart.dto.response.CartResponse;
import com.urban_shop.backend.cart.entity.Cart;
import com.urban_shop.backend.cart.entity.CartItem;
import com.urban_shop.backend.cart.entity.CartStatus;
import com.urban_shop.backend.cart.repository.CartItemRepository;
import com.urban_shop.backend.cart.repository.CartRepository;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.repository.CustomerRepository;
import com.urban_shop.backend.product.entity.Product;
import com.urban_shop.backend.product.entity.ProductImage;
import com.urban_shop.backend.product.entity.ProductStatus;
import com.urban_shop.backend.product.entity.ProductVariant;
import com.urban_shop.backend.product.repository.ProductImageRepository;
import com.urban_shop.backend.product.repository.ProductRepository;
import com.urban_shop.backend.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository itemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;

    @Override
    @Transactional
    public CartResponse get(UUID tenantId, UUID customerId) {
        Cart cart = requireActiveCart(tenantId, customerId);
        return buildResponse(tenantId, cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID tenantId, UUID customerId, CartItemCreateRequest request) {
        Cart cart = requireActiveCart(tenantId, customerId);
        ProductVariant variant = requirePurchasableVariant(tenantId, request.variantId());
        Product product = requirePurchasableProduct(tenantId, variant.getProductId());

        CartItem item = itemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId())
            .orElseGet(() -> newCartItem(tenantId, cart, product, variant));
        int requestedQuantity;
        try {
            requestedQuantity = Math.addExact(item.getQuantity(), request.quantity());
        } catch (ArithmeticException ex) {
            throw new BusinessException("La cantidad solicitada excede el limite permitido");
        }
        validateStock(variant, requestedQuantity);

        item.setQuantity(requestedQuantity);
        item.setUnitPrice(currentPrice(product, variant));
        itemRepository.save(item);
        touch(cart);
        return buildResponse(tenantId, cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(
        UUID tenantId,
        UUID customerId,
        UUID itemId,
        CartItemUpdateRequest request
    ) {
        Cart cart = requireActiveCart(tenantId, customerId);
        CartItem item = itemRepository.findByIdAndCartId(itemId, cart.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Item de carrito no encontrado"));
        ProductVariant variant = requirePurchasableVariant(tenantId, item.getVariantId());
        Product product = requirePurchasableProduct(tenantId, item.getProductId());
        if (!variant.getProductId().equals(product.getId())) {
            throw new BusinessException("La variante no pertenece al producto del carrito");
        }
        validateStock(variant, request.quantity());

        item.setQuantity(request.quantity());
        item.setUnitPrice(currentPrice(product, variant));
        itemRepository.save(item);
        touch(cart);
        return buildResponse(tenantId, cart);
    }

    @Override
    @Transactional
    public CartResponse deleteItem(UUID tenantId, UUID customerId, UUID itemId) {
        Cart cart = requireActiveCart(tenantId, customerId);
        CartItem item = itemRepository.findByIdAndCartId(itemId, cart.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Item de carrito no encontrado"));
        itemRepository.delete(item);
        itemRepository.flush();
        touch(cart);
        return buildResponse(tenantId, cart);
    }

    private Cart requireActiveCart(UUID tenantId, UUID customerId) {
        Customer customer = customerRepository.findByTenantIdAndIdForUpdate(tenantId, customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        if (!customer.isActive()) {
            throw new BusinessException("El cliente no esta activo");
        }
        return cartRepository.findByTenantIdAndCustomerIdAndStatus(
                tenantId,
                customerId,
                CartStatus.ACTIVE
            )
            .orElseGet(() -> {
                Cart cart = new Cart();
                cart.setTenantId(tenantId);
                cart.setCustomerId(customerId);
                return cartRepository.save(cart);
            });
    }

    private ProductVariant requirePurchasableVariant(UUID tenantId, UUID variantId) {
        ProductVariant variant = variantRepository.findByTenantIdAndIdForUpdate(tenantId, variantId)
            .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada"));
        if (!variant.isActive()) {
            throw new BusinessException("La variante no esta disponible");
        }
        return variant;
    }

    private Product requirePurchasableProduct(UUID tenantId, UUID productId) {
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException("El producto no esta disponible para compra");
        }
        return product;
    }

    private CartItem newCartItem(
        UUID tenantId,
        Cart cart,
        Product product,
        ProductVariant variant
    ) {
        CartItem item = new CartItem();
        item.setTenantId(tenantId);
        item.setCartId(cart.getId());
        item.setProductId(product.getId());
        item.setVariantId(variant.getId());
        item.setQuantity(0);
        return item;
    }

    private void validateStock(ProductVariant variant, int quantity) {
        if (quantity > variant.getStock()) {
            throw new BusinessException(
                "Stock insuficiente. Disponible: " + variant.getStock()
            );
        }
    }

    private BigDecimal currentPrice(Product product, ProductVariant variant) {
        if (variant.getPrice() != null) {
            return variant.getPrice();
        }
        return product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice();
    }

    private void touch(Cart cart) {
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    private CartResponse buildResponse(UUID tenantId, Cart cart) {
        List<CartItemResponse> responses = new ArrayList<>();
        for (CartItem item : itemRepository.findAllByCartIdOrderByCreatedAtAsc(cart.getId())) {
            Product product = productRepository.findByTenantIdAndId(tenantId, item.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto del carrito no encontrado"));
            ProductVariant variant = variantRepository.findByTenantIdAndId(tenantId, item.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variante del carrito no encontrada"));
            boolean available = product.getStatus() == ProductStatus.ACTIVE
                && variant.isActive()
                && variant.getStock() >= item.getQuantity();
            String imageUrl = imageRepository
                .findAllByProductIdOrderByDisplayOrderAscCreatedAtAsc(product.getId())
                .stream()
                .min(Comparator.comparing(ProductImage::isMain).reversed()
                    .thenComparing(ProductImage::getDisplayOrder))
                .map(ProductImage::getImageUrl)
                .orElse(null);

            responses.add(new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                imageUrl,
                variant.getId(),
                variant.getSku(),
                variant.getSize(),
                variant.getColor(),
                variant.getColorHex(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                variant.getStock(),
                available
            ));
        }

        int totalItems = responses.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotal = responses.stream()
            .map(CartItemResponse::totalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean readyForCheckout = !responses.isEmpty()
            && responses.stream().allMatch(CartItemResponse::available);
        return new CartResponse(
            cart.getId(),
            cart.getStatus(),
            responses,
            totalItems,
            subtotal,
            readyForCheckout,
            cart.getUpdatedAt()
        );
    }
}
