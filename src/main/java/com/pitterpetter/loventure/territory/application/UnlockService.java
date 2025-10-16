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
     * 해제된 지역 목록 조회 (프론트 요구 JSON 구조로 반환)
     */
    @Transactional(readOnly = true)
    public UnlockedOverviewResponse getUnlockedRegions(Long coupleId) {
        // 1. 모든 행정구역
        List<Region> allRegions = regionRepository.findAll();

        // 2. 커플이 해금한 지역 조회
        List<CoupleRegion> unlockedRegions = coupleRegionRepository.findByCoupleIdAndIsLockedFalse(coupleId);
        Set<Long> unlockedIds = unlockedRegions.stream()
                .map(cr -> cr.getRegion().getId())
                .collect(Collectors.toSet());

        // 3. 시/도 단위 그룹핑
        Map<String, List<Region>> grouped = allRegions.stream()
                .collect(Collectors.groupingBy(Region::getSi_do));

        // 4. 각 시/도별 구/군 구성
        List<CitySummary> cities = grouped.entrySet().stream()
                .map(entry -> {
                    String city = entry.getKey();
                    List<Region> regions = entry.getValue();

                    List<DistrictSummary> districts = regions.stream()
                            .map(r -> DistrictSummary.builder()
                                    .id(r.getSigCd())
                                    .name(r.getGu_si())
                                    .isLocked(!unlockedIds.contains(r.getId()))
                                    .description(null) // 필요 시 설명 추가 가능
                                    .lat(null)
                                    .lng(null)
                                    .build())
                            .collect(Collectors.toList());

                    long unlockedCount = districts.stream()
                            .filter(d -> !d.isLocked())
                            .count();

                    return CitySummary.builder()
                            .cityName(city)
                            .totalDistricts(districts.size())
                            .lockedDistricts((int) (districts.size() - unlockedCount))
                            .unlockedDistricts((int) unlockedCount)
                            .districts(districts)
                            .build();
                })
                .collect(Collectors.toList());

        // 5. 응답 JSON 구성
        Map<String, Object> data = Map.of(
                "totalKeys", unlockedRegions.size(),
                "cities", cities
        );

        return UnlockedOverviewResponse.builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * GeoJSON FeatureCollection (기존 유지)
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
        if (request.getRegionName() != null && !request.getRegionName().isBlank()) {
            String normalized = request.getRegionName().trim();

            Optional<Region> byName = regionRepository.findByGuSi(normalized);
            if (byName.isEmpty() && normalized.contains(" ")) {
                String lastSegment = normalized.substring(normalized.lastIndexOf(' ') + 1);
                byName = regionRepository.findByGuSi(lastSegment);
            }
            if (byName.isPresent()) {
                return byName.get();
            }
            if (normalized.matches("\\d+")) {
                return regionRepository.findBySigCd(normalized)
                        .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
            }
            throw new ApiException(ErrorCode.REGION_NOT_FOUND);
        }
        throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
}
