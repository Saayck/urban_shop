package com.urban_shop.backend.auth.repository;

import com.urban_shop.backend.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Invalida los tokens vigentes de un correo antes de emitir uno nuevo. */
    @Modifying
    @Query("""
        update PasswordResetToken token
        set token.usedAt = :now
        where lower(token.email) = lower(:email)
          and token.usedAt is null
        """)
    int invalidateActiveTokens(@Param("email") String email, @Param("now") LocalDateTime now);

    /** Purga tokens caducados o ya usados para que la tabla no crezca sin limite. */
    @Modifying
    @Query("delete from PasswordResetToken token where token.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") LocalDateTime threshold);
}
