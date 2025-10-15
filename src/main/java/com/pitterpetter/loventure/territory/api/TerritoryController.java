package com.pitterpetter.loventure.territory.api;

import com.pitterpetter.loventure.territory.application.TerritoryService;
import com.pitterpetter.loventure.territory.dto.CheckResponse;
import com.pitterpetter.loventure.territory.dto.LookupResponse;
import com.pitterpetter.loventure.territory.util.CoupleHeaderResolver;
import com.pitterpetter.loventure.territory.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class TerritoryController {

    private final TerritoryService territoryService;

    @GetMapping("/check")
    public ResponseEntity<CheckResponse> check(@RequestParam("lon") double lon,
                                               @RequestParam("lat") double lat,
                                               HttpServletRequest request) {
        ValidationUtils.validateLonLat(lon, lat);
        Long coupleId = CoupleHeaderResolver.resolveCoupleId(request);
        return ResponseEntity.ok(territoryService.check(coupleId, lon, lat));
    }

    @GetMapping("/lookup")
    public ResponseEntity<LookupResponse> lookup(@RequestParam("lon") double lon,
                                                 @RequestParam("lat") double lat) {
        ValidationUtils.validateLonLat(lon, lat);
        return ResponseEntity.ok(territoryService.lookup(lon, lat));
    }
}
