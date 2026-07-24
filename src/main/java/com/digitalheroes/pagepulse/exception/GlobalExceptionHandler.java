package com.digitalheroes.pagepulse.exception;

import com.digitalheroes.pagepulse.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised error handling. Without this, an unhandled exception in
 * Spring Boot returns its default HTML "Whitelabel Error Page" (or a
 * raw stack trace in dev mode) - not something a frontend can parse,
 * and not something we'd want to expose. Every failure path, expected
 * or not, is funnelled through here into one consistent JSON shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuditException.class)
    public ResponseEntity<ErrorResponse> handleAuditException(AuditException ex) {
        log.warn("Audit failed [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("MISSING_PARAMETER",
                        "Required parameter '" + ex.getParameterName() + "' is missing."));
    }

    // Catch-all safety net: guarantees the API can never crash with a raw
    // stack trace, even for a failure mode we didn't explicitly anticipate.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error during audit", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR",
                        "Something went wrong while processing this request."));
    }
}
