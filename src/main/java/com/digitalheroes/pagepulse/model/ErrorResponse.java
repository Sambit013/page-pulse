package com.digitalheroes.pagepulse.model;

/**
 * Every error the API returns - bad input, timeout, unreachable host,
 * non-HTML response - is shaped like this so the frontend (and anyone
 * else calling the API) only ever has to handle one error contract.
 */
public record ErrorResponse(
        String errorCode,
        String message
) {
}
