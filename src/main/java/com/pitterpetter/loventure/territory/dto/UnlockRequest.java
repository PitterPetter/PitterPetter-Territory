package com.pitterpetter.loventure.territory.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnlockRequest {

    private String sigCd;       // 선택적 필드 (지금은 안 써도 됨)
    private String regionId;

    @JsonAlias({"regions", "region", "regionName"})
    private List<String> regionNames;  // 이제 배열을 받을 수 있음
}
