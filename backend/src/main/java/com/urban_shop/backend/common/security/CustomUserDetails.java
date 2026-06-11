package com.urban_shop.backend.common.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.entity.User;
import com.urban_shop.backend.customer.entity.Customer;

import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID principalId;
    private final UUID tenantId;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final PrincipalType principalType;
    private final List<GrantedAuthority> authorities;
    private final boolean enabled;

    public CustomUserDetails(User user, boolean tenantEnabled) {
        this.principalId = user.getId();
        this.tenantId = user.getTenantId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.principalType = PrincipalType.INTERNAL_USER;
        this.enabled = user.isActive() && tenantEnabled;
        this.authorities = user.getRoles().stream()
                .map(Role::getName)
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + name))
                .toList();
    }

    public CustomUserDetails(Customer customer, boolean tenantEnabled) {
        this.principalId = customer.getId();
        this.tenantId = customer.getTenantId();
        this.email = customer.getEmail();
        this.passwordHash = customer.getPasswordHash();
        this.fullName = customer.getFirstName() + " " + customer.getLastName();
        this.principalType = PrincipalType.CUSTOMER;
        this.enabled = customer.isActive() && tenantEnabled;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    public UUID getUserId() {
        return principalId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public boolean isCustomer() {
        return principalType == PrincipalType.CUSTOMER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
