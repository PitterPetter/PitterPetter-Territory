package com.pitterpetter.loventure.territory.application;

import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegion;
import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegionRepository;
import com.pitterpetter.loventure.territory.domain.region.Region;
import com.pitterpetter.loventure.territory.domain.region.RegionRepository;
import com.pitterpetter.loventure.territory.dto.*;
import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import com.pitterpetter.loventure.territory.util.GeoJsonUtils;
import com.pitterpetter.loventure.territory.util.ValidationUtils;
import java.time.LocalDateTime;
import java.util.*;
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
     * ✅ 기존 Unlock 방식 (Request에 regionId, sigCd, regionName 중 하나 포함)
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
     * ✅ regionName 하나만 받아서 자동 매핑 처리하는 Unlock 버전
     */
    @Transactional
    @CacheEvict(value = "unlockedRegions", key = "#coupleId")
    public UnlockResponse unlockRegion(Long coupleId, String sigCd, String regionId, String regionName) {
        Long verifiedCoupleId = ValidationUtils.requirePositive(coupleId, ErrorCode.INVALID_REQUEST);

        // regionId, sigCd, regionName 중 우선순위로 Region 조회
        Region region = resolveRegionByDirectValues(regionId, sigCd, regionName);

        CoupleRegion coupleRegion = coupleRegionRepository
                .findByCoupleIdAndRegion(verifiedCoupleId, region)
                .map(this::updateUnlock)
                .orElseGet(() -> createUnlock(verifiedCoupleId, region));

        CoupleRegion saved = coupleRegionRepository.save(coupleRegion);
        return UnlockResponse.from(saved);
    }

    // -------------------- 공통 조회 로직 -------------------- //

    /**
     * ✅ regionId / sigCd / regionName 으로 Region 직접 조회
     */
    private Region resolveRegionByDirectValues(String regionId, String sigCd, String regionName) {
        if (regionId != null && !regionId.isBlank()) {
            return regionRepository.findById(regionId.trim())
                    .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        }
        if (sigCd != null && !sigCd.isBlank()) {
            return regionRepository.findBySigCd(sigCd)
                    .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        }
        if (regionName != null && !regionName.isBlank()) {
            String normalized = regionName.trim();
            return regionRepository.findByGuSi(normalized)
                    .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        }
        throw new ApiException(ErrorCode.INVALID_REQUEST);
    }

    /**
     * ✅ 기존 UnlockRequest 기반 Region 조회 (sigCd, regionId, regionName 대응)
     */
    private Region resolveRegion(UnlockRequest request) {
        if (request.getRegionId() != null && !request.getRegionId().isBlank()) {
            return regionRepository.findById(request.getRegionId().trim())
                    .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        }

        if (request.getSigCd() != null && !request.getSigCd().isBlank()) {
            return regionRepository.findBySigCd(request.getSigCd())
                    .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        }

        if (request.getRegionName() != null && !request.getRegionName().isBlank()) {
            String normalized = request.getRegionName().trim();
            Optional<Region> byName = regionRepository.findByGuSi(normalized);

            if (byName.isEmpty() && normalized.contains(" ")) {
                String lastSegment = normalized.substring(normalized.lastIndexOf(' ') + 1);
                byName = regionRepository.findByGuSi(lastSegment);
            }
            if (byName.isPresent()) return byName.get();

            if (normalized.matches("\\d+")) {
                return regionRepository.findBySigCd(normalized)
                        .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
            }
            throw new ApiException(ErrorCode.REGION_NOT_FOUND);
        }

        throw new ApiException(ErrorCode.INVALID_REQUEST);
    }

    // -------------------- 조회 관련 -------------------- //

    @Transactional(readOnly = true)
    public UnlockedOverviewResponse getUnlockedRegions(Long coupleId) {
        List<Region> allRegions = regionRepository.findAll();
        List<CoupleRegion> unlockedRegions = coupleRegionRepository.findByCoupleIdAndIsLockedFalse(coupleId);
        Set<String> unlockedIds = unlockedRegions.stream()
                .map(cr -> cr.getRegion().getId())
                .collect(Collectors.toSet());

        Map<String, List<Region>> grouped = allRegions.stream()
                .collect(Collectors.groupingBy(Region::getSi_do));

        List<CitySummary> cities = grouped.entrySet().stream()
                .map(entry -> {
                    String city = entry.getKey();
                    List<Region> regions = entry.getValue();

                    List<DistrictSummary> districts = regions.stream()
                            .map(r -> DistrictSummary.builder()
                                    .id(r.getSigCd())
                                    .name(r.getGu_si())
                                    .isLocked(!unlockedIds.contains(r.getId()))
                                    .build())
                            .collect(Collectors.toList());

                    long unlockedCount = districts.stream().filter(d -> !d.isLocked()).count();

                    return CitySummary.builder()
                            .cityName(city)
                            .totalDistricts(districts.size())
                            .lockedDistricts((int) (districts.size() - unlockedCount))
                            .unlockedDistricts((int) unlockedCount)
                            .districts(districts)
                            .build();
                })
                .collect(Collectors.toList());

        Map<String, Object> data = Map.of(
                "totalKeys", unlockedRegions.size(),
                "cities", cities
        );

        return UnlockedOverviewResponse.builder()
                .success(true)
                .data(data)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUnlockedRegionsAsFeature(Long coupleId) {
        List<CoupleRegion> coupleRegions = getUnlockedCoupleRegions(coupleId);
        List<Region> regions = coupleRegions.stream()
                .map(CoupleRegion::getRegion)
                .collect(Collectors.toList());
        return GeoJsonUtils.toFeatureCollection(regions);
    }

    private List<CoupleRegion> getUnlockedCoupleRegions(Long coupleId) {
        Long verifiedCoupleId = ValidationUtils.requirePositive(coupleId, ErrorCode.INVALID_REQUEST);
        return coupleRegionRepository.findByCoupleIdAndIsLockedFalse(verifiedCoupleId);
    }

    private CoupleRegion updateUnlock(CoupleRegion coupleRegion) {
        if (coupleRegion.isLocked()) {
            coupleRegion.setLocked(false);
            coupleRegion.setUnlockedAt(LocalDateTime.now());
        }
        return coupleRegion;
    }

    private CoupleRegion createUnlock(Long coupleId, Region region) {
        return CoupleRegion.builder()
                .coupleId(coupleId)
                .region(region)
                .isLocked(false)
                .unlockedAt(LocalDateTime.now())
                .build();
    }
}
