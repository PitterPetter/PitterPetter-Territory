package com.pitterpetter.loventure.territory.util;

import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("local") // 🔥 local 프로필에서만 활성화
public class CoupleHeaderResolverLocal extends CoupleHeaderResolver {

    @Value("${spring.jwt.secret:}")
    private String jwtSecret;

    public CoupleHeaderResolverLocal(@Value("${spring.jwt.secret:}") String jwtSecret) {
        super(jwtSecret);
    }

    @Override
    public String resolveCoupleId(HttpServletRequest request) {
        // Swagger나 Postman 테스트용 헤더
        String coupleHeader = request.getHeader("COUPLE-ID");
        if (coupleHeader != null && !coupleHeader.isBlank()) {
            return coupleHeader;
        }

        // 기본값 (Swagger 테스트용)
        return "2";
    }
}
