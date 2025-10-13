package com.pitterpetter.loventure.territory.api;

import com.pitterpetter.loventure.territory.application.UnlockService;
import com.pitterpetter.loventure.territory.dto.UnlockRequest;
import com.pitterpetter.loventure.territory.dto.UnlockResponse;
import com.pitterpetter.loventure.territory.dto.UnlockedResult;
import com.pitterpetter.loventure.territory.util.CoupleHeaderResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/territory")
@RequiredArgsConstructor
public class UnlockController {

    private final UnlockService unlockService;

    @PostMapping("/unlock")
    public ResponseEntity<UnlockResponse> unlock(@Valid @RequestBody UnlockRequest request,
                                                 HttpServletRequest httpServletRequest) {
        Long coupleId = CoupleHeaderResolver.resolveCoupleId(httpServletRequest);
        return ResponseEntity.ok(unlockService.unlock(coupleId, request));
    }

    @GetMapping("/unlocked")
    public ResponseEntity<List<UnlockedResult>> unlocked(HttpServletRequest httpServletRequest) {
        Long coupleId = CoupleHeaderResolver.resolveCoupleId(httpServletRequest);
        return ResponseEntity.ok(unlockService.findUnlockedRegions(coupleId));
    }
}
