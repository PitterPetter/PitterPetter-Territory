package com.pitterpetter.loventure.territory.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitySummary {

    private String cityName;          // 시/도 이름 (예: 서울시, 경기도)
    private int totalDistricts;       // 전체 구/군 개수
    private int lockedDistricts;      // 잠긴 구/군 개수
    private int unlockedDistricts;    // 해제된 구/군 개수
    private List<DistrictSummary> districts; // 하위 구/군 리스트
}
