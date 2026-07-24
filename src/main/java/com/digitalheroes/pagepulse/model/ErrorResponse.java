package com.digitalheroes.pagepulse.model;

/**
 * All API errors use this response format,
 * whether the problem is an invalid URL, timeout,
 * unreachable host, or a non-HTML response.
 * This gives the frontend one consistent error format to handle.
 */
public record ErrorResponse(
        String errorCode,
        String message
) {
}
