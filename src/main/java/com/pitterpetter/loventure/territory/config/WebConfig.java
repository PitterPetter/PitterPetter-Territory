package com.pitterpetter.loventure.territory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 허용할 Origin (배포 환경 + 로컬 개발 환경)
                .allowedOriginPatterns(
                        "https://loventure.us",
                        "https://*.loventure.us",
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )

                // 허용할 HTTP 메서드
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")

                // 허용할 요청 헤더
                .allowedHeaders("*")

                // 응답 시 노출할 커스텀 헤더 (JWT나 couple-id 등)
                .exposedHeaders("X-User-Id", "X-Couple-Id")

                // 쿠키 / 인증정보 포함 허용
                .allowCredentials(true)

                // preflight 캐시 시간 (1시간)
                .maxAge(3600);
    }
}
