package com.urban_shop.backend.payment.repository;

import com.urban_shop.backend.payment.entity.Payment;
import com.urban_shop.backend.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    boolean existsByTenantIdAndProviderIgnoreCaseAndOperationCodeIgnoreCase(
        UUID tenantId,
        String provider,
        String operationCode
    );

    Optional<Payment> findFirstByTenantIdAndOrderIdAndStatusInOrderByCreatedAtDesc(
        UUID tenantId,
        UUID orderId,
        Collection<PaymentStatus> statuses
    );

    @Query("""
        select p from Payment p
        where p.tenantId = :tenantId
          and (:status is null or p.status = :status)
        """)
    Page<Payment> findAdmin(
        @Param("tenantId") UUID tenantId,
        @Param("status") PaymentStatus status,
        Pageable pageable
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
