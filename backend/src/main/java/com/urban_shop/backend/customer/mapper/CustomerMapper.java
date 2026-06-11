package com.urban_shop.backend.customer.mapper;

import com.urban_shop.backend.customer.dto.response.CustomerAddressResponse;
import com.urban_shop.backend.customer.dto.response.CustomerResponse;
import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.entity.CustomerAddress;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getTenantId(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getDocumentType(),
            customer.getDocumentNumber(),
            customer.isEmailVerified(),
            customer.isPhoneVerified(),
            customer.getStatus(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }

    public static CustomerAddressResponse toAddressResponse(CustomerAddress address) {
        return new CustomerAddressResponse(
            address.getId(),
            address.getDepartment(),
            address.getProvince(),
            address.getDistrict(),
            address.getAddress(),
            address.getReference(),
            address.getLatitude(),
            address.getLongitude(),
            address.isDefaultAddress(),
            address.getCreatedAt()
        );
    }
}
