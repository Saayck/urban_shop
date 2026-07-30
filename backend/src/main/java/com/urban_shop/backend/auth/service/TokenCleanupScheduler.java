package com.urban_shop.backend.auth.service;

import com.urban_shop.backend.auth.repository.EmailVerificationTokenRepository;
import com.urban_shop.backend.auth.repository.PasswordResetTokenRepository;
import com.urban_shop.backend.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Purga los tokens caducados para que las tablas de sesion no crezcan indefinidamente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private static final long RETENTION_DAYS = 7;

    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int resetTokens = resetTokenRepository.deleteExpiredBefore(threshold);
        int refreshTokens = refreshTokenRepository.deleteExpiredBefore(threshold);
        int verificationTokens = verificationTokenRepository.deleteExpiredBefore(threshold);
        log.info(
            "Limpieza de tokens: {} de restablecimiento, {} de refresco y {} de verificacion eliminados",
            resetTokens,
            refreshTokens,
            verificationTokens
        );
    }
}
