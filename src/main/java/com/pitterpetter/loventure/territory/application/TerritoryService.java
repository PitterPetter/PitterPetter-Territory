package com.pitterpetter.loventure.territory.application;

import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegion;
import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegionRepository;
import com.pitterpetter.loventure.territory.domain.region.Region;
import com.pitterpetter.loventure.territory.domain.region.RegionRepository;
import com.pitterpetter.loventure.territory.dto.CheckResponse;
import com.pitterpetter.loventure.territory.dto.LookupResponse;
import com.pitterpetter.loventure.territory.dto.RegionSummary;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import com.pitterpetter.loventure.territory.util.ValidationUtils;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TerritoryService {

    private final RegionRepository regionRepository;
    private final CoupleRegionRepository coupleRegionRepository;

    public CheckResponse check(String coupleId, double lon, double lat) {
        ValidationUtils.validateLonLat(lon, lat);
        String verifiedCoupleId = ValidationUtils.requireNonBlank(coupleId, ErrorCode.INVALID_REQUEST);
        Optional<Region> regionOptional = findRegionByPoint(lon, lat);

        if (regionOptional.isEmpty()) {
            return CheckResponse.builder()
                .ok(false)
                .reason(CheckResponse.Reason.OUT_OF_COVERAGE)
                .region(null)
                .build();
        }

        Region region = regionOptional.get();
        boolean unlocked = coupleRegionRepository.findByCoupleIdAndRegion(verifiedCoupleId, region)
            .map(CoupleRegion::isLocked)
            .map(locked -> !locked)
            .orElse(false);

        CheckResponse.Reason reason = unlocked
            ? CheckResponse.Reason.UNLOCKED_REGION
            : CheckResponse.Reason.LOCKED_REGION;

        return CheckResponse.builder()
            .ok(unlocked)
            .reason(reason)
            .region(RegionSummary.from(region))
            .build();
    }

    public LookupResponse lookup(double lon, double lat) {
        ValidationUtils.validateLonLat(lon, lat);
        return findRegionByPoint(lon, lat)
            .map(LookupResponse::inCoverage)
            .orElseGet(LookupResponse::outOfCoverage);
    }

    private Optional<Region> findRegionByPoint(double lon, double lat) {
        String pointWkt = String.format(Locale.US, "POINT(%f %f)", lon, lat);
        return regionRepository.findRegionByPoint(pointWkt);
    }
}
