package com.pitterpetter.loventure.territory.dto;

import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegion;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record UnlockedResult(Long coupleRegionId,
                             Long coupleId,
                             RegionSummary region,
                             Instant unlockedAt,
                             String unlockType,
                             String selectedBy) {

    public static UnlockedResult from(CoupleRegion coupleRegion) {
        return new UnlockedResult(
            coupleRegion.getId(),
            coupleRegion.getCoupleId(),
            RegionSummary.from(coupleRegion.getRegion()),
            toInstant(coupleRegion.getUnlockedAt()),
            coupleRegion.getUnlockType(),
            coupleRegion.getSelectedBy()
        );
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
