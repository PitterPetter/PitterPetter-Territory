package com.pitterpetter.loventure.territory.util;

import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import java.util.Optional;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static void validateLonLat(double lon, double lat) {
        if (Double.isNaN(lon) || Double.isNaN(lat)) {
            throw new IllegalArgumentException("NaN");
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("lon out of range");
        }
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("lat out of range");
        }
    }

    public static Optional<Long> parseLong(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static Long requirePositive(Long value, ErrorCode errorCode) {
        if (value == null || value <= 0) {
            throw new ApiException(errorCode);
        }
        return value;
    }

    public static String requireNonBlank(String value, ErrorCode errorCode) {
        if (value == null) {
            throw new ApiException(errorCode);
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(errorCode);
        }
        return trimmed;
    }
}
