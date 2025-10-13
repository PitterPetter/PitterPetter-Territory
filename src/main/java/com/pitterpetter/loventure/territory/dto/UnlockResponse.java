package com.pitterpetter.loventure.territory.dto;

import lombok.*;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnlockResponse {
    private Long coupleId;
    private RegionSummary region;
    private boolean isUnlocked;
    private Instant unlockedAt;
    private String unlockType;   // INIT/TICKET
    private String selectedBy;   // male/female
}
