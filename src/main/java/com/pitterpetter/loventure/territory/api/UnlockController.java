package com.pitterpetter.loventure.territory.api;

import com.pitterpetter.loventure.territory.application.UnlockService;
import com.pitterpetter.loventure.territory.dto.UnlockRequest;
import com.pitterpetter.loventure.territory.dto.UnlockResponse;
import com.pitterpetter.loventure.territory.dto.UnlockedOverviewResponse;
import com.pitterpetter.loventure.territory.util.CoupleHeaderResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class UnlockController {

    private final UnlockService unlockService;

    /**
     * ✅ 지역 해금 API
     * POST /api/regions/unlock
     * Body 예시: { "regions": "광진구" } or { "sigCd": "41500" } or { "regionId": 123 }
     */
    @PostMapping("/unlock")
    public ResponseEntity<UnlockResponse> unlock(
            @Valid @RequestBody UnlockRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Long coupleId = CoupleHeaderResolver.resolveCoupleId(httpServletRequest);
        return ResponseEntity.ok(unlockService.unlock(coupleId, request));
    }

    /**
     * ✅ 해금 지역 조회 API
     * GET /api/regions/search?format=list|feature
     *
     * - format=list → 프론트 요구 JSON 구조(totalKeys, cities, districts)
     * - format=feature → GeoJSON FeatureCollection 반환
     */
    @GetMapping("/search")
    public ResponseEntity<?> unlocked(
            HttpServletRequest httpServletRequest,
            @RequestParam(value = "format", defaultValue = "list") String format
    ) {
        Long coupleId = CoupleHeaderResolver.resolveCoupleId(httpServletRequest);

        if ("feature".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(unlockService.getUnlockedRegionsAsFeature(coupleId));
        }

        // 기본 list 포맷 → UnlockedOverviewResponse(JSON grouped by city)
        UnlockedOverviewResponse response = unlockService.getUnlockedRegions(coupleId);
        return ResponseEntity.ok(response);
    }
}
