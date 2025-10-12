package com.pitterpetter.loventure.territory.domain.coupleregion;

import com.pitterpetter.loventure.territory.domain.region.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoupleRegionRepository extends JpaRepository<CoupleRegion, Long> {

    Optional<CoupleRegion> findByCoupleIdAndRegion(Long coupleId, Region region);

    List<CoupleRegion> findByCoupleIdAndIsLockedFalse(Long coupleId);
}
