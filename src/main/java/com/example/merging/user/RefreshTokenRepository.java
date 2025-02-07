package com.example.merging.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    // 만료된 토큰 삭제 (스케줄러용)
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    @Modifying
    @Transactional
    void deleteByEmail(String email);

    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
