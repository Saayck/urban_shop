package com.urban_shop.backend.customer.repository;

import com.urban_shop.backend.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByTenantIdAndId(UUID tenantId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select customer from Customer customer
        where customer.tenantId = :tenantId and customer.id = :customerId
        """)
    Optional<Customer> findByTenantIdAndIdForUpdate(
        @Param("tenantId") UUID tenantId,
        @Param("customerId") UUID customerId
    );

    Optional<Customer> findByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    boolean existsByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    boolean existsByTenantIdAndEmailIgnoreCaseAndIdNot(UUID tenantId, String email, UUID id);

    boolean existsByTenantIdAndPhone(UUID tenantId, String phone);

    boolean existsByTenantIdAndPhoneAndIdNot(UUID tenantId, String phone, UUID id);

    boolean existsByTenantIdAndDocumentTypeAndDocumentNumber(
        UUID tenantId,
        String documentType,
        String documentNumber
    );

    boolean existsByTenantIdAndDocumentTypeAndDocumentNumberAndIdNot(
        UUID tenantId,
        String documentType,
        String documentNumber,
        UUID id
    );
}
