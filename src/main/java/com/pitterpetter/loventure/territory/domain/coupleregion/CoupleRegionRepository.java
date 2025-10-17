package com.pitterpetter.loventure.territory.domain.coupleregion;

import com.pitterpetter.loventure.territory.domain.region.Region;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoupleRegionRepository extends JpaRepository<CoupleRegion, Long> {

    Optional<CoupleRegion> findByCoupleIdAndRegion(Long coupleId, Region region);

    Optional<CoupleRegion> findByCoupleIdAndRegion_Id(Long coupleId, String regionId);

    List<CoupleRegion> findByCoupleId(Long coupleId);

    List<CoupleRegion> findByCoupleIdAndIsLockedFalse(Long coupleId);
}
