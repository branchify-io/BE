package com.example.merging.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    // 만료된 토큰 삭제
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
