package com.pitterpetter.loventure.territory.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnlockRequest {
    private String sigCd;       // 또는 regionId
    private Long regionId;
    private String unlockType;  // "INIT" | "TICKET"
    private String selectedBy;  // "male" | "female"
}
