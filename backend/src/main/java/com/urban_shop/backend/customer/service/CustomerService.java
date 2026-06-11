package com.urban_shop.backend.customer.service;

import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.customer.dto.request.CustomerAddressRequest;
import com.urban_shop.backend.customer.dto.request.CustomerUpdateRequest;
import com.urban_shop.backend.customer.dto.response.CustomerAddressResponse;
import com.urban_shop.backend.customer.dto.response.CustomerResponse;
import com.urban_shop.backend.customer.entity.Customer;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    Customer register(UUID tenantId, RegisterCustomerRequest request);

    CustomerResponse get(UUID tenantId, UUID customerId);

    CustomerResponse update(UUID tenantId, UUID customerId, CustomerUpdateRequest request);

    List<CustomerAddressResponse> listAddresses(UUID tenantId, UUID customerId);

    CustomerAddressResponse createAddress(
        UUID tenantId,
        UUID customerId,
        CustomerAddressRequest request
    );

    CustomerAddressResponse updateAddress(
        UUID tenantId,
        UUID customerId,
        UUID addressId,
        CustomerAddressRequest request
    );

    void deleteAddress(UUID tenantId, UUID customerId, UUID addressId);
}
