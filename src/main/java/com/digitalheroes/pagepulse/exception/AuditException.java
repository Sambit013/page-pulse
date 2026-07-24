package com.digitalheroes.pagepulse.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all expected audit errors.
 * Each error has its own HTTP status and error code,
 * so the GlobalExceptionHandler can return the correct API response.
 */
public class AuditException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public AuditException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
