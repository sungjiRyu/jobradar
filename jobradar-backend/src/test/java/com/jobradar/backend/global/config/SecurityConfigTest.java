package com.jobradar.backend.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final SecurityConfig securityConfig =
            mock(SecurityConfig.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("CORS Origin의 앞뒤 공백과 빈 항목을 제거한다")
    void corsConfigurationSource_normalizesAllowedOrigins() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource(
                List.of(
                        " https://jobradar.me ",
                        "",
                        "   ",
                        "https://admin.jobradar.me"
                )
        );

        CorsConfiguration configuration =
                source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly(
                        "https://jobradar.me",
                        "https://admin.jobradar.me"
                );
    }

    @Test
    @DisplayName("정규화 후 유효한 CORS Origin이 없으면 예외가 발생한다")
    void corsConfigurationSource_rejectsBlankAllowedOrigins() {
        assertThatThrownBy(() ->
                securityConfig.corsConfigurationSource(List.of("", " ", "   ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "app.cors.allowed-origins must contain at least one non-blank origin"
                );
    }
}
