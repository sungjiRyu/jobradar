package com.jobradar.backend.user.service;

import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.user.dto.SignupRequest;
import com.jobradar.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * UserService 단위 테스트
 *
 * [테스트 대상 선정 이유]
 * - 이메일 중복 체크: 동일 이메일로 가입 시 예외 발생 여부가 핵심 비즈니스 규칙
 *   → 이 로직이 누락되면 같은 이메일로 여러 계정이 생성되는 심각한 버그 발생
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private UserService userService;

    // ===== 회원가입 테스트 =====

    @Test
    @DisplayName("회원가입 실패 - 이미 사용 중인 이메일 → EMAIL_ALREADY_EXISTS 예외")
    void signup_이메일중복_예외() {
        // given
        SignupRequest request = mock(SignupRequest.class); // 생성자 없는 DTO → mock으로 생성
        given(request.getEmail()).willReturn("duplicate@example.com");
        given(userRepository.existsByEmail("duplicate@example.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
