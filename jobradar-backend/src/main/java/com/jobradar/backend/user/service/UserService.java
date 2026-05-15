package com.jobradar.backend.user.service;

import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.user.dto.ChangePasswordRequest;
import com.jobradar.backend.user.dto.SignupRequest;
import com.jobradar.backend.user.dto.UpdateNicknameRequest;
import com.jobradar.backend.user.dto.UserResponse;
import com.jobradar.backend.user.entity.User;
import com.jobradar.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    /** 회원가입 */
    @Transactional
    public UserResponse signup(SignupRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        UserResponse response = UserResponse.from(userRepository.save(user));
        log.info("[User] 회원가입: email={}", request.getEmail());
        return response;
    }

    /** 내 정보 조회 */
    @Transactional(readOnly = true)
    public UserResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    /** 닉네임 수정 */
    @Transactional
    public UserResponse updateMe(String email, UpdateNicknameRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateNickname(request.getNickname());
        log.info("[User] 닉네임 변경: email={}", email);
        return UserResponse.from(user);
    }

    /** 비밀번호 변경 */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 새 비밀번호를 암호화하여 저장 (현재 비밀번호 확인은 /api/auth/verify-password에서 선행)
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        log.info("[User] 비밀번호 변경: email={}", email);
    }

    /** 회원 탈퇴 */
    @Transactional
    public void withdraw(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Redis에서 Refresh 토큰도 함께 삭제
        redisTemplate.delete("refresh:" + email);
        userRepository.delete(user);
        log.info("[User] 회원 탈퇴: email={}", email);
    }
}
