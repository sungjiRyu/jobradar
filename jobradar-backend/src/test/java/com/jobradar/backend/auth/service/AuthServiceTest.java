package com.jobradar.backend.auth.service;

import com.jobradar.backend.auth.dto.LoginRequest;
import com.jobradar.backend.auth.dto.RefreshRequest;
import com.jobradar.backend.auth.dto.TokenResponse;
import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.global.security.JwtProvider;
import com.jobradar.backend.user.entity.User;
import com.jobradar.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock; // LoginRequest, RefreshRequest는 생성자 없어서 mock으로 생성

/**
 * AuthService 단위 테스트
 *
 * [@ExtendWith(MockitoExtension.class)]
 * - Spring 컨텍스트 없이 Mockito만으로 테스트 → DB·Redis 연결 없이 빠르게 실행
 *
 * [테스트 대상 선정 이유]
 * - 로그인: 이메일·비밀번호 검증 로직이 잘못되면 서비스 전체가 동작 안 함 → 최우선 검증 대상
 * - 토큰 재발급: Redis와 토큰 불일치 시 예외 처리가 핵심 보안 로직
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    // @InjectMocks: 위의 @Mock 객체들을 AuthService 생성자에 자동 주입
    @InjectMocks
    private AuthService authService;

    // ===== 로그인 테스트 =====

    @Test
    @DisplayName("로그인 성공 - 이메일·비밀번호 일치 시 Access + Refresh 토큰 반환")
    void login_성공() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("테스터")
                .build();

        // DTO에 생성자가 없으므로 mock으로 필드값 주입
        LoginRequest request = mock(LoginRequest.class);
        given(request.getEmail()).willReturn("test@example.com");
        given(request.getPassword()).willReturn("rawPassword");

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("rawPassword", "encodedPassword")).willReturn(true);
        given(jwtProvider.generateAccessToken(anyString(), anyString())).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(anyString())).willReturn("refreshToken");

        // Redis opsForValue().set() 호출을 위한 mock (void 메서드라 별도 설정 불필요)
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일 → USER_NOT_FOUND 예외")
    void login_이메일없음_예외() {
        // given
        LoginRequest request = mock(LoginRequest.class);
        given(request.getEmail()).willReturn("notexist@example.com");
        given(userRepository.findByEmail("notexist@example.com")).willReturn(Optional.empty());

        // when & then: assertThatThrownBy → 예외가 발생하는지, 어떤 에러 코드인지 검증
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 → INVALID_PASSWORD 예외")
    void login_비밀번호불일치_예외() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("테스터")
                .build();

        LoginRequest request = mock(LoginRequest.class);
        given(request.getEmail()).willReturn("test@example.com");
        given(request.getPassword()).willReturn("wrongPassword");

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    // ===== 토큰 재발급 테스트 =====

    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 저장된 토큰과 불일치 → INVALID_TOKEN 예외")
    void refresh_Redis불일치_예외() {
        // given: 클라이언트가 보낸 토큰이 Redis에 없는 경우 (로그아웃 이후 재사용 또는 탈취된 토큰)
        RefreshRequest request = mock(RefreshRequest.class);
        given(request.getRefreshToken()).willReturn("someRefreshToken");

        // validateToken()은 void 메서드 → mock이므로 기본적으로 아무것도 하지 않음 (예외 발생 안 함)
        given(jwtProvider.getEmail("someRefreshToken")).willReturn("test@example.com");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:test@example.com")).willReturn(null); // Redis에 없음

        // when & then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }
}
