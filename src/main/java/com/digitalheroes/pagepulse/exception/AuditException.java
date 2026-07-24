package com.digitalheroes.pagepulse.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for every expected failure mode in the audit pipeline.
 * Keeping these as checked failure types (not generic RuntimeExceptions)
 * means the GlobalExceptionHandler can map each one to a precise HTTP
 * status and error code instead of guessing from a message string.
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
