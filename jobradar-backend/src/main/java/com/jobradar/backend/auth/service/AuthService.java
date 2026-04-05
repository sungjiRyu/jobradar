package com.jobradar.backend.auth.service;

import com.jobradar.backend.auth.dto.LoginRequest;
import com.jobradar.backend.auth.dto.RefreshRequest;
import com.jobradar.backend.auth.dto.TokenResponse;
import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.global.security.JwtProvider;
import com.jobradar.backend.user.entity.User;
import com.jobradar.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_KEY_PREFIX = "refresh:";

    /** 로그인: 이메일/비밀번호 확인 후 Access + Refresh 토큰 발급 */
    public TokenResponse login(LoginRequest request) {
        // 1. 이메일로 유저 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 비밀번호 검증 (입력값 vs 암호화된 DB 값)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. 토큰 생성
        String accessToken = jwtProvider.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());

        // 4. Refresh 토큰을 Redis에 저장 (key: "refresh:{email}", TTL: 7일)
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + user.getEmail(),
                refreshToken,
                7, TimeUnit.DAYS
        );

        return new TokenResponse(accessToken, refreshToken);
    }

    /** 토큰 재발급: Refresh 토큰 검증 후 새 Access 토큰 발급 */
    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Refresh 토큰 유효성 검증 (만료/위변조 확인)
        jwtProvider.validateToken(refreshToken);

        // 2. 토큰에서 email 추출
        String email = jwtProvider.getEmail(refreshToken);

        // 3. Redis에 저장된 토큰과 비교 (로그아웃 여부 확인)
        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + email);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 4. 유저 조회 후 새 토큰 발급
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.generateAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getEmail());

        // 5. Redis에 새 Refresh 토큰으로 교체
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + email,
                newRefreshToken,
                7, TimeUnit.DAYS
        );

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /** 로그아웃: Redis에서 Refresh 토큰 삭제 */
    public void logout(String email) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + email);
    }
}
