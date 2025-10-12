package com.pitterpetter.loventure.territory.domain.region;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    @Query("""
        SELECT r FROM Region r
        WHERE ST_Contains(r.geom, ST_GeomFromText(:point, 4326)) = true
    """)
    Optional<Region> findRegionByPoint(@Param("point") String point);
}
