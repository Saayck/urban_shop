package com.urban_shop.backend.customer.repository;

import com.urban_shop.backend.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findAllByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(UUID customerId);

    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);

    boolean existsByCustomerId(UUID customerId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update CustomerAddress address
        set address.defaultAddress = false
        where address.customerId = :customerId
        """)
    void clearDefault(UUID customerId);
}
