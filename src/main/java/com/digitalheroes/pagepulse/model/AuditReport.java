package com.digitalheroes.pagepulse.model;

import java.time.Instant;

/**
 * Stores the audit results for a successfully checked URL.
 * This record only holds the report data.
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
