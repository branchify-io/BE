package com.example.merging.user;

import com.example.merging.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

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
    public Map<String, String> login(String email, String password, HttpServletResponse response) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(email);
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);

        // Refresh Token 저장 (이전 토큰 덮어쓰기)
        refreshTokenRepository.save(new RefreshToken(email, refreshToken));

        // HttpOnly Cookie에 Refresh Token 저장
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)  // JavaScript에서 접근 불가
                .secure(true)  // True: HTTPS에서만 전송 -> localhost에서는 false로 설정해야함
                .path("/")  // 모든 경로에서 쿠키 사용 가능
                .maxAge(7 * 24 * 60 * 60)  // 7일 유지
                .sameSite("None")  // 크로스 도메인에서도 쿠키가 전송되도록 설정
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return Map.of("accessToken", accessToken, "refreshToken", refreshToken); // 프론트엔드는 Access Token만 관리하면 됨
    }

    // 액세스 토큰 갱신
    public String refreshAccessToken(HttpServletRequest request) {
        // HttpOnly Cookie에서 refreshToken 가져오기
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new RuntimeException("리프레시 토큰이 존재하지 않습니다.");
        }

        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refreshToken")) {
                refreshToken = cookie.getValue();
            }
        }

        if (refreshToken == null) {
            throw new RuntimeException("리프레시 토큰이 존재하지 않습니다.");
        }

        // DB에서 Refresh Token 검증
        RefreshToken storedToken = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> {
                    return new RuntimeException("리프레시 토큰이 존재하지 않습니다.");
                });

        System.out.println("✅ DB에서 찾은 refreshToken: " + storedToken.getRefreshToken());

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 만료되었습니다.");
        }

        // 새 액세스 토큰 발급
        return jwtTokenProvider.generateAccessToken(storedToken.getEmail());
    }

    // 로그아웃
    @Transactional
    public void logout(String email, HttpServletResponse response) {
        // DB에서 Refresh Token 삭제
        refreshTokenRepository.deleteByEmail(email);

        // HttpOnly Cookie 삭제 (클라이언트에서도 제거됨)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true) // True: HTTPS에서만 전송 -> localhost에서는 false로 설정해야함
                .path("/")
                .maxAge(0)  // 즉시 만료
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // 만료된 토큰 삭제 스케줄링
    @Transactional
    @Scheduled(fixedRate = 86400000) // 하루에 한 번 실행
    public void deleteExpiredTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7); // 7일 만료 기준
        refreshTokenRepository.deleteByCreatedAtBefore(cutoffDate);
    }

}
