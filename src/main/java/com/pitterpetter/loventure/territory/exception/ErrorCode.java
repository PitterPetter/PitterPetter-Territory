package com.pitterpetter.loventure.territory.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    COUPLE_HEADER_MISSING(HttpStatus.BAD_REQUEST, "커플 식별 헤더가 필요합니다."),
    COUPLE_HEADER_INVALID(HttpStatus.BAD_REQUEST, "커플 식별 헤더 값이 올바르지 않습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 지역을 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
