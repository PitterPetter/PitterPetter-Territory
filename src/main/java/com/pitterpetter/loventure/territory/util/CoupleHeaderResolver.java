package com.pitterpetter.loventure.territory.util;

import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;

public final class CoupleHeaderResolver {

    public static final String COUPLE_ID_HEADER = "X-COUPLE-ID";

    private CoupleHeaderResolver() {
    }

    public static Long resolveCoupleId(HttpServletRequest request) {
        String rawValue = request.getHeader(COUPLE_ID_HEADER);
        if (rawValue == null || rawValue.isBlank()) {
            throw new ApiException(ErrorCode.COUPLE_HEADER_MISSING);
        }
        return ValidationUtils.parseLong(rawValue)
            .map(value -> ValidationUtils.requirePositive(value, ErrorCode.COUPLE_HEADER_INVALID))
            .orElseThrow(() -> new ApiException(ErrorCode.COUPLE_HEADER_INVALID));
    }
}
