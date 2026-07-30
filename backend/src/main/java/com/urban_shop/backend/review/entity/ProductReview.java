package com.urban_shop.backend.review.entity;

import com.urban_shop.backend.common.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@NoArgsConstructor
public class ProductReview extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /** Pedido que acredita la compra: sin el no se puede resenar. */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "rating", nullable = false)
    private short rating;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_visible", nullable = false)
    private boolean visible = true;

    @Column(name = "moderation_note", length = 500)
    private String moderationNote;
}
