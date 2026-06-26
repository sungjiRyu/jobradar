package com.jobradar.backend.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentVariableConfigTest {

    @Test
    @DisplayName("로컬 설정은 Redis와 CORS Origin의 기본값을 사용한다")
    void localDefaults() throws IOException {
        StandardEnvironment environment = loadEnvironment("application.yml.example", Map.of());

        assertThat(environment.getProperty("spring.data.redis.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("spring.data.redis.port", Integer.class)).isEqualTo(6379);
        assertThat(environment.getProperty("spring.data.redis.ssl.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("app.cors.allowed-origins"))
                .isEqualTo("http://localhost:5173");
    }

    @Test
    @DisplayName("운영 설정은 Redis와 CORS Origin 환경변수를 주입받는다")
    void productionEnvironmentVariables() throws IOException {
        StandardEnvironment environment = loadEnvironment(
                "application-prod.yml",
                Map.of(
                        "REDIS_HOST", "redis.internal",
                        "REDIS_PORT", "6380",
                        "REDIS_SSL", "true",
                        "CORS_ALLOWED_ORIGINS",
                        "https://jobradar.me,https://admin.jobradar.me"
                )
        );

        assertThat(environment.getProperty("spring.data.redis.host")).isEqualTo("redis.internal");
        assertThat(environment.getProperty("spring.data.redis.port", Integer.class)).isEqualTo(6380);
        assertThat(environment.getProperty("spring.data.redis.ssl.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("app.cors.allowed-origins", String[].class))
                .containsExactly("https://jobradar.me", "https://admin.jobradar.me");
    }

    private StandardEnvironment loadEnvironment(
            String resourceName,
            Map<String, Object> environmentVariables) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(
                StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(
                new MapPropertySource("test-environment", environmentVariables));

        List<PropertySource<?>> yamlSources = new YamlPropertySourceLoader()
                .load(resourceName, new ClassPathResource(resourceName));
        yamlSources.forEach(environment.getPropertySources()::addLast);

        return environment;
    }
}
