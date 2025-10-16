package com.pitterpetter.loventure.territory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 허용 Origin: 프론트, Auth, AI 등 모든 MSA 호출 허용
        config.addAllowedOriginPattern("https://loventure.us");
        config.addAllowedOriginPattern("https://*.loventure.us");
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("http://127.0.0.1:*");

        // 메서드, 헤더 전부 허용
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        // JWT 헤더 전달 허용
        config.setAllowCredentials(true);

        // preflight 캐시 (1시간)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
