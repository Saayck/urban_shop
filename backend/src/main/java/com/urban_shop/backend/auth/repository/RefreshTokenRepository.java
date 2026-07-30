package com.urban_shop.backend.auth.repository;

import com.urban_shop.backend.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoca todas las sesiones activas de un principal (cambio de contrasena, desactivacion). */
    @Modifying
    @Query("""
        update RefreshToken token
        set token.revokedAt = :now
        where token.principalId = :principalId
          and token.revokedAt is null
        """)
    int revokeAllForPrincipal(@Param("principalId") UUID principalId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from RefreshToken token where token.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") LocalDateTime threshold);
}
