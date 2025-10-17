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
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class UnlockController {

    private final UnlockService unlockService;
    private final CoupleHeaderResolver coupleHeaderResolver;

    /**
     * ✅ 여러 지역 해금 API
     * POST /api/regions/unlock
     * {
     *   "regions": ["광진구", "노원구"]
     * }
     */
    @PostMapping("/unlock")
    public ResponseEntity<?> unlockRegions(
            @Valid @RequestBody UnlockRequest request,
            HttpServletRequest httpRequest
    ) {
        String coupleIdStr = coupleHeaderResolver.resolveCoupleId(httpRequest);
        Long coupleId = parseCoupleIdSafely(coupleIdStr);

        List<UnlockResponse> results = unlockService.unlockMultipleRegions(coupleId, request.getRegionNames());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "coupleId", coupleId,
                "unlockedCount", results.size(),
                "unlockedRegions", results
        ));
    }

    /**
     * ✅ 해금 지역 조회 API
     */
    @GetMapping("/search")
    public ResponseEntity<?> unlockedRegions(
            HttpServletRequest httpRequest,
            @RequestParam(value = "format", defaultValue = "list") String format
    ) {
        String coupleIdStr = coupleHeaderResolver.resolveCoupleId(httpRequest);
        Long coupleId = parseCoupleIdSafely(coupleIdStr);

        if ("feature".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(unlockService.getUnlockedRegionsAsFeature(coupleId));
        }

        UnlockedOverviewResponse response = unlockService.getUnlockedRegions(coupleId);
        return ResponseEntity.ok(response);
    }

    private Long parseCoupleIdSafely(String coupleId) {
        try {
            return Long.parseLong(coupleId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
