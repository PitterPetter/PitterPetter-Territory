package com.pitterpetter.loventure.territory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckResponse {
    public enum Reason { UNLOCKED_REGION, LOCKED_REGION, OUT_OF_COVERAGE }
    private boolean ok;
    private Reason reason;
    private RegionSummary region;
}
