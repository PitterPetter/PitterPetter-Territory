package com.pitterpetter.loventure.territory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI territoryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Territory Service API")
                        .description("러브벤처 커플 지역 관리 및 잠금 해제 API 문서")
                        .version("1.0.0"));
    }
}
