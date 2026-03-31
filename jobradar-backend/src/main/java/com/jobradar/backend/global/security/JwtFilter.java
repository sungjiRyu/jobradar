package com.jobradar.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobradar.backend.global.common.ApiResponse;
import com.jobradar.backend.global.exception.CustomException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 모든 HTTP 요청을 가로채서 JWT를 검증하는 필터
 *
 * [동작 순서]
 * 1. Authorization 헤더에서 토큰 추출
 * 2. 토큰이 있으면 유효성 검증
 * 3. 유효하면 SecurityContext에 인증 정보 저장 → 이후 컨트롤러에서 "로그인된 사용자"로 인식
 * 4. 유효하지 않으면 에러 응답 반환
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                jwtProvider.validateToken(token);

                // 토큰에서 userId, role 추출
                Long userId = jwtProvider.getUserId(token);
                Claims claims = jwtProvider.parseClaims(token);
                String role = claims.get("role", String.class);

                // Spring Security에 "이 사용자는 인증됨"을 알림
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,                                         // principal (컨트롤러에서 꺼낼 수 있음)
                                null,                                           // credentials (비밀번호 - 이미 검증됐으므로 null)
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))  // 권한 목록
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (CustomException e) {
                // 토큰이 만료되거나 위변조된 경우 → 에러 응답 반환 후 필터 체인 중단
                sendErrorResponse(response, e);
                return;
            }
        }

        // 토큰이 없거나 유효한 경우 → 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /** Authorization 헤더에서 "Bearer {token}" 형식으로 토큰 추출 */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }
        return null;
    }

    /** 필터 단계의 에러를 JSON 형식으로 직접 응답 */
    private void sendErrorResponse(HttpServletResponse response, CustomException e) throws IOException {
        response.setStatus(e.getErrorCode().getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<?> body = ApiResponse.fail(e.getErrorCode().getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
