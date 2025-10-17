package com.pitterpetter.loventure.territory.dto;

import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegion;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record UnlockedResult(Long coupleRegionId,
                             String coupleId,
                             RegionSummary region,
                             Instant unlockedAt) {

    public static UnlockedResult from(CoupleRegion coupleRegion) {
        return new UnlockedResult(
            coupleRegion.getId(),
            coupleRegion.getCoupleId(),
            RegionSummary.from(coupleRegion.getRegion()),
            toInstant(coupleRegion.getUnlockedAt())
        );
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
