package com.jobradar.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI 3.0) 설정
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT Bearer 인증 스키마 이름 (임의 식별자)
        String jwtSchemeName = "BearerAuth";

        // SecurityRequirement: 모든 API에 이 인증 스키마를 기본 적용
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtSchemeName);

        // SecurityScheme: Bearer 토큰 방식으로 HTTP Authorization 헤더 사용
        SecurityScheme securityScheme = new SecurityScheme()
                .name(jwtSchemeName)
                .type(SecurityScheme.Type.HTTP)   // HTTP 방식 (쿠키/API Key 방식과 구분)
                .scheme("bearer")                 // Authorization: Bearer {token} 형식
                .bearerFormat("JWT");             // 토큰 형식이 JWT임을 명시 (문서용)

        return new OpenAPI()
                .info(new Info()
                        .title("JobRadar API")
                        .description("개발자 취업준비생을 위한 채용공고 수집 및 대시보드 서비스 API")
                        .version("v1.0"))
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes(jwtSchemeName, securityScheme));
    }
}
