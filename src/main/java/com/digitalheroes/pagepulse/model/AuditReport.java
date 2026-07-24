package com.digitalheroes.pagepulse.model;

import java.time.Instant;

/**
 * The JSON report returned for a successfully audited URL.
 * A record is used because this object is a pure, immutable data
 * carrier with no behaviour of its own.
 */
public record AuditReport(
        String url,
        int httpStatus,
        long responseTimeMs,
        String title,
        String metaDescription,
        int h1Count,
        int totalImages,
        int imagesMissingAlt,
        int wordCount,
        Instant fetchedAt
) {
}
