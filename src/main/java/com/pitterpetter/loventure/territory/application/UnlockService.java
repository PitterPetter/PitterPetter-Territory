package com.pitterpetter.loventure.territory.application;

import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegion;
import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegionRepository;
import com.pitterpetter.loventure.territory.domain.region.Region;
import com.pitterpetter.loventure.territory.domain.region.RegionRepository;
import com.pitterpetter.loventure.territory.dto.UnlockRequest;
import com.pitterpetter.loventure.territory.dto.UnlockResponse;
import com.pitterpetter.loventure.territory.dto.UnlockedResult;
import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import com.pitterpetter.loventure.territory.util.GeoJsonUtils;
import com.pitterpetter.loventure.territory.util.ValidationUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnlockService {

    private final CoupleRegionRepository coupleRegionRepository;
    private final RegionRepository regionRepository;

    /**
     * 커플이 지역을 해금하는 서비스 메서드
     */
    @Transactional
    @CacheEvict(value = "unlockedRegions", key = "#coupleId")
    public UnlockResponse unlock(Long coupleId, UnlockRequest request) {
        Long verifiedCoupleId = ValidationUtils.requirePositive(coupleId, ErrorCode.INVALID_REQUEST);
        Region region = resolveRegion(request);

        CoupleRegion coupleRegion = coupleRegionRepository
                .findByCoupleIdAndRegion(verifiedCoupleId, region)
                .map(this::updateUnlock)
                .orElseGet(() -> createUnlock(verifiedCoupleId, region));

        CoupleRegion saved = coupleRegionRepository.save(coupleRegion);
        return UnlockResponse.from(saved);
    }

    /**
     * 해제된 지역 목록 조회 (리스트)
     */
    @Transactional(readOnly = true)
    public List<UnlockedResult> getUnlockedRegions(Long coupleId) {
        return getUnlockedCoupleRegions(coupleId).stream()
                .map(UnlockedResult::from)
                .collect(Collectors.toList());
    }

    /**
     * 해제된 지역 목록 조회 (GeoJSON FeatureCollection)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUnlockedRegionsAsFeature(Long coupleId) {
        List<CoupleRegion> coupleRegions = getUnlockedCoupleRegions(coupleId);
        List<Region> regions = coupleRegions.stream()
                .map(CoupleRegion::getRegion)
                .collect(Collectors.toList());
        return GeoJsonUtils.toFeatureCollection(regions);
    }

    /**
     * 커플의 해제된 구역 조회
     */
    private List<CoupleRegion> getUnlockedCoupleRegions(Long coupleId) {
        Long verifiedCoupleId = ValidationUtils.requirePositive(coupleId, ErrorCode.INVALID_REQUEST);
        return coupleRegionRepository.findByCoupleIdAndIsLockedFalse(verifiedCoupleId);
    }

    /**
     * 이미 존재하는 해금 이력 갱신
     */
    private CoupleRegion updateUnlock(CoupleRegion coupleRegion) {
        if (coupleRegion.isLocked()) {
            coupleRegion.setLocked(false);
            coupleRegion.setUnlockedAt(LocalDateTime.now());
        }
        return coupleRegion;
    }

    /**
     * 새로운 해금 이력 생성
     */
    private CoupleRegion createUnlock(Long coupleId, Region region) {
        return CoupleRegion.builder()
                .coupleId(coupleId)
                .region(region)
                .isLocked(false)
                .unlockedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 요청의 regionId 또는 sigCd로 Region 조회
     */
    private Region resolveRegion(UnlockRequest request) {
        if (request.getRegionId() != null) {
            Long regionId = ValidationUtils.requirePositive(request.getRegionId(), ErrorCode.INVALID_REQUEST);
            return regionRepository.findById(regionId)
                    .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        }
        if (request.getSigCd() != null && !request.getSigCd().isEmpty()) {
            return regionRepository.findBySigCd(request.getSigCd())
                    .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        }
        throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
}
