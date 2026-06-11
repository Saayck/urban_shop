package com.urban_shop.backend.common.security;

import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.entity.User;
import com.urban_shop.backend.user.repository.RoleRepository;
import com.urban_shop.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "app.bootstrap.super-admin",
    name = "enabled",
    havingValue = "true"
)
public class AdminBootstrap {

    private final SuperAdminBootstrapProperties properties;

    @Bean
    public CommandLineRunner ensureSuperAdmin(UserRepository userRepository,
                                               RoleRepository roleRepository,
                                               PasswordEncoder passwordEncoder) {
        return args -> {
            validateConfiguration();

            String email = properties.getEmail().trim().toLowerCase();
            if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
                return;
            }
            Role superAdmin = roleRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                    "Role SUPER_ADMIN not found. Did Flyway seed data run?"));

            User admin = new User();
            admin.setEmail(email);
            admin.setFullName(properties.getFullName().trim());
            admin.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
            admin.setActive(true);
            admin.getRoles().add(superAdmin);
            userRepository.save(admin);

            log.warn("Bootstrap SUPER_ADMIN created for email {}. Disable bootstrap after initial setup.", email);
        };
    }

    private void validateConfiguration() {
        if (properties.getEmail() == null || properties.getEmail().isBlank()) {
            throw new IllegalStateException("BOOTSTRAP_SUPER_ADMIN_EMAIL is required when bootstrap is enabled");
        }
        if (properties.getPassword() == null || properties.getPassword().length() < 12) {
            throw new IllegalStateException(
                "BOOTSTRAP_SUPER_ADMIN_PASSWORD must contain at least 12 characters when bootstrap is enabled");
        }
        if (properties.getFullName() == null || properties.getFullName().isBlank()) {
            throw new IllegalStateException("BOOTSTRAP_SUPER_ADMIN_FULL_NAME is required when bootstrap is enabled");
        }
    }
}
