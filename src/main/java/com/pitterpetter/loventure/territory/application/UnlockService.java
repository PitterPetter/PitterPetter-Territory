package com.pitterpetter.loventure.territory.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegion;
import com.pitterpetter.loventure.territory.domain.coupleregion.CoupleRegionRepository;
import com.pitterpetter.loventure.territory.domain.region.Region;
import com.pitterpetter.loventure.territory.domain.region.RegionRepository;
import com.pitterpetter.loventure.territory.dto.CitySummary;
import com.pitterpetter.loventure.territory.dto.DistrictSummary;
import com.pitterpetter.loventure.territory.dto.UnlockResponse;
import com.pitterpetter.loventure.territory.dto.UnlockedOverviewResponse;
import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import com.pitterpetter.loventure.territory.infra.AuthClient;
import com.pitterpetter.loventure.territory.service.RedisTicketService;
import com.pitterpetter.loventure.territory.util.GeoJsonUtils;
import com.pitterpetter.loventure.territory.util.ValidationUtils;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnlockService {

    private final CoupleRegionRepository coupleRegionRepository;
    private final RegionRepository regionRepository;
    private final AuthClient authClient;
    private final RedisTicketService redisTicketService;

    // ========================================================================
    // ✅ [1] Auth 검증 기반 초기 해금
    // 프론트 → Territory → Auth → OK → 티켓 차감 → 해금
    // ========================================================================
    public List<UnlockResponse> initUnlock(String coupleId, List<String> regions, HttpServletRequest request) {
        log.info("🔐 [Init Unlock] Auth 검증 시작...");

        if (!verifyAuthToken(coupleId, request)) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID, "Auth 토큰 검증 실패");
        }

        log.info("✅ Auth 검증 통과. 티켓 차감 및 해금 진행...");
        
        // 티켓 차감 요청
        if (!consumeTicketFromAuthService(coupleId, request)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "티켓 잔여 수량 부족");
        }
        
        return unlockMultipleRegions(coupleId, regions);
    }

    // ========================================================================
    // ✅ [2] Redis 검증 기반 티켓 해금
    // Gateway → Redis → Territory → 해금
    // ========================================================================
    public List<UnlockResponse> rewardUnlock(String coupleId, List<String> regions) {
        log.info("🎟️ [Reward Unlock] Redis 검증 시작...");

        if (!verifyRedisTicket(coupleId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "티켓 잔여 수량 부족");
        }

        log.info("✅ Redis 검증 통과. 해금 진행...");
        return unlockMultipleRegions(coupleId, regions);
    }

    // ========================================================================
    // ✅ Auth 검증 (FeignClient 기반)
    // ========================================================================
    public boolean verifyAuthToken(String coupleId, HttpServletRequest request) {
        try {
            String token = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                log.warn("⚠️ Authorization 헤더 누락 또는 잘못된 형식");
                return false;
            }

            authClient.verifyToken(token);
            log.info("✅ Auth 서버 검증 성공 (coupleId={})", coupleId);
            return true;

        } catch (FeignException e) {
            log.error("❌ Auth 검증 실패 (status={}, coupleId={}): {}", e.status(), coupleId, e.contentUTF8());
            return false;
        } catch (Exception e) {
            log.error("❌ Auth 서버 통신 오류 (coupleId={}): {}", coupleId, e.getMessage());
            return false;
        }
    }

    // ========================================================================
    // ✅ Redis 검증 (실제 구현)
    // ========================================================================
    public boolean verifyRedisTicket(String coupleId) {
        try {
            boolean hasTicket = redisTicketService.hasTicket(coupleId);
            if (hasTicket) {
                log.info("🎟️ Redis 티켓 검증 성공 - coupleId: {}", coupleId);
                return true;
            } else {
                log.warn("❌ Redis 티켓 부족 - coupleId: {}", coupleId);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Redis 티켓 검증 실패 - coupleId: {}, error: {}", coupleId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Gateway에서 전달받은 티켓 정보를 Redis에 저장
     */
    public void setTicketCountFromGateway(String coupleId, int ticketCount) {
        try {
            redisTicketService.setTicketCount(coupleId, ticketCount);
            log.info("🎟️ Gateway에서 전달받은 티켓 정보 저장 완료 - coupleId: {}, ticketCount: {}", coupleId, ticketCount);
        } catch (Exception e) {
            log.error("❌ Gateway 티켓 정보 저장 실패 - coupleId: {}, ticketCount: {}, error: {}", 
                    coupleId, ticketCount, e.getMessage());
        }
    }

    // ========================================================================
    // ✅ Auth Service에서 티켓 차감 및 Rock 완료 요청 (init unlock용)

    // ========================================================================
    public boolean consumeTicketFromAuthService(String coupleId, HttpServletRequest request) {
        try {
            String token = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                log.warn("⚠️ Authorization 헤더 누락 또는 잘못된 형식");
                return false;
            }

            // Auth Service에 티켓 차감 및 Rock 완료 요청 (init unlock용)
            authClient.consumeTicketAndCompleteRock(coupleId, token);
            log.info("✅ Auth Service에서 티켓 차감 및 Rock 완료 성공 (coupleId={})", coupleId);
            return true;

        } catch (FeignException e) {
            log.error("❌ 티켓 차감 및 Rock 완료 실패 (status={}, coupleId={}): {}", e.status(), coupleId, e.contentUTF8());
            return false;
        } catch (Exception e) {
            log.error("❌ Auth Service 티켓 차감 및 Rock 완료 통신 오류 (coupleId={}): {}", coupleId, e.getMessage());
            return false;
        }
    }

    // ========================================================================
    // ✅ 다중 해금 처리 (regions 배열 입력용)
    // ========================================================================
    @Transactional
    @CacheEvict(value = "unlockedRegions", key = "#coupleId")
    public List<UnlockResponse> unlockMultipleRegions(String coupleId, List<String> regionNames) {
        if (regionNames == null || regionNames.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "regionNames 리스트가 비어 있습니다.");
        }

        String verifiedCoupleId = ValidationUtils.requireNonBlank(coupleId, ErrorCode.INVALID_REQUEST);
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

    // ========================================================================
    // ✅ 기존 단일 해금 (호환 유지)
    // ========================================================================
    @Transactional
    @CacheEvict(value = "unlockedRegions", key = "#coupleId")
    public UnlockResponse unlockRegion(String coupleId, String sigCd, String regionId, String regionName) {
        String verifiedCoupleId = ValidationUtils.requireNonBlank(coupleId, ErrorCode.INVALID_REQUEST);
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

    // ========================================================================
    // ✅ 조회 관련
    // ========================================================================
    @Transactional(readOnly = true)
    public UnlockedOverviewResponse getUnlockedRegions(String coupleId) {
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

        Map<String, Object> data = Map.of("cities", cities);

        return UnlockedOverviewResponse.builder()
                .success(true)
                .data(data)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUnlockedRegionsAsFeature(String coupleId) {
        List<CoupleRegion> coupleRegions = getUnlockedCoupleRegions(coupleId);
        List<Region> regions = coupleRegions.stream()
                .map(CoupleRegion::getRegion)
                .collect(Collectors.toList());
        return GeoJsonUtils.toFeatureCollection(regions);
    }

    private List<CoupleRegion> getUnlockedCoupleRegions(String coupleId) {
        String verifiedCoupleId = ValidationUtils.requireNonBlank(coupleId, ErrorCode.INVALID_REQUEST);
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

        if (!siDo.isEmpty() && !guSi.isEmpty()) return (siDo + " " + guSi).trim();
        if (!guSi.isEmpty()) return guSi;
        if (!siDo.isEmpty()) return siDo;
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

    private CoupleRegion createUnlock(String coupleId, Region region) {
        return CoupleRegion.builder()
                .coupleId(coupleId)
                .region(region)
                .isLocked(false)
                .unlockedAt(LocalDateTime.now())
                .build();
    }
}
