package com.digitalheroes.pagepulse.exception;

import org.springframework.http.HttpStatus;

public class InvalidUrlException extends AuditException {
    public InvalidUrlException(String url) {
        super("INVALID_URL",
                "'" + url + "' is not a valid, well-formed http(s) URL.",
                HttpStatus.BAD_REQUEST);
    }
}
