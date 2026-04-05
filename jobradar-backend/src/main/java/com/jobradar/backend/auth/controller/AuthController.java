package com.jobradar.backend.auth.controller;

import com.jobradar.backend.auth.dto.LoginRequest;
import com.jobradar.backend.auth.dto.RefreshRequest;
import com.jobradar.backend.auth.dto.TokenResponse;
import com.jobradar.backend.auth.service.AuthService;
import com.jobradar.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/login - 로그인 */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /** POST /api/auth/refresh - 액세스 토큰 재발급 */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    /** POST /api/auth/logout - 로그아웃 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal String email) {
        authService.logout(email);
        return ApiResponse.ok("로그아웃 되었습니다.");
    }
}
