package com.jobradar.backend.global.config;

import com.jobradar.backend.global.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화: REST API는 세션 없이 토큰 방식으로 동작하므로 불필요
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 설정 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 세션 미사용: JWT로 인증하므로 서버에 세션을 유지하지 않음
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/api/auth/**",          // 로그인, 토큰 갱신, 로그아웃
                            "/api/users/signup",     // 회원가입
                            "/api/jobs/**",          // 채용공고 조회 (비로그인도 가능)
                            "/api/tech-stacks/**",   // 기술스택 목록 조회 (비로그인도 가능)
                            "/api/stats/**",         // 대시보드 통계 (비로그인도 가능)
                            "/swagger-ui/**",        // Swagger UI
                            "/v3/api-docs/**"        // Swagger API 문서
                    ).permitAll()
                    // /api/admin/**는 로그인한 사용자만 접근 가능 (수동 크롤링 등 관리 기능)
                    // 운영 환경에서는 .hasRole("ADMIN")으로 더 엄격히 제한 권장
                    .requestMatchers("/api/admin/**").authenticated()
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

    /** CORS 설정: 프론트엔드(Vite 기본 포트 5173)에서 오는 요청 허용 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
