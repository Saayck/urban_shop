package com.urban_shop.backend.customer.repository;

import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.entity.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Busqueda del panel de administracion por nombre, correo, telefono o documento. */
    @Query("""
        select customer from Customer customer
        where customer.tenantId = :tenantId
          and (:status is null or customer.status = :status)
          and (
            :search is null
            or lower(customer.firstName) like lower(concat('%', :search, '%'))
            or lower(customer.lastName) like lower(concat('%', :search, '%'))
            or lower(customer.email) like lower(concat('%', :search, '%'))
            or customer.phone like concat('%', :search, '%')
            or customer.documentNumber like concat('%', :search, '%')
          )
        """)
    Page<Customer> searchAdmin(
        @Param("tenantId") UUID tenantId,
        @Param("search") String search,
        @Param("status") CustomerStatus status,
        Pageable pageable
    );

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
