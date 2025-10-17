package com.pitterpetter.loventure.territory.api;

import com.pitterpetter.loventure.territory.util.CoupleHeaderResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions/unlock")
public class UnlockController {

    private final CoupleHeaderResolver coupleHeaderResolver; // ✅ 주입 받기

    @PostMapping
    public ResponseEntity<?> unlock(HttpServletRequest request) {
        String coupleId = coupleHeaderResolver.resolveCoupleId(request);

        Long coupleIdLong = null;
        try {
            coupleIdLong = Long.parseLong(coupleId);
        } catch (NumberFormatException e) {
            // 문자열이면 그냥 string으로 취급
        }

        return ResponseEntity.ok(Map.of(
                "message", "unlock success",
                "coupleId", coupleId,
                "coupleIdLong", coupleIdLong
        ));
    }
}
