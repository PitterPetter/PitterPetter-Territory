package com.pitterpetter.loventure.territory.api;

import com.pitterpetter.loventure.territory.application.TerritoryService;
import com.pitterpetter.loventure.territory.dto.CheckResponse;
import com.pitterpetter.loventure.territory.dto.RegionSummary;
import com.pitterpetter.loventure.territory.util.CoupleHeaderResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/territory")
public class TerritoryController {

    private final TerritoryService territoryService;

    public TerritoryController(TerritoryService territoryService) {
        this.territoryService = territoryService;
    }

    @GetMapping("/check")
    public ResponseEntity<CheckResponse> check(@RequestParam("lat") double latitude,
                                               @RequestParam("lng") double longitude,
                                               HttpServletRequest request) {
        Long coupleId = CoupleHeaderResolver.resolveCoupleId(request);
        return ResponseEntity.ok(territoryService.checkTerritory(coupleId, latitude, longitude));
    }

    @GetMapping("/lookup")
    public ResponseEntity<RegionSummary> lookup(@RequestParam("lat") double latitude,
                                                @RequestParam("lng") double longitude) {
        return ResponseEntity.ok(territoryService.lookupTerritory(latitude, longitude));
    }
}
