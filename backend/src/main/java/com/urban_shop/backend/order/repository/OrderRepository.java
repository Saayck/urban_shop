package com.urban_shop.backend.order.repository;

import com.urban_shop.backend.order.entity.CustomerOrder;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    Optional<CustomerOrder> findByTenantIdAndCustomerIdAndId(
        UUID tenantId,
        UUID customerId,
        UUID id
    );

    Optional<CustomerOrder> findByTenantIdAndId(UUID tenantId, UUID id);

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
