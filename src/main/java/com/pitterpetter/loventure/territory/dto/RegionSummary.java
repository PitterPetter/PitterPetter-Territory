package com.pitterpetter.loventure.territory.dto;

import com.pitterpetter.loventure.territory.domain.region.Region;

public record RegionSummary(String id, String sigCd, String guSi, String siDo) {

    public static RegionSummary from(Region region) {
        return new RegionSummary(
            region.getId(),
            region.getSigCd(),
            region.getGu_si(),
            region.getSi_do()
        );
    }

    public static RegionSummary empty() {
        return new RegionSummary(null, null, null, null);
    }
}
