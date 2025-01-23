package com.example.merging.user;

import com.example.merging.jwt.JwtTokenProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // 회원가입
    public String joinUser(UserDTO userDTO) {
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new RuntimeException("이미 등록된 이메일입니다.");
        }

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setAffiliation((userDTO.getAffiliation()));
        userRepository.save(user);

        return jwtTokenProvider.generateAccessToken(user.getEmail());
    }

    // 로그인
    public Map<String, String> login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(email);
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);

        // Refresh Token 저장 (이전 토큰 덮어쓰기)
        refreshTokenRepository.save(new RefreshToken(email, refreshToken));

        return Map.of("accessToken", accessToken, "refrshToken", refreshToken);
    }

    // 액세스 토큰 갱신
    public String refreshAccessToken(String email, String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("리프레시 토큰이 존재하지 않습니다."));

        if (!storedToken.getRefreshToken().equals(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 유효하지 않습니다.");
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 만료되었습니다.");
        }

        // 새 액세스 토큰 발급
        return jwtTokenProvider.generateAccessToken(email);
    }

    // 만료된 토큰 삭제 스케줄링
    @Scheduled(fixedRate = 86400000) // 하루에 한 번 실행
    public void deleteExpiredTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7); // 7일 만료 기준
        refreshTokenRepository.deleteByCreatedAtBefore(cutoffDate);
    }
}
