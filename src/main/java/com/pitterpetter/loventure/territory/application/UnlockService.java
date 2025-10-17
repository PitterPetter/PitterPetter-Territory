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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Point;

@Service
@RequiredArgsConstructor
public class UnlockService {

    private final CoupleRegionRepository coupleRegionRepository;
    private final RegionRepository regionRepository;

    // ✅ 다중 해금 처리 (regions 배열 입력용)
    @Transactional
    @CacheEvict(value = "unlockedRegions", key = "#coupleId")
    public List<UnlockResponse> unlockMultipleRegions(Long coupleId, List<String> regionNames) {
        if (regionNames == null || regionNames.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "regionNames 리스트가 비어 있습니다.");
        }

        Long verifiedCoupleId = ValidationUtils.requirePositive(coupleId, ErrorCode.INVALID_REQUEST);
        List<UnlockResponse> unlockedList = new ArrayList<>();

        for (String name : regionNames) {
            regionRepository.findByGuSi(name.trim()).ifPresentOrElse(region -> {
                CoupleRegion cr = coupleRegionRepository
                        .findByCoupleIdAndRegion(verifiedCoupleId, region)
                        .map(this::updateUnlock)
                        .orElseGet(() -> createUnlock(verifiedCoupleId, region));

                CoupleRegion saved = coupleRegionRepository.save(cr);
                unlockedList.add(UnlockResponse.from(saved));

            }, () -> {
                throw new ApiException(ErrorCode.REGION_NOT_FOUND, "존재하지 않는 지역명: " + name);
            });
        }
        return unlockedList;
    }

    // ✅ 기존 단일 해금 (호환 유지)
    @Transactional
    @CacheEvict(value = "unlockedRegions", key = "#coupleId")
    public UnlockResponse unlockRegion(Long coupleId, String sigCd, String regionId, String regionName) {
        Long verifiedCoupleId = ValidationUtils.requirePositive(coupleId, ErrorCode.INVALID_REQUEST);
        Region region = resolveRegionByDirectValues(regionId, sigCd, regionName);

        CoupleRegion coupleRegion = coupleRegionRepository
                .findByCoupleIdAndRegion(verifiedCoupleId, region)
                .map(this::updateUnlock)
                .orElseGet(() -> createUnlock(verifiedCoupleId, region));

        CoupleRegion saved = coupleRegionRepository.save(coupleRegion);
        return UnlockResponse.from(saved);
    }

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
            return regionRepository.findByGuSi(regionName.trim())
                    .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        }
        throw new ApiException(ErrorCode.INVALID_REQUEST);
    }

    // ✅ 조회 관련
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
                            .map(region -> toDistrictSummary(region, unlockedIds))
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

    private DistrictSummary toDistrictSummary(Region region, Set<String> unlockedIds) {
        boolean isLocked = !unlockedIds.contains(region.getId());
        Double lat = null;
        Double lng = null;

        if (region.getGeom() != null && !region.getGeom().isEmpty()) {
            Point centroid = region.getGeom().getCentroid();
            if (centroid != null && !centroid.isEmpty()) {
                lng = centroid.getX();
                lat = centroid.getY();
            }
        }

        return DistrictSummary.builder()
                .id(region.getId())
                .name(region.getGu_si())
                .isLocked(isLocked)
                .description(buildRegionDescription(region))
                .lat(lat)
                .lng(lng)
                .build();
    }

    private String buildRegionDescription(Region region) {
        String siDo = safeTrim(region.getSi_do());
        String guSi = safeTrim(region.getGu_si());

        if (!siDo.isEmpty() && !guSi.isEmpty()) {
            return (siDo + " " + guSi).trim();
        }
        if (!guSi.isEmpty()) {
            return guSi;
        }
        if (!siDo.isEmpty()) {
            return siDo;
        }
        return null;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
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
