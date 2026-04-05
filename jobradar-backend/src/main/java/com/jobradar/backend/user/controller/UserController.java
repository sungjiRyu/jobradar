package com.jobradar.backend.user.controller;

import com.jobradar.backend.global.common.ApiResponse;
import com.jobradar.backend.user.dto.ChangePasswordRequest;
import com.jobradar.backend.user.dto.SignupRequest;
import com.jobradar.backend.user.dto.UpdateNicknameRequest;
import com.jobradar.backend.user.dto.UserResponse;
import com.jobradar.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** POST /api/users/signup - 회원가입 */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok("회원가입이 완료되었습니다.", userService.signup(request));
    }

    /** GET /api/users/me - 내 정보 조회 */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal String email) {
        return ApiResponse.ok(userService.getMe(email));
    }

    /** PUT /api/users/me - 닉네임 수정 */
    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMe(@AuthenticationPrincipal String email,
                                              @Valid @RequestBody UpdateNicknameRequest request) {
        return ApiResponse.ok(userService.updateMe(email, request));
    }

    /** PATCH /api/users/me/password - 비밀번호 변경 */
    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal String email,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(email, request);
        return ApiResponse.ok("비밀번호가 변경되었습니다.");
    }

    /** DELETE /api/users/me - 회원 탈퇴 */
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal String email) {
        userService.withdraw(email);
        return ApiResponse.ok("회원 탈퇴가 완료되었습니다.");
    }
}
