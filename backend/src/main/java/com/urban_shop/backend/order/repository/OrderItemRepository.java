package com.urban_shop.backend.order.repository;

import com.urban_shop.backend.order.entity.OrderItem;
import com.urban_shop.backend.report.dto.TopProductResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);

    List<OrderItem> findAllByOrderIdIn(Collection<UUID> orderIds);

    /** Ranking de productos mas vendidos, excluyendo pedidos cancelados. */
    @Query("""
        select new com.urban_shop.backend.report.dto.TopProductResponse(
            item.productId,
            min(item.productName),
            sum(item.quantity),
            sum(item.totalPrice)
        )
        from OrderItem item, CustomerOrder o
        where item.orderId = o.id
          and item.tenantId = :tenantId
          and o.orderStatus <> com.urban_shop.backend.order.entity.OrderStatus.CANCELLED
          and o.createdAt >= :from
          and o.createdAt < :to
        group by item.productId
        order by sum(item.quantity) desc
        """)
    List<TopProductResponse> findTopProducts(
        @Param("tenantId") UUID tenantId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );

    /**
     * Pedido entregado mas reciente en el que el cliente compro ese producto.
     * Acredita la compra antes de permitir una resena.
     */
    @Query("""
        select o.id
        from OrderItem item, CustomerOrder o
        where item.orderId = o.id
          and item.tenantId = :tenantId
          and item.productId = :productId
          and o.customerId = :customerId
          and o.orderStatus = com.urban_shop.backend.order.entity.OrderStatus.DELIVERED
        order by o.createdAt desc
        """)
    List<UUID> findDeliveredOrderIds(
        @Param("tenantId") UUID tenantId,
        @Param("customerId") UUID customerId,
        @Param("productId") UUID productId,
        Pageable pageable
    );

    boolean existsByProductId(UUID productId);

    boolean existsByVariantId(UUID variantId);
}
