package com.pitterpetter.loventure.territory.api;

import com.pitterpetter.loventure.territory.application.UnlockService;
import com.pitterpetter.loventure.territory.domain.region.Region;
import com.pitterpetter.loventure.territory.domain.region.RegionRepository;
import com.pitterpetter.loventure.territory.dto.UnlockRequest;
import com.pitterpetter.loventure.territory.dto.UnlockResponse;
import com.pitterpetter.loventure.territory.dto.UnlockedOverviewResponse;
import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import com.pitterpetter.loventure.territory.util.CoupleHeaderResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class UnlockController {

    private final UnlockService unlockService;
    private final CoupleHeaderResolver coupleHeaderResolver;
    private final RegionRepository regionRepository; // ✅ 자동 매핑용

    /**
     * ✅ 지역 해금 API
     * POST /api/regions/unlock
     *
     * 요청 예시:
     * { "regionName": "강남구" }
     *
     * - 프론트는 regionName 하나만 보내면 됨.
     * - 백엔드에서 regionName으로 regionId, sigCd 자동 매핑.
     */
    @PostMapping("/unlock")
    public ResponseEntity<UnlockResponse> unlockRegion(
            @Valid @RequestBody UnlockRequest request,
            HttpServletRequest httpRequest
    ) {
        // ① JWT에서 coupleId 추출
        String coupleIdStr = coupleHeaderResolver.resolveCoupleId(httpRequest);
        Long coupleId = parseCoupleIdSafely(coupleIdStr);

        // ② regionName으로 Region 조회
        Region region = regionRepository.findByGuSi(request.getRegionName())
                .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));

        // ③ UnlockService에 매핑된 정보 전달
        UnlockResponse response = unlockService.unlockRegion(
                coupleId,
                region.getSigCd(),
                region.getId(),
                region.getGu_si()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ 해금 지역 조회 API
     * GET /api/regions/search?format=list|feature
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
