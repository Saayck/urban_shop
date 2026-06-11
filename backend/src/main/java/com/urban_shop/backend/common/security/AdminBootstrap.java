package com.urban_shop.backend.common.security;

import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.entity.User;
import com.urban_shop.backend.user.repository.RoleRepository;
import com.urban_shop.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap {

    private static final String DEFAULT_ADMIN_EMAIL = "admin@urbanshop.pe";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin123!";

    @Bean
    public CommandLineRunner ensureSuperAdmin(UserRepository userRepository,
                                              RoleRepository roleRepository,
                                              PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail(DEFAULT_ADMIN_EMAIL).isPresent()) {
                return;
            }
            Role superAdmin = roleRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                    "Role SUPER_ADMIN not found. Did Flyway seed data run?"));

            User admin = new User();
            admin.setEmail(DEFAULT_ADMIN_EMAIL);
            admin.setFullName("Super Administrador");
            admin.setPasswordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            admin.setActive(true);
            admin.getRoles().add(superAdmin);
            userRepository.save(admin);

            log.warn("Default SUPER_ADMIN created: {} / {} -- CHANGE THIS PASSWORD",
                DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
        };
    }
}
