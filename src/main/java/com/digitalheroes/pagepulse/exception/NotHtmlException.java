package com.digitalheroes.pagepulse.exception;

import org.springframework.http.HttpStatus;

public class NotHtmlException extends AuditException {
    public NotHtmlException(String url, String actualContentType) {
        super("NOT_HTML",
                "'" + url + "' did not return HTML (content-type: " + actualContentType + ").",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
