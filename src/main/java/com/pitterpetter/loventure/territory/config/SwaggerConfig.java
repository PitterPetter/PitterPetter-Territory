package com.pitterpetter.loventure.territory.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public OpenAPI openAPI() {

        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("PitterPetter Territory API")
                        .version("v1.0.0")
                        .description("커플 지역락 Territory 서비스 API 문서"));

        // ✅ 운영 환경에서만 BearerAuth 추가
        if (!"local".equalsIgnoreCase(activeProfile)) {
            openAPI.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                    .components(new Components()
                            .addSecuritySchemes("bearerAuth",
                                    new SecurityScheme()
                                            .name("bearerAuth")
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")
                                            .description("JWT 인증 토큰 (Bearer <token>) 사용")));
        }

        return openAPI;
    }
}
