package com.pitterpetter.loventure.territory.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pitterpetter.loventure.territory.application.UnlockService;
import com.pitterpetter.loventure.territory.dto.UnlockListResponse;
import com.pitterpetter.loventure.territory.dto.UnlockRequest;
import com.pitterpetter.loventure.territory.dto.UnlockResponse;
import com.pitterpetter.loventure.territory.dto.UnlockedOverviewResponse;
import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import com.pitterpetter.loventure.territory.util.CoupleHeaderResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 🌐 지역 해금 컨트롤러
 * - 초기 해금(init): Auth 서버 검증 포함
 * - 티켓 해금(reward): Redis 검증 포함
 * - 해금 조회(search)
 */
@Slf4j
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class UnlockController {

    private final UnlockService unlockService;
    private final CoupleHeaderResolver coupleHeaderResolver;

    // ========================================================================
    // ✅ [1] 초기 해금 (Init Unlock)
    // 프론트 → Territory → Auth 검증 → OK 시 해금
    // ========================================================================
    @PostMapping("/unlock/init")
    public ResponseEntity<UnlockListResponse> initUnlock(
            @Valid @RequestBody UnlockRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("🔓 [Init Unlock] 요청 수신: {}", request);

        // ① JWT에서 coupleId 추출
        String coupleIdStr = coupleHeaderResolver.resolveCoupleId(httpRequest);
        String coupleId = normalizeCoupleId(coupleIdStr);
        if (coupleId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "coupleId 헤더가 비어 있습니다.");
        }

        // ② Auth 검증
        boolean verified = unlockService.verifyAuthToken(coupleId, httpRequest);
        if (!verified) {
            log.warn("❌ [Init Unlock] Auth 검증 실패 (coupleId={})", coupleId);
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID, "Auth 서버 검증 실패");
        }

        // ③ 티켓 차감 및 Rock 완료 (init unlock용)
        boolean ticketConsumed = unlockService.consumeTicketFromAuthService(coupleId, httpRequest);
        if (!ticketConsumed) {
            log.warn("❌ [Init Unlock] 티켓 차감 실패 - 티켓 부족 (coupleId={})", coupleId);
            throw new ApiException(ErrorCode.INVALID_REQUEST, "티켓이 부족합니다");
        }

        // ④ 실제 지역 해금 수행
        List<UnlockResponse> results = unlockService.unlockMultipleRegions(coupleId, request.getRegionNames());
        log.info("✅ [Init Unlock] 해금 완료 (count={}, coupleId={})", results.size(), coupleId);

        return ResponseEntity.ok(
                UnlockListResponse.builder()
                        .success(true)
                        .count(results.size())
                        .data(results)
                        .build()
        );
    }

    // ========================================================================
    // ✅ [2] 티켓 해금 (Reward Unlock)
    // 프론트 → Gateway → Redis → Territory
    // Auth는 관여 X (게이트웨이/JWT 검증만)
    // ========================================================================
    @PostMapping("/unlock/reward")
    public ResponseEntity<UnlockListResponse> rewardUnlock(
            @Valid @RequestBody UnlockRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("🎟️ [Reward Unlock] 요청 수신: {}", request);

        // ① JWT에서 coupleId 추출
        String coupleIdStr = coupleHeaderResolver.resolveCoupleId(httpRequest);
        String coupleId = normalizeCoupleId(coupleIdStr);
        if (coupleId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "coupleId 헤더가 비어 있습니다.");
        }

        // ② Gateway에서 전달받은 티켓 정보 처리
        String ticketCountHeader = httpRequest.getHeader("X-Ticket-Count");
        if (ticketCountHeader != null) {
            try {
                int ticketCount = Integer.parseInt(ticketCountHeader);
                unlockService.setTicketCountFromGateway(coupleId, ticketCount);
                log.info("🎟️ Gateway에서 전달받은 티켓 정보 저장 - coupleId: {}, ticketCount: {}", coupleId, ticketCount);
            } catch (NumberFormatException e) {
                log.warn("⚠️ 잘못된 티켓 개수 형식 - coupleId: {}, ticketCount: {}", coupleId, ticketCountHeader);
            }
        }

        // ③ Redis 티켓 검증
        boolean validTicket = unlockService.verifyRedisTicket(coupleId);
        if (!validTicket) {
            log.warn("❌ [Reward Unlock] 티켓 부족 (coupleId={})", coupleId);
            throw new ApiException(ErrorCode.INVALID_REQUEST, "티켓 잔여 수량 부족");
        }

        // ③ 해금 처리
        List<UnlockResponse> results = unlockService.unlockMultipleRegions(coupleId, request.getRegionNames());
        log.info("✅ [Reward Unlock] 해금 완료 (count={}, coupleId={})", results.size(), coupleId);

        return ResponseEntity.ok(
                UnlockListResponse.builder()
                        .success(true)
                        .count(results.size())
                        .data(results)
                        .build()
        );
    }

    // ========================================================================
    // ✅ [3] 해금 지역 조회 API
    // ========================================================================
    @GetMapping("/search")
    public ResponseEntity<?> unlockedRegions(
            HttpServletRequest httpRequest,
            @RequestParam(value = "format", defaultValue = "list") String format
    ) {
        String coupleIdStr = coupleHeaderResolver.resolveCoupleId(httpRequest);
        String coupleId = normalizeCoupleId(coupleIdStr);
        if (coupleId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "coupleId 헤더가 비어 있습니다.");
        }
        log.info("📍 [Search] 해금 지역 조회 요청 (coupleId={}, format={})", coupleId, format);

        if ("feature".equalsIgnoreCase(format)) {
            log.debug("📄 GeoJSON FeatureCollection 형태 반환");
            return ResponseEntity.ok(unlockService.getUnlockedRegionsAsFeature(coupleId));
        }

        UnlockedOverviewResponse response = unlockService.getUnlockedRegions(coupleId);
        log.debug("📦 [Search] 리스트형 응답 반환 (도시 수={})", response.getData().size());
        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // ✅ 내부 유틸
    // ========================================================================
    private String normalizeCoupleId(String coupleId) {
        if (coupleId == null || coupleId.isBlank()) {
            log.warn("⚠️ 비어있는 coupleId 헤더");
            return null;
        }
        return coupleId.trim();
    }
}
