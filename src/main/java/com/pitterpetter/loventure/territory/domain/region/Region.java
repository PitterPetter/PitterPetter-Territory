package com.pitterpetter.loventure.territory.domain.region;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.MultiPolygon;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    private String sigCd; // 행정 코드

    @Column(name = "gu_si")
    @JsonProperty("name_ko") // ✅ JSON의 name_ko → DB 컬럼 gu_si로 매핑
    private String gu_si;    // 구/시 이름

    @Column(name = "si_do")
    @JsonProperty("parent") // ✅ JSON의 parent → DB 컬럼 si_do로 매핑
    private String si_do;    // 상위 시/도

    @Column(name = "geom", columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom; // ✅ PostGIS Geometry
}
