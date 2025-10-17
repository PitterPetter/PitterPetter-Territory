package com.pitterpetter.loventure.territory.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnlockRequest {
    private String sigCd;       // 또는 regionId
    private String regionId;
    @JsonAlias({"regions", "region"})
    private String regionName;  // 프론트에서 전달하는 지역 이름
}
