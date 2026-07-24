package com.digitalheroes.pagepulse.service;

import com.digitalheroes.pagepulse.exception.InvalidUrlException;
import com.digitalheroes.pagepulse.model.AuditReport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests target the pure parsing logic (buildReport) directly with a
 * Jsoup Document built from a fixed HTML string, so no real network
 * call is made. validateAndNormalize is tested separately since it's
 * the other pure, easily-isolated piece of the pipeline. The actual
 * network fetch() method is intentionally NOT unit tested here - it's
 * a thin wrapper around Jsoup.connect() and is better covered by a
 * manual/integration check against a real or local server.
 */
class AuditServiceTest {

    private final AuditService service = new AuditService();

    @Nested
    @DisplayName("Happy path: parsing a well-formed HTML page")
    class HappyPath {

        @Test
        @DisplayName("extracts title, meta description, h1 count, image alt gaps, and word count")
        void parsesAllFieldsCorrectly() {
            String html = """
                <html>
                  <head>
                    <title>Digital Heroes | Training Task</title>
                    <meta name="description" content="A practical SDE take-home task.">
                  </head>
                  <body>
                    <h1>Welcome</h1>
                    <h1>Second heading</h1>
                    <p>This is a short paragraph with exactly eight words here.</p>
                    <img src="a.png" alt="A description">
                    <img src="b.png">
                    <img src="c.png" alt="">
                  </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);

            AuditReport report = service.buildReport("https://example.com", 200, 123, doc);

            assertThat(report.title()).isEqualTo("Digital Heroes | Training Task");
            assertThat(report.metaDescription()).isEqualTo("A practical SDE take-home task.");
            assertThat(report.h1Count()).isEqualTo(2);
            assertThat(report.totalImages()).isEqualTo(3);
            // b.png has no alt attribute at all -> missing.
            // c.png has alt="" -> treated as an intentional "decorative image"
            // signal per the HTML spec, so it does NOT count as missing.
            assertThat(report.imagesMissingAlt()).isEqualTo(1);
            assertThat(report.wordCount()).isEqualTo(10); // "This is a short paragraph with exactly eight words here."
            assertThat(report.httpStatus()).isEqualTo(200);
            assertThat(report.responseTimeMs()).isEqualTo(123);
            assertThat(report.url()).isEqualTo("https://example.com");
        }

        @Test
        @DisplayName("treats alt=\"\" as a valid decorative-image signal, not a missing-alt violation")
        void emptyAltIsNotCountedAsMissing() {
            String html = """
                <html><head><title>t</title></head>
                <body>
                  <img src="decorative.png" alt="">
                  <img src="no-attribute.png">
                </body></html>
                """;
            Document doc = Jsoup.parse(html);

            AuditReport report = service.buildReport("https://example.com", 200, 5, doc);

            assertThat(report.totalImages()).isEqualTo(2);
            assertThat(report.imagesMissingAlt()).isEqualTo(1); // only no-attribute.png
        }

        @Test
        @DisplayName("excludes script and style content from the word count")
        void ignoresScriptAndStyleText() {
            String html = """
                <html>
                <head><title>t</title></head>
                <body>
                  <script>var thisShouldNotBeCounted = "lots of fake words here";</script>
                  <style>.thisAlsoShouldNotCount { color: red; }</style>
                  <p>Only these four words count.</p>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);

            AuditReport report = service.buildReport("https://example.com", 200, 10, doc);

            assertThat(report.wordCount()).isEqualTo(5); // "Only these four words count."
        }
    }

    @Nested
    @DisplayName("Failure case 1: invalid URLs are rejected before any network call")
    class InvalidUrls {

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "not a url at all", "ht!tp://bad", "https://"})
        @DisplayName("throws InvalidUrlException for malformed input")
        void rejectsMalformedUrls(String badUrl) {
            assertThatThrownBy(() -> service.validateAndNormalize(badUrl))
                    .isInstanceOf(InvalidUrlException.class);
        }

        @Test
        @DisplayName("throws InvalidUrlException for null input")
        void rejectsNullUrl() {
            assertThatThrownBy(() -> service.validateAndNormalize(null))
                    .isInstanceOf(InvalidUrlException.class);
        }

        @Test
        @DisplayName("adds https:// scheme automatically when missing but the rest is valid")
        void addsMissingScheme() {
            String normalized = service.validateAndNormalize("example.com");
            assertThat(normalized).isEqualTo("https://example.com");
        }
    }

    @Nested
    @DisplayName("Failure case 2: pages with no content still produce a valid (empty) report, never a crash")
    class EmptyOrMinimalContent {

        @Test
        @DisplayName("returns zeroed-out fields for a bare, near-empty HTML document instead of throwing")
        void handlesEmptyBody() {
            Document doc = Jsoup.parse("<html><head></head><body></body></html>");

            AuditReport report = service.buildReport("https://example.com", 200, 5, doc);

            assertThat(report.h1Count()).isZero();
            assertThat(report.totalImages()).isZero();
            assertThat(report.imagesMissingAlt()).isZero();
            assertThat(report.wordCount()).isZero();
            assertThat(report.metaDescription()).isEmpty();
        }
    }
}