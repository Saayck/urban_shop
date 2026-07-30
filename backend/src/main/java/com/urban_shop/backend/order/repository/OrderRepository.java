package com.urban_shop.backend.order.repository;

import com.urban_shop.backend.order.entity.CustomerOrder;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;
import com.urban_shop.backend.report.dto.SalesSummary;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    Optional<CustomerOrder> findByTenantIdAndCustomerIdAndId(
        UUID tenantId,
        UUID customerId,
        UUID id
    );

    Optional<CustomerOrder> findByTenantIdAndId(UUID tenantId, UUID id);

    /** Resumen de ventas calculado en base de datos, sin traer los pedidos a memoria. */
    @Query("""
        select new com.urban_shop.backend.report.dto.SalesSummary(
            count(o),
            sum(case when o.orderStatus = com.urban_shop.backend.order.entity.OrderStatus.DELIVERED then 1L else 0L end),
            sum(case when o.orderStatus in (
                    com.urban_shop.backend.order.entity.OrderStatus.CREATED,
                    com.urban_shop.backend.order.entity.OrderStatus.PAID,
                    com.urban_shop.backend.order.entity.OrderStatus.PREPARING,
                    com.urban_shop.backend.order.entity.OrderStatus.READY_FOR_PICKUP,
                    com.urban_shop.backend.order.entity.OrderStatus.ON_THE_WAY
                ) then 1L else 0L end),
            sum(case when o.orderStatus = com.urban_shop.backend.order.entity.OrderStatus.CANCELLED then 1L else 0L end),
            coalesce(sum(case when o.orderStatus <> com.urban_shop.backend.order.entity.OrderStatus.CANCELLED
                then o.total else 0 end), 0)
        )
        from CustomerOrder o
        where o.tenantId = :tenantId
          and o.createdAt >= :from
          and o.createdAt < :to
        """)
    SalesSummary summarize(
        @Param("tenantId") UUID tenantId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /** Pedidos, gasto e ultima compra por cliente, resueltos en una sola consulta. */
    @Query("""
        select new com.urban_shop.backend.order.repository.CustomerPurchaseStats(
            o.customerId,
            count(o),
            coalesce(sum(case when o.orderStatus <> com.urban_shop.backend.order.entity.OrderStatus.CANCELLED
                then o.total else 0 end), 0),
            max(o.createdAt)
        )
        from CustomerOrder o
        where o.tenantId = :tenantId
          and o.customerId in :customerIds
        group by o.customerId
        """)
    List<CustomerPurchaseStats> findPurchaseStats(
        @Param("tenantId") UUID tenantId,
        @Param("customerIds") Collection<UUID> customerIds
    );

    @Query("""
        select o from CustomerOrder o
        where o.tenantId = :tenantId
          and o.createdAt >= :from
          and o.createdAt < :to
        order by o.createdAt asc
        """)
    List<CustomerOrder> findForExport(
        @Param("tenantId") UUID tenantId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    Page<CustomerOrder> findAllByTenantIdAndCustomerId(
        UUID tenantId,
        UUID customerId,
        Pageable pageable
    );

    @Query("""
        select o from CustomerOrder o
        where o.tenantId = :tenantId
          and (:orderStatus is null or o.orderStatus = :orderStatus)
          and (:paymentStatus is null or o.paymentStatus = :paymentStatus)
        """)
    Page<CustomerOrder> findAdmin(
        @Param("tenantId") UUID tenantId,
        @Param("orderStatus") OrderStatus orderStatus,
        @Param("paymentStatus") OrderPaymentStatus paymentStatus,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o from CustomerOrder o
        where o.tenantId = :tenantId and o.id = :orderId
        """)
    Optional<CustomerOrder> findByTenantIdAndIdForUpdate(
        @Param("tenantId") UUID tenantId,
        @Param("orderId") UUID orderId
    );
}
