package com.urban_shop.backend.review.service;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.util.SlugUtils;
import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.repository.CustomerRepository;
import com.urban_shop.backend.order.repository.OrderItemRepository;
import com.urban_shop.backend.product.entity.Product;
import com.urban_shop.backend.product.repository.ProductRepository;
import com.urban_shop.backend.review.dto.request.ReviewCreateRequest;
import com.urban_shop.backend.review.dto.request.ReviewModerationRequest;
import com.urban_shop.backend.review.dto.request.ReviewUpdateRequest;
import com.urban_shop.backend.review.dto.response.ReviewResponse;
import com.urban_shop.backend.review.dto.response.ReviewSummary;
import com.urban_shop.backend.review.entity.ProductReview;
import com.urban_shop.backend.review.repository.ProductReviewRepository;
import com.urban_shop.backend.tenant.service.PublicTenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final String REVIEW_NOT_FOUND = "Resena no encontrada";
    private static final String PRODUCT_NOT_FOUND = "Producto no encontrado";

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;
    private final PublicTenantResolver publicTenantResolver;

    @Override
    @Transactional
    @CacheEvict(value = "storeProducts", allEntries = true)
    public ReviewResponse create(UUID tenantId, UUID customerId, ReviewCreateRequest request) {
        productRepository.findByTenantIdAndId(tenantId, request.productId())
            .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND));

        if (reviewRepository.existsByProductIdAndCustomerId(request.productId(), customerId)) {
            throw new BusinessException("Ya has resenado este producto");
        }

        // Solo resena quien compro y recibio: evita resenas falsas.
        UUID orderId = orderItemRepository.findDeliveredOrderIds(
            tenantId,
            customerId,
            request.productId(),
            PageRequest.of(0, 1)
        ).stream().findFirst().orElseThrow(() -> new BusinessException(
            "Solo puedes resenar productos de un pedido que ya recibiste"
        ));

        ProductReview review = new ProductReview();
        review.setTenantId(tenantId);
        review.setProductId(request.productId());
        review.setCustomerId(customerId);
        review.setOrderId(orderId);
        review.setRating(request.rating());
        review.setTitle(trimToNull(request.title()));
        review.setComment(trimToNull(request.comment()));
        review.setVisible(true);
        return toResponse(reviewRepository.save(review), customerName(customerId));
    }

    @Override
    @Transactional
    @CacheEvict(value = "storeProducts", allEntries = true)
    public ReviewResponse update(
        UUID tenantId,
        UUID customerId,
        UUID reviewId,
        ReviewUpdateRequest request
    ) {
        ProductReview review = requireOwnReview(tenantId, customerId, reviewId);
        review.setRating(request.rating());
        review.setTitle(trimToNull(request.title()));
        review.setComment(trimToNull(request.comment()));
        return toResponse(reviewRepository.save(review), customerName(customerId));
    }

    @Override
    @Transactional
    @CacheEvict(value = "storeProducts", allEntries = true)
    public void delete(UUID tenantId, UUID customerId, UUID reviewId) {
        reviewRepository.delete(requireOwnReview(tenantId, customerId, reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listMine(UUID tenantId, UUID customerId, int page, int size) {
        Page<ProductReview> reviews = reviewRepository.findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(
            tenantId,
            customerId,
            PageRequest.of(page, size)
        );
        String name = customerName(customerId);
        return PageResponse.from(reviews.map(review -> toResponse(review, name)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listPublic(
        String tenantSlug,
        String productSlug,
        int page,
        int size
    ) {
        Product product = requirePublicProduct(tenantSlug, productSlug);
        Page<ProductReview> reviews = reviewRepository.findAllByProductIdAndVisibleTrueOrderByCreatedAtDesc(
            product.getId(),
            PageRequest.of(page, size)
        );
        return PageResponse.from(reviews.map(review -> toResponse(review, null)))
            .withContent(withCustomerNames(reviews.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummary summaryPublic(String tenantSlug, String productSlug) {
        return reviewRepository.summarizeForProduct(requirePublicProduct(tenantSlug, productSlug).getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listAdmin(UUID tenantId, int page, int size) {
        Page<ProductReview> reviews = reviewRepository.findAllByTenantIdOrderByCreatedAtDesc(
            tenantId,
            PageRequest.of(page, size)
        );
        return PageResponse.from(reviews.map(review -> toResponse(review, null)))
            .withContent(withCustomerNames(reviews.getContent()));
    }

    @Override
    @Transactional
    @CacheEvict(value = "storeProducts", allEntries = true)
    public ReviewResponse moderate(UUID tenantId, UUID reviewId, ReviewModerationRequest request) {
        ProductReview review = reviewRepository.findByTenantIdAndId(tenantId, reviewId)
            .orElseThrow(() -> new ResourceNotFoundException(REVIEW_NOT_FOUND));
        review.setVisible(Boolean.TRUE.equals(request.visible()));
        review.setModerationNote(trimToNull(request.moderationNote()));
        return toResponse(reviewRepository.save(review), customerName(review.getCustomerId()));
    }

    /** Resuelve los nombres de una pagina de resenas en una sola consulta. */
    private List<ReviewResponse> withCustomerNames(List<ProductReview> reviews) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        List<UUID> customerIds = reviews.stream().map(ProductReview::getCustomerId).distinct().toList();
        Map<UUID, Customer> byId = customerRepository.findAllById(customerIds).stream()
            .collect(Collectors.toMap(Customer::getId, Function.identity()));
        return reviews.stream()
            .map(review -> toResponse(review, displayName(byId.get(review.getCustomerId()))))
            .toList();
    }

    private Product requirePublicProduct(String tenantSlug, String productSlug) {
        UUID tenantId = publicTenantResolver.requireTenantId(tenantSlug);
        String normalized;
        try {
            normalized = SlugUtils.normalizeOrGenerate(productSlug, productSlug);
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException(PRODUCT_NOT_FOUND);
        }
        return productRepository.findByTenantIdAndSlug(tenantId, normalized)
            .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND));
    }

    private ProductReview requireOwnReview(UUID tenantId, UUID customerId, UUID reviewId) {
        return reviewRepository.findByTenantIdAndId(tenantId, reviewId)
            .filter(review -> customerId.equals(review.getCustomerId()))
            .orElseThrow(() -> new ResourceNotFoundException(REVIEW_NOT_FOUND));
    }

    private String customerName(UUID customerId) {
        return customerRepository.findById(customerId).map(this::displayName).orElse(null);
    }

    /** Solo nombre e inicial del apellido: no se expone el apellido completo en la tienda. */
    private String displayName(Customer customer) {
        if (customer == null) {
            return null;
        }
        String lastName = customer.getLastName();
        String initial = lastName == null || lastName.isBlank()
            ? ""
            : " " + lastName.trim().charAt(0) + ".";
        return customer.getFirstName() + initial;
    }

    private ReviewResponse toResponse(ProductReview review, String customerName) {
        return new ReviewResponse(
            review.getId(),
            review.getProductId(),
            review.getCustomerId(),
            customerName,
            review.getRating(),
            review.getTitle(),
            review.getComment(),
            review.isVisible(),
            review.getModerationNote(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
