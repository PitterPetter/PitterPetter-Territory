package com.pitterpetter.loventure.territory.domain.region;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegionRepository extends JpaRepository<Region, String> {

    /**
     * ✅ 위도/경도 좌표로 포함된 Region 찾기 (PostGIS)
     */
    @Query("""
        SELECT r FROM Region r
        WHERE ST_Contains(r.geom, ST_GeomFromText(:point, 4326)) = true
    """)
    Optional<Region> findRegionByPoint(@Param("point") String point);

    /**
     * ✅ 행정코드(sigCd)로 Region 조회
     */
    Optional<Region> findBySigCd(String sigCd);

    /**
     * ✅ 구/시 이름(예: 강남구, 마포구 등)으로 Region 조회
     */
    @Query("SELECT r FROM Region r WHERE r.gu_si = :name")
    Optional<Region> findByGuSi(@Param("name") String name);
}
