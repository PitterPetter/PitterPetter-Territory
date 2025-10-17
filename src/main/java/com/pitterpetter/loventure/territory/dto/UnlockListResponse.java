package com.pitterpetter.loventure.territory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

/**
 * 여러 지역 해금 결과를 담는 공통 응답 DTO.
 * [
 *   {
 *     "sigCd": "41500",
 *     "coupleId": 2,
 *     "unlocked": true,
 *     "region": {
 *       "nameKo": "마포구",
 *       "parent": "서울시"
 *     }
 *   },
 *   {
 *     "sigCd": "41800",
 *     "coupleId": 2,
 *     "unlocked": true,
 *     "region": {
 *       "nameKo": "강남구",
 *       "parent": "서울시"
 *     }
 *   }
 * ]
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnlockListResponse {

    private boolean success;            // 요청 성공 여부
    private int count;                  // 해금된 지역 수
    private List<UnlockResponse> data;  // 해금된 지역 리스트
}
