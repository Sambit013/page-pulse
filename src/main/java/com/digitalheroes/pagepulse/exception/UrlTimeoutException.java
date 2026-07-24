package com.digitalheroes.pagepulse.exception;

import org.springframework.http.HttpStatus;

public class UrlTimeoutException extends AuditException {
    public UrlTimeoutException(String url) {
        super("TIMEOUT",
                "Timed out waiting for a response from '" + url + "'.",
                HttpStatus.GATEWAY_TIMEOUT);
    }
}
