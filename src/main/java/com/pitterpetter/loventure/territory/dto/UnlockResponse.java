package com.pitterpetter.loventure.territory.dto;

import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegion;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record UnlockResponse(String coupleId,
                             RegionSummary region,
                             boolean unlocked,
                             Instant unlockedAt) {

    public static UnlockResponse from(CoupleRegion coupleRegion) {
        return new UnlockResponse(
            coupleRegion.getCoupleId(),
            RegionSummary.from(coupleRegion.getRegion()),
            !coupleRegion.isLocked(),
            toInstant(coupleRegion.getUnlockedAt())
        );
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
