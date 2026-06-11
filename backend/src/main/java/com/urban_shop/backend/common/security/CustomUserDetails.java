package com.urban_shop.backend.common.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.entity.User;

import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;
    private final boolean tenantEnabled;

    public CustomUserDetails(User user, boolean tenantEnabled) {
        this.user = user;
        this.tenantEnabled = tenantEnabled;
        this.authorities = user.getRoles().stream()
                .map(Role::getName)
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + name))
                .toList();
    }

    public UUID getUserId() {
        return user.getId();
    }

    public UUID getTenantId() {
        return user.getTenantId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
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
        return user.isActive() && tenantEnabled;
    }
}
