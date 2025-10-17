package com.pitterpetter.loventure.territory.api;

import com.pitterpetter.loventure.territory.util.CoupleHeaderResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
public class TerritoryController {

    private final CoupleHeaderResolver coupleHeaderResolver; // ✅ 주입 받기

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(HttpServletRequest request) {
        // ✅ Bean 주입을 통해 인스턴스 메서드로 호출
        String coupleId = coupleHeaderResolver.resolveCoupleId(request);

        // 필요 시 Long으로 변환
        Long coupleIdLong = null;
        try {
            coupleIdLong = Long.parseLong(coupleId);
        } catch (NumberFormatException e) {
            // JWT의 coupleId가 문자열이면 그대로 사용
        }

        return ResponseEntity.ok(Map.of(
                "coupleId", coupleId,
                "coupleIdLong", coupleIdLong
        ));
    }
}
