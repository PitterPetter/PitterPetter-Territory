package com.pitterpetter.loventure.territory.infra;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Auth 서비스와 통신하기 위한 FeignClient
 * - JWT 검증용
 * - 내부 MSA 간 통신: Bearer 토큰 전달
 */
@FeignClient(
        name = "authClient",
        url = "${auth.service.url}"  // 예: http://loventure-prod-auth-service.loventure-app.svc.cluster.local:8081
)
public interface AuthClient {

    /**
     * ✅ JWT 검증 요청
     * 내부 MSA 통신이라 Authorization 헤더만 전달하면 됨
     */
    @GetMapping("/internal/api/regions/verify")
    void verifyToken(
            @RequestHeader("Authorization") String token
    );
}
