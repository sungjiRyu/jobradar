package com.jobradar.backend.global.config;

import com.jobradar.backend.global.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정
 *
 * [핵심 역할]
 * 1. URL별 인증 필요 여부 결정
 * 2. JwtFilter를 Spring Security 필터 체인에 등록
 * 3. BCryptPasswordEncoder 제공 (비밀번호 암호화)
 * 4. CORS 허용 (프론트엔드 ↔ 백엔드 통신)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // 컨트롤러의 @PreAuthorize 사용을 위해 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            // CSRF 비활성화
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 설정 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // 세션 미사용: JWT로 인증하므로 서버에 세션을 유지하지 않음
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/api/auth/**",  // 로그인, 토큰 갱신, 로그아웃
                            "/api/users/signup",         // 회원가입
                            "/api/jobs/**",              // 채용공고 조회 (비로그인도 가능)
                            "/api/tech-stacks/**",       // 기술스택 목록 조회 (비로그인도 가능)
                            "/api/stats/**",             // 대시보드 통계 (비로그인도 가능)
                            "/swagger-ui/**",            // Swagger UI
                            "/v3/api-docs/**"            // Swagger API 문서
                    ).permitAll()
                    // /api/admin/** — ADMIN 권한 보유자만 접근 가능
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()  // 그 외 모든 요청은 로그인 필요
            )

            // JwtFilter를 Spring Security 기본 인증 필터 앞에 삽입
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 비밀번호 BCrypt 암호화 Bean */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** CORS 설정: 환경별로 주입된 프론트엔드 Origin의 요청 허용 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        List<String> normalizedAllowedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        if (normalizedAllowedOrigins.isEmpty()) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins must contain at least one non-blank origin"
            );
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(normalizedAllowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
