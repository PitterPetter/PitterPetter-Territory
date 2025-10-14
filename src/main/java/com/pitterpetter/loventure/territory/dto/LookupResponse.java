package com.pitterpetter.loventure.territory.dto;

import com.pitterpetter.loventure.territory.domain.region.Region;

public record LookupResponse(boolean inCoverage, RegionSummary region) {

    public static LookupResponse inCoverage(Region region) {
        return new LookupResponse(true, RegionSummary.from(region));
    }

    public static LookupResponse outOfCoverage() {
        return new LookupResponse(false, null);
    }
}
