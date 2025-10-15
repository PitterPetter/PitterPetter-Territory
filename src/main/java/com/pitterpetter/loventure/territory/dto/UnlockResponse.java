package com.pitterpetter.loventure.territory.dto;

import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegion;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record UnlockResponse(Long coupleId,
                             RegionSummary region,
                             boolean unlocked,
                             Instant unlockedAt,
                             String unlockType,
                             String selectedBy) {

    public static UnlockResponse from(CoupleRegion coupleRegion) {
        return new UnlockResponse(
            coupleRegion.getCoupleId(),
            RegionSummary.from(coupleRegion.getRegion()),
            !coupleRegion.isLocked(),
            toInstant(coupleRegion.getUnlockedAt()),
            coupleRegion.getUnlockType(),
            coupleRegion.getSelectedBy()
        );
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
