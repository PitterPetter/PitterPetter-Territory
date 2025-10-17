package com.pitterpetter.loventure.territory.util;

import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * JWT Authorization 헤더에서 coupleId를 안전하게 추출하는 Resolver.
 * - Auth 서비스에서 발급된 JWT 토큰을 Territory 서비스에서 검증 및 해석
 * - Base64 단순 디코딩이 아닌 HMAC-SHA256 서명 검증 수행
 */
@Slf4j
@Component
public class CoupleHeaderResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> COUPLE_ID_KEYS = List.of(
            "coupleId", "couple_id", "coupleID", "couple", "couple-id"
    );

    private final Key secretKey;

    public CoupleHeaderResolver(@Value("${spring.jwt.secret}") String secret) {
        Key keyTemp;
        try {
            // Auth 서비스와 동일한 secret 키(Base64 인코딩된 값)를 디코딩
            byte[] decodedKey = Base64.getDecoder().decode(secret);
            keyTemp = Keys.hmacShaKeyFor(decodedKey);
            log.info("✅ JWT 시크릿 키(Base64) 로드 완료 (길이: {})", decodedKey.length);
        } catch (IllegalArgumentException e) {
            // Base64 decode 실패 시 문자열을 그대로 사용 (fallback)
            keyTemp = Keys.hmacShaKeyFor(secret.getBytes());
            log.warn("⚠️ Base64 디코딩 실패, 문자열 기반 키로 대체 (길이: {})", secret.getBytes().length);
        }
        this.secretKey = keyTemp;
    }

    /**
     * 요청 헤더에서 JWT를 추출하고 서명 검증 후 coupleId 반환
     */
    public String resolveCoupleId(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ApiException(ErrorCode.AUTH_HEADER_MISSING);
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        try {
            // ✅ 토큰 파싱 및 서명 검증
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // ✅ coupleId 추출
            String coupleId = extractCoupleId(claims)
                    .orElseThrow(() -> new ApiException(ErrorCode.AUTH_TOKEN_INVALID));

            log.debug("✅ JWT 검증 성공, coupleId = {}", coupleId);
            return coupleId;

        } catch (SignatureException e) {
            log.error("❌ JWT 서명 검증 실패: {}", e.getMessage());
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        } catch (ExpiredJwtException e) {
            log.error("❌ JWT 토큰 만료: {}", e.getMessage());
            throw new ApiException(ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (Exception e) {
            log.error("❌ JWT 파싱 오류: {}", e.getMessage(), e);
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    /**
     * Claims 객체에서 coupleId를 다양한 키 이름으로 탐색
     */
    private Optional<String> extractCoupleId(Claims claims) {
        for (String key : COUPLE_ID_KEYS) {
            Object value = claims.get(key);
            if (value != null) {
                return Optional.of(value.toString());
            }
        }
        return Optional.empty();
    }
}
