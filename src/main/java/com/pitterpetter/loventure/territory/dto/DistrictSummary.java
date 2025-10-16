package com.pitterpetter.loventure.territory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistrictSummary {

    private String id;          // region.sigCd (또는 구 코드)
    private String name;        // region.guSi
    private boolean isLocked;   // 해금 여부 (true = 잠김, false = 해제됨)
    private String description; // 선택적 필드 (없으면 null 가능)
    private Double lat;         // 중심 위도 (nullable)
    private Double lng;         // 중심 경도 (nullable)
}
