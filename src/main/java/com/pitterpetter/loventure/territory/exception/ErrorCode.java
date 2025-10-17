package com.pitterpetter.loventure.territory.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    AUTH_HEADER_MISSING(HttpStatus.UNAUTHORIZED, "Authorization 헤더가 없습니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "JWT 토큰이 유효하지 않습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT 토큰이 만료되었습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 지역을 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
