package com.pitterpetter.loventure.territory.api;

import com.pitterpetter.loventure.territory.application.TerritoryService;
import com.pitterpetter.loventure.territory.dto.CheckResponse;
import com.pitterpetter.loventure.territory.dto.LookupResponse;
import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import com.pitterpetter.loventure.territory.util.CoupleHeaderResolver;
import com.pitterpetter.loventure.territory.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class TerritoryController {

    private final TerritoryService territoryService;
    private final CoupleHeaderResolver coupleHeaderResolver; // ✅ Bean 주입

    /**
     * ✅ 좌표 기반 지역 체크 API
     * GET /api/regions/check?lon=127.0&lat=37.5
     *
     * - 사용자의 현재 위치(lon, lat)가 어떤 행정구역에 속하는지 확인
     * - 커플 ID는 JWT에서 추출하여 인증된 요청만 허용
     */
    @GetMapping("/check")
    public ResponseEntity<CheckResponse> check(
            @RequestParam("lon") double lon,
            @RequestParam("lat") double lat,
            HttpServletRequest request
    ) {
        ValidationUtils.validateLonLat(lon, lat);

        // ✅ JWT에서 coupleId 추출
        String coupleId = normalizeCoupleId(coupleHeaderResolver.resolveCoupleId(request));
        if (coupleId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "coupleId 헤더가 비어 있습니다.");
        }

        return ResponseEntity.ok(territoryService.check(coupleId, lon, lat));
    }

    /**
     * ✅ 좌표 기반 행정구역 조회 API
     * GET /api/regions/lookup?lon=127.0&lat=37.5
     *
     * - 단순 조회용 (커플 인증 불필요)
     * - 사용자의 좌표가 속한 행정구역 정보를 반환
     */
    @GetMapping("/lookup")
    public ResponseEntity<LookupResponse> lookup(
            @RequestParam("lon") double lon,
            @RequestParam("lat") double lat
    ) {
        ValidationUtils.validateLonLat(lon, lat);
        return ResponseEntity.ok(territoryService.lookup(lon, lat));
    }

    /**
     * ✅ JWT에서 추출된 coupleId 상태 확인용 (테스트 및 디버그용)
     * GET /api/regions/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(HttpServletRequest request) {
        String coupleId = normalizeCoupleId(coupleHeaderResolver.resolveCoupleId(request));
        if (coupleId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "coupleId 헤더가 비어 있습니다.");
        }

        return ResponseEntity.ok(Map.of(
                "coupleId", coupleId
        ));
    }

    /**
     * ✅ JWT 내부 coupleId 문자열이 Long 변환 불가할 경우 fallback
     */
    private String normalizeCoupleId(String coupleId) {
        if (coupleId == null) {
            return null;
        }
        String trimmed = coupleId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
