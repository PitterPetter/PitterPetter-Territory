package com.pitterpetter.loventure.territory.infra;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Auth 서비스와 통신하기 위한 FeignClient
 * - JWT 검증용
 * - 티켓 차감용
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

    /**
     * ✅ 티켓 차감 요청 (Gateway용)
     * Territory Service에서 Auth Service로 티켓 차감 요청
     */
    @PostMapping("/api/couples/{coupleId}/ticket/consume")
    void consumeTicket(
            @PathVariable("coupleId") String coupleId,
            @RequestHeader("Authorization") String token
    );

    /**
     * ✅ 티켓 차감 및 Rock 완료 요청 (init unlock용)
     * Territory Service에서 초기 해금 시 사용자 상태도 함께 변경
     */
    @PostMapping("/api/couples/{coupleId}/ticket/consume-and-complete")
    void consumeTicketAndCompleteRock(
            @PathVariable("coupleId") String coupleId,
            @RequestHeader("Authorization") String token
    );
}
