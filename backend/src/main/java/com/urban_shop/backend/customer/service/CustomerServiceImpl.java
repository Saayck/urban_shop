package com.urban_shop.backend.customer.service;

import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.customer.dto.request.CustomerAddressRequest;
import com.urban_shop.backend.customer.dto.request.CustomerUpdateRequest;
import com.urban_shop.backend.customer.dto.response.CustomerAddressResponse;
import com.urban_shop.backend.customer.dto.response.CustomerResponse;
import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.entity.CustomerAddress;
import com.urban_shop.backend.customer.mapper.CustomerMapper;
import com.urban_shop.backend.customer.repository.CustomerAddressRepository;
import com.urban_shop.backend.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Customer register(UUID tenantId, RegisterCustomerRequest request) {
        String email = normalizeEmail(request.email());
        String phone = request.phone().trim();
        DocumentData document = validateDocument(request.documentType(), request.documentNumber());
        ensureUniqueData(tenantId, email, phone, document, null);

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setDocumentType(document.type());
        customer.setDocumentNumber(document.number());
        customer.setPasswordHash(passwordEncoder.encode(request.password()));
        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse get(UUID tenantId, UUID customerId) {
        return CustomerMapper.toResponse(requireCustomer(tenantId, customerId));
    }

    @Override
    @Transactional
    public CustomerResponse update(UUID tenantId, UUID customerId, CustomerUpdateRequest request) {
        Customer customer = requireCustomer(tenantId, customerId);
        String email = normalizeEmail(request.email());
        String phone = request.phone().trim();
        DocumentData document = validateDocument(request.documentType(), request.documentNumber());
        ensureUniqueData(tenantId, email, phone, document, customerId);

        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setDocumentType(document.type());
        customer.setDocumentNumber(document.number());
        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> listAddresses(UUID tenantId, UUID customerId) {
        requireCustomer(tenantId, customerId);
        return addressRepository.findAllByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customerId)
            .stream()
            .map(CustomerMapper::toAddressResponse)
            .toList();
    }

    @Override
    @Transactional
    public CustomerAddressResponse createAddress(
        UUID tenantId,
        UUID customerId,
        CustomerAddressRequest request
    ) {
        requireCustomer(tenantId, customerId);
        boolean firstAddress = !addressRepository.existsByCustomerId(customerId);
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(request.defaultAddress());
        if (makeDefault && !firstAddress) {
            addressRepository.clearDefault(customerId);
        }

        CustomerAddress address = new CustomerAddress();
        address.setCustomerId(customerId);
        applyAddress(address, request);
        address.setDefaultAddress(makeDefault);
        return CustomerMapper.toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public CustomerAddressResponse updateAddress(
        UUID tenantId,
        UUID customerId,
        UUID addressId,
        CustomerAddressRequest request
    ) {
        requireCustomer(tenantId, customerId);
        CustomerAddress address = requireAddress(customerId, addressId);
        boolean makeDefault = Boolean.TRUE.equals(request.defaultAddress());
        if (makeDefault && !address.isDefaultAddress()) {
            addressRepository.clearDefault(customerId);
        }
        applyAddress(address, request);
        if (makeDefault) {
            address.setDefaultAddress(true);
        } else if (!address.isDefaultAddress()) {
            address.setDefaultAddress(false);
        }
        return CustomerMapper.toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(UUID tenantId, UUID customerId, UUID addressId) {
        requireCustomer(tenantId, customerId);
        CustomerAddress address = requireAddress(customerId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        addressRepository.flush();

        if (wasDefault) {
            addressRepository.findAllByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customerId)
                .stream()
                .findFirst()
                .ifPresent(next -> {
                    next.setDefaultAddress(true);
                    addressRepository.save(next);
                });
        }
    }

    private Customer requireCustomer(UUID tenantId, UUID customerId) {
        return customerRepository.findByTenantIdAndId(tenantId, customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    private CustomerAddress requireAddress(UUID customerId, UUID addressId) {
        return addressRepository.findByIdAndCustomerId(addressId, customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Direccion no encontrada"));
    }

    private void ensureUniqueData(
        UUID tenantId,
        String email,
        String phone,
        DocumentData document,
        UUID currentId
    ) {
        boolean emailExists = currentId == null
            ? customerRepository.existsByTenantIdAndEmailIgnoreCase(tenantId, email)
            : customerRepository.existsByTenantIdAndEmailIgnoreCaseAndIdNot(tenantId, email, currentId);
        if (emailExists) {
            throw new BusinessException("El correo ya esta registrado en esta tienda");
        }

        boolean phoneExists = currentId == null
            ? customerRepository.existsByTenantIdAndPhone(tenantId, phone)
            : customerRepository.existsByTenantIdAndPhoneAndIdNot(tenantId, phone, currentId);
        if (phoneExists) {
            throw new BusinessException("El celular ya esta registrado en esta tienda");
        }

        if (document.number() != null) {
            boolean documentExists = currentId == null
                ? customerRepository.existsByTenantIdAndDocumentTypeAndDocumentNumber(
                    tenantId,
                    document.type(),
                    document.number()
                )
                : customerRepository.existsByTenantIdAndDocumentTypeAndDocumentNumberAndIdNot(
                    tenantId,
                    document.type(),
                    document.number(),
                    currentId
                );
            if (documentExists) {
                throw new BusinessException("El documento ya esta registrado en esta tienda");
            }
        }
    }

    private DocumentData validateDocument(String typeValue, String numberValue) {
        String type = trimToNull(typeValue);
        String number = trimToNull(numberValue);
        if ((type == null) != (number == null)) {
            throw new BusinessException("Tipo y numero de documento deben enviarse juntos");
        }
        if (type == null) {
            return new DocumentData(null, null);
        }

        type = type.toUpperCase(Locale.ROOT);
        boolean valid = switch (type) {
            case "DNI" -> number.matches("^\\d{8}$");
            case "CE" -> number.matches("^\\d{9,12}$");
            case "PASSPORT" -> number.matches("^[A-Za-z0-9]{6,12}$");
            default -> false;
        };
        if (!valid) {
            throw new BusinessException("Numero de documento no valido para " + type);
        }
        return new DocumentData(type, number.toUpperCase(Locale.ROOT));
    }

    private void applyAddress(CustomerAddress address, CustomerAddressRequest request) {
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new BusinessException("Latitud y longitud deben enviarse juntas");
        }
        address.setDepartment(request.department().trim());
        address.setProvince(request.province().trim());
        address.setDistrict(request.district().trim());
        address.setAddress(request.address().trim());
        address.setReference(trimToNull(request.reference()));
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DocumentData(String type, String number) {
    }
}
