package kr.co.awesomelead.groupware_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info().title("어썸리드 API").version("v1.0.0").description("어썸리드 API 입니다");

        // JWT 인증 스키마 정의
        SecurityScheme securityScheme =
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // Security Requirement 정의
        SecurityRequirement securityRequirement =
            new SecurityRequirement().addList("Bearer Authentication");

        return new OpenAPI()
            .components(
                new Components()
                    .addSecuritySchemes("Bearer Authentication", securityScheme))
            .addSecurityItem(securityRequirement)
            .addServersItem(new Server().url("/"))
            .info(info);
    }
}
