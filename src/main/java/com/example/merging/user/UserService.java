package com.example.merging.user;

import com.example.merging.jwt.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // Refresh 토큰 저장소 (데이터베이스 또는 메모리로 변경 가능)
    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

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

        // 리프레시 토큰 저장
        refreshTokenStore.put(email, refreshToken);

        return Map.of("accessToken", accessToken, "refrshToken", refreshToken);
    }

    // 액세스 토큰 갱신
    public String refreshAccessToken(String email, String refreshToken) {
        String storedRefreshToke = refreshTokenStore.get(email);

        if (storedRefreshToke == null || !storedRefreshToke.equals(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 유효하지 않습니다.");
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 만료되었습니다.");
        }

        // 새 액세스 토큰 발급
        return jwtTokenProvider.generateAccessToken(email);
    }
}
