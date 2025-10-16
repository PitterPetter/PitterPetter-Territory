package com.pitterpetter.loventure.territory.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitterpetter.loventure.territory.exception.ApiException;
import com.pitterpetter.loventure.territory.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CoupleHeaderResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final List<String> COUPLE_ID_CLAIM_KEYS = List.of(
        "coupleId",
        "couple_id",
        "coupleID",
        "couple",
        "couple-id"
    );

    private CoupleHeaderResolver() {
    }

    public static Long resolveCoupleId(HttpServletRequest request) {
        String token = extractBearerToken(request.getHeader(AUTHORIZATION_HEADER));
        Map<String, Object> payload = decodePayload(token);
        Long coupleId = extractCoupleId(payload)
            .orElseThrow(() -> new ApiException(ErrorCode.AUTH_TOKEN_INVALID));
        return ValidationUtils.requirePositive(coupleId, ErrorCode.AUTH_TOKEN_INVALID);
    }

    private static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ApiException(ErrorCode.AUTH_HEADER_MISSING);
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        return token;
    }

    private static Map<String, Object> decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(applyPadding(parts[1]));
            return OBJECT_MAPPER.readValue(payloadBytes, MAP_TYPE);
        } catch (IllegalArgumentException | IOException exception) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    private static Optional<Long> extractCoupleId(Map<String, Object> payload) {
        for (String key : COUPLE_ID_CLAIM_KEYS) {
            Optional<Long> value = toLong(payload.get(key));
            if (value.isPresent()) {
                return value;
            }
        }
        return toLong(payload.get("sub"));
    }

    private static Optional<Long> toLong(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        if (value instanceof String stringValue) {
            return ValidationUtils.parseLong(stringValue);
        }
        if (value instanceof List<?> list && !list.isEmpty()) {
            return toLong(list.get(0));
        }
        return Optional.empty();
    }

    private static String applyPadding(String value) {
        int remainder = value.length() % 4;
        if (remainder == 0) {
            return value;
        }
        if (remainder == 1) {
            throw new IllegalArgumentException("Invalid base64 length");
        }
        return value + "====".substring(remainder);
    }
}
