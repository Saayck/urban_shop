package com.urban_shop.backend.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.bootstrap.super-admin")
public class SuperAdminBootstrapProperties {

    private boolean enabled;
    private String email;
    private String password;
    private String fullName;
}
