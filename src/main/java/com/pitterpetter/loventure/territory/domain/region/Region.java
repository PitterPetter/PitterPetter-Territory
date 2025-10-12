package com.pitterpetter.loventure.territory.domain.region;

import jakarta.persistence.*;
import org.locationtech.jts.geom.MultiPolygon;
import lombok.*;

@Entity
@Table(name = "region")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sig_cd", length = 10, unique = true)
    private String sigCd; // 행정코드

    @Column(name = "gu_si")
    private String gu_si; // 지역명 (ex: 강남구)

    @Column(name = "si_do")
    private String si_do; // 상위 지역명 (ex: 서울특별시)

    @Column(name = "geom", columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom; // PostGIS Geometry
}
