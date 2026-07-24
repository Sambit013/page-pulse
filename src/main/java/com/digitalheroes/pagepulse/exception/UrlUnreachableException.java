package com.digitalheroes.pagepulse.exception;

import org.springframework.http.HttpStatus;

public class UrlUnreachableException extends AuditException {
    public UrlUnreachableException(String url, String reason) {
        super("UNREACHABLE",
                "Could not reach '" + url + "': " + reason,
                HttpStatus.BAD_GATEWAY);
    }
}
