package com.pitterpetter.loventure.territory.application;

import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegion;
import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegionRepository;
import com.pitterpetter.loventure.territory.domain.region.Region;
import com.pitterpetter.loventure.territory.domain.region.RegionRepository;
import com.pitterpetter.loventure.territory.dto.RegionSummary;
import com.pitterpetter.loventure.territory.dto.UnlockRequest;
import com.pitterpetter.loventure.territory.dto.UnlockResponse;
import com.pitterpetter.loventure.territory.dto.UnlockedResult;
import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import com.pitterpetter.loventure.territory.util.ValidationUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UnlockService {

    private static final String DEFAULT_UNLOCK_TYPE = "INIT";

    private final CoupleRegionRepository coupleRegionRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public UnlockResponse unlock(Long coupleId, UnlockRequest request) {
        Long verifiedCoupleId = ValidationUtils.requirePositive(coupleId, ErrorCode.INVALID_REQUEST);
        Region region = resolveRegion(request);

        CoupleRegion coupleRegion = coupleRegionRepository
            .findByCoupleIdAndRegion(verifiedCoupleId, region)
            .map(existing -> updateUnlock(existing, request))
            .orElseGet(() -> createUnlock(verifiedCoupleId, region, request));

        CoupleRegion saved = coupleRegionRepository.save(coupleRegion);
        return UnlockResponse.builder()
            .coupleId(saved.getCoupleId())
            .region(RegionSummary.from(saved.getRegion()))
            .isUnlocked(!saved.isLocked())
            .unlockType(saved.getUnlockType())
            .selectedBy(saved.getSelectedBy())
            .unlockedAt(convertToInstant(saved.getUnlockedAt()))
            .build();
    }

    @Transactional(readOnly = true)
    public List<UnlockedResult> findUnlockedRegions(Long coupleId) {
        Long verifiedCoupleId = ValidationUtils.requirePositive(coupleId, ErrorCode.INVALID_REQUEST);
        return coupleRegionRepository.findByCoupleIdAndIsLockedFalse(verifiedCoupleId).stream()
            .map(UnlockedResult::from)
            .collect(Collectors.toList());
    }

    private CoupleRegion updateUnlock(CoupleRegion coupleRegion, UnlockRequest request) {
        if (coupleRegion.isLocked()) {
            coupleRegion.setLocked(false);
            coupleRegion.setUnlockedAt(LocalDateTime.now());
        }
        applyMetadata(coupleRegion, request);
        return coupleRegion;
    }

    private CoupleRegion createUnlock(Long coupleId, Region region, UnlockRequest request) {
        CoupleRegion coupleRegion = CoupleRegion.builder()
            .coupleId(coupleId)
            .region(region)
            .isLocked(false)
            .unlockedAt(LocalDateTime.now())
            .build();
        applyMetadata(coupleRegion, request);
        return coupleRegion;
    }

    private void applyMetadata(CoupleRegion coupleRegion, UnlockRequest request) {
        if (StringUtils.hasText(request.getUnlockType())) {
            coupleRegion.setUnlockType(request.getUnlockType());
        } else if (!StringUtils.hasText(coupleRegion.getUnlockType())) {
            coupleRegion.setUnlockType(DEFAULT_UNLOCK_TYPE);
        }

        if (request.getSelectedBy() != null) {
            coupleRegion.setSelectedBy(request.getSelectedBy());
        }
    }

    private Region resolveRegion(UnlockRequest request) {
        if (request.getRegionId() != null) {
            Long regionId = ValidationUtils.requirePositive(request.getRegionId(), ErrorCode.INVALID_REQUEST);
            return regionRepository.findById(regionId)
                .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        }
        if (StringUtils.hasText(request.getSigCd())) {
            return regionRepository.findBySigCd(request.getSigCd())
                .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        }
        throw new ApiException(ErrorCode.INVALID_REQUEST);
    }

    private Instant convertToInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
