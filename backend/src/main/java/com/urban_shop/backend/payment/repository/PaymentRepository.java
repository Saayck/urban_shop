package com.urban_shop.backend.payment.repository;

import com.urban_shop.backend.payment.entity.Payment;
import com.urban_shop.backend.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);

    Optional<Payment> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndOrderIdAndStatusIn(
        UUID tenantId,
        UUID orderId,
        Collection<PaymentStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select payment from Payment payment
        where payment.tenantId = :tenantId and payment.id = :paymentId
        """)
    Optional<Payment> findByTenantIdAndIdForUpdate(
        @Param("tenantId") UUID tenantId,
        @Param("paymentId") UUID paymentId
    );
}
