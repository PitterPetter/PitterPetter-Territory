package com.pitterpetter.loventure.territory.exception;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handle(ApiException exception) {
        ErrorCode code = exception.getCode();
        return ResponseEntity.status(code.getStatus())
            .body(Map.of("error", code.name(), "message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", ErrorCode.INVALID_REQUEST.name(), "message", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAny(Exception exception) {
        return ResponseEntity.internalServerError()
            .body(Map.of("error", ErrorCode.INTERNAL_ERROR.name(), "message", exception.getMessage()));
    }
}
