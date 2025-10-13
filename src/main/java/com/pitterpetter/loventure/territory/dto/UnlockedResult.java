package com.pitterpetter.loventure.territory.dto;

import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegion;

public record UnlockedResult(Long coupleRegionId, Long coupleId, RegionSummary region) {

    public static UnlockedResult from(CoupleRegion coupleRegion) {
        return new UnlockedResult(
            coupleRegion.getId(),
            coupleRegion.getCoupleId(),
            RegionSummary.from(coupleRegion.getRegion())
        );
    }

    public static UnlockedResult empty() {
        return new UnlockedResult(null, null, RegionSummary.empty());
    }
}
