package com.urban_shop.backend.auth.repository;

import com.urban_shop.backend.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        update EmailVerificationToken token
        set token.usedAt = :now
        where lower(token.email) = lower(:email)
          and token.usedAt is null
        """)
    int invalidateActiveTokens(@Param("email") String email, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from EmailVerificationToken token where token.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") LocalDateTime threshold);
}
