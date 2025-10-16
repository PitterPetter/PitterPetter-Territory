package com.pitterpetter.loventure.territory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PitterPetter Territory API")
                        .version("v1.0.0")
                        .description("커플 지역락 Territory 서비스 API 문서"))
                .addSecurityItem(new SecurityRequirement().addList("coupleHeader"))
                .components(new Components()
                        .addSecuritySchemes("coupleHeader",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-COUPLE-ID")
                                        .description("커플 식별용 헤더 (필수)")));
    }
}
