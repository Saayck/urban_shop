package com.urban_shop.backend.common.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urban_shop.backend.user.entity.User;
import com.urban_shop.backend.user.repository.UserRepository;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Set<String> ENABLED_TENANT_STATUSES = Set.of("ACTIVE", "TRIAL");

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
        return buildUserDetails(user);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(UUID userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + userId));
        return buildUserDetails(user);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadPrincipalById(UUID principalId, PrincipalType principalType)
        throws UsernameNotFoundException {
        if (principalType == PrincipalType.CUSTOMER) {
            Customer customer = customerRepository.findById(principalId)
                .orElseThrow(() -> new UsernameNotFoundException("Cliente no encontrado: " + principalId));
            return buildCustomerDetails(customer);
        }
        return loadUserById(principalId);
    }

    private CustomUserDetails buildUserDetails(User user) {
        boolean tenantEnabled;
        if (user.getTenantId() == null) {
            tenantEnabled = user.getRoles().stream().anyMatch(role -> "SUPER_ADMIN".equals(role.getName()));
        } else {
            tenantEnabled = tenantRepository.findById(user.getTenantId())
                .map(tenant -> ENABLED_TENANT_STATUSES.contains(tenant.getStatus()))
                .orElse(false);
        }
        return new CustomUserDetails(user, tenantEnabled);
    }

    private CustomUserDetails buildCustomerDetails(Customer customer) {
        boolean tenantEnabled = tenantRepository.findById(customer.getTenantId())
            .map(tenant -> ENABLED_TENANT_STATUSES.contains(tenant.getStatus()))
            .orElse(false);
        return new CustomUserDetails(customer, tenantEnabled);
    }
}
