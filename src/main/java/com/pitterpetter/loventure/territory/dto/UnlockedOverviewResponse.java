package com.pitterpetter.loventure.territory.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnlockedOverviewResponse {

    private boolean success;      // 항상 true/false
    private Map<String, Object> data;  // 실제 데이터 payload (totalKeys, cities 등)
}
