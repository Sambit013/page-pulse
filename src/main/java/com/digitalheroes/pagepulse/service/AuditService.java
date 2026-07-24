package com.digitalheroes.pagepulse.service;

import com.digitalheroes.pagepulse.exception.InvalidUrlException;
import com.digitalheroes.pagepulse.exception.NotHtmlException;
import com.digitalheroes.pagepulse.exception.UrlTimeoutException;
import com.digitalheroes.pagepulse.exception.UrlUnreachableException;
import com.digitalheroes.pagepulse.model.AuditReport;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.regex.Pattern;

@Service
public class AuditService {

    private static final int TIMEOUT_MS = 8_000;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Checks the given URL and returns the audit report.
     */

    public AuditReport audit(String rawUrl) {
        String normalizedUrl = validateAndNormalize(rawUrl);

        long start = System.currentTimeMillis();
        Connection.Response response = fetch(normalizedUrl);
        long elapsed = System.currentTimeMillis() - start;

        String contentType = response.contentType() == null ? "" : response.contentType();
        if (!contentType.toLowerCase().contains("text/html")
                && !contentType.toLowerCase().contains("application/xhtml+xml")) {
            throw new NotHtmlException(normalizedUrl, contentType.isBlank() ? "unknown" : contentType);
        }

        Document document = parseBody(response);
        return buildReport(normalizedUrl, response.statusCode(), elapsed, document);
    }

    // Step 1: validation -------------------------------------------------

    /**
     * Design decision: We validate the URL before making the network request.
     * If the URL is invalid, Jsoup throws a generic IllegalArgumentException,
     * which does not clearly tell what went wrong.
     * By checking the URL first, we can return a clear
     * 400 INVALID_URL response.
     */
    String validateAndNormalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException(String.valueOf(rawUrl));
        }

        String candidate = rawUrl.trim();
        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
            candidate = "https://" + candidate;
        }

        try {
            URL url = URI.create(candidate).toURL();
            if (url.getHost() == null || url.getHost().isBlank()) {
                throw new InvalidUrlException(rawUrl);
            }
            return url.toString();
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new InvalidUrlException(rawUrl);
        }
    }

    // Step 2: fetch (network I/O - the part we mock in tests) -----------

    Connection.Response fetch(String url) {
        try {
            return Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; PagePulse/1.0; +https://digitalheroesco.com)")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .execute();
        } catch (SocketTimeoutException e) {
            throw new UrlTimeoutException(url);
        } catch (UnknownHostException e) {
            throw new UrlUnreachableException(url, "unknown host");
        } catch (java.io.IOException e) {
            throw new UrlUnreachableException(url, e.getMessage() == null ? "connection failed" : e.getMessage());
        }
    }

    // Step 3: parse (pure logic - fully unit-testable) -------------------

    Document parseBody(Connection.Response response) {
        try {
            return response.parse();
        } catch (java.io.IOException e) {
            throw new UrlUnreachableException(response.url().toString(), "could not read response body");
        }
    }

    /**
     * Design decision: This method only reads the HTML and creates the report.
     * It does not make any network request or handle exceptions.
     * The unit tests check this method by passing a Jsoup Document
     * created from a fixed HTML string.
     */
    AuditReport buildReport(String url, int statusCode, long responseTimeMs, Document document) {
        String title = document.title();

        String metaDescription = document.select("meta[name=description]")
                .attr("content");

        int h1Count = document.select("h1").size();

        Elements images = document.select("img");
        int totalImages = images.size();
        // Only flag images with NO alt attribute at all. An explicit
        // alt="" is a valid, intentional signal per the HTML spec that
        // an image is decorative and should be skipped by screen readers -
        // it is not the same failure as a missing attribute, where the
        // browser has no idea whether the image is meaningful or not.
        int missingAlt = (int) images.stream()
                .filter(img -> !img.hasAttr("alt"))
                .count();

        int wordCount = countWords(document);

        return new AuditReport(
                url,
                statusCode,
                responseTimeMs,
                title,
                metaDescription,
                h1Count,
                totalImages,
                missingAlt,
                wordCount,
                Instant.now()
        );
    }

    private int countWords(Document document) {
        // Clone so we don't mutate the document the caller might still use,
        // then strip script/style content - it isn't visible page text and
        // would otherwise inflate the word count.
        Document clone = document.clone();
        clone.select("script, style, noscript").remove();
        String text = clone.body() != null ? clone.body().text() : "";
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return WHITESPACE.split(trimmed).length;
    }
}