# Page Pulse

A small web tool that audits any URL: it fetches the page and returns HTTP
status, response time, title, meta description, H1 count, image alt-text
gaps, and an approximate word count.

Built for the Digital Heroes SDE take-home task (Task A + Task B).

🔗 **Live demo:** [https://page-pulse.onrender.com](https://page-pulse.onrender.com) *(replace with your actual Render URL once deployed)*

> Note: this is hosted on Render's free tier, which spins down after ~15
> minutes of inactivity. The first request after idle time may take
> 30–60 seconds to wake the service up — that's expected, not a bug.

---

---

## Setup

**Requirements:** Java 17+, Maven 3.8+

```bash
# Clone and enter the project
git clone <your-repo-url>
cd page-pulse

# Run tests
mvn test

# Run the app locally
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Open that URL in a browser to
use the frontend, or call the API directly:

```bash
curl "http://localhost:8080/api/audit?url=https://example.com"
```

### Deploying (Render, free tier)

Render no longer offers a native Java runtime, so this deploys via the included
`Dockerfile` instead (a multi-stage build: compiles with Maven, then runs on a
lean JRE image).

1. Push this repo to GitHub (Dockerfile included at the project root).
2. On [render.com](https://render.com), create a **New Web Service**, connect the repo.
3. When asked for the **Runtime**, select **Docker** — Render will auto-detect the `Dockerfile` and use it. You do not need to set a separate build/start command; they're defined in the Dockerfile.
4. Render injects `PORT` automatically — `application.properties` already reads it.

---

## API Contract

### `GET /api/audit?url={url}`

Fetches `url`, parses the response, and returns an audit report.

**Success — `200 OK`**

```json
{
  "url": "https://example.com",
  "httpStatus": 200,
  "responseTimeMs": 184,
  "title": "Example Domain",
  "metaDescription": "",
  "h1Count": 1,
  "totalImages": 0,
  "imagesMissingAlt": 0,
  "wordCount": 28,
  "fetchedAt": "2026-07-24T10:15:30.123Z"
}
```

**Failure — `4xx` / `5xx`**

Every failure mode returns the same shape, only the HTTP status and
`errorCode` change:

```json
{
  "errorCode": "TIMEOUT",
  "message": "Timed out waiting for a response from 'https://example.com'."
}
```

| Scenario | HTTP status | `errorCode` |
|---|---|---|
| Missing/blank/malformed URL | 400 | `INVALID_URL` |
| Missing `url` query param | 400 | `MISSING_PARAMETER` |
| Response isn't HTML (e.g. a PDF, image) | 415 | `NOT_HTML` |
| Host unreachable / DNS failure | 502 | `UNREACHABLE` |
| Request timed out (8s limit) | 504 | `TIMEOUT` |
| Anything unanticipated | 500 | `INTERNAL_ERROR` |

The API never returns a raw stack trace or Spring's default HTML error
page — everything is funnelled through `GlobalExceptionHandler` into this
one contract.

---

## Design decisions

**1. Jsoup instead of raw `HttpURLConnection` + regex parsing.**
Real-world HTML is frequently malformed — unclosed tags, missing quotes,
inconsistent casing. Jsoup parses it the way a browser would (lenient,
DOM-based) rather than relying on regex against raw text, which breaks
easily and silently on edge cases. It also gives timeout handling,
redirect following, and a clean `Document`/CSS-selector API in one
dependency, which kept the codebase small.

**2. Fetching (network I/O) and parsing (pure logic) are two separate
methods in `AuditService`, not one blended method.**
`fetch()` hits the network and can fail in network-specific ways
(timeout, DNS failure). `buildReport()` takes an already-parsed `Document`
and only ever does deterministic field extraction — no I/O, no exceptions
expected. That split is what makes the Task B unit tests possible without
network mocking or a test server: they call `buildReport()` directly with
a `Document` built from a fixed HTML string. If a bug shows up in "word
count is wrong," it's obviously in parsing, not fetching — the separation
also pays off there.

**3. A custom exception hierarchy (`AuditException` + four subtypes) with
a single `@RestControllerAdvice`, instead of try/catch inside the
controller.**
Each failure mode (`InvalidUrlException`, `UrlTimeoutException`,
`UrlUnreachableException`, `NotHtmlException`) carries its own HTTP status
and error code, decided at the point the failure is detected — deep
inside `AuditService`, where the most context is available. The
controller and `GlobalExceptionHandler` don't need to know *why*
something failed, just how to shape the response. This also guarantees
the "never crash" requirement: a catch-all `Exception` handler is the
last line of defense so nothing can escape as an unhandled 500 with a
stack trace.

---

## A note on the alt-text rule

`imagesMissingAlt` only counts images with **no `alt` attribute at all** —
not images with `alt=""`. An empty alt is a valid, intentional signal per
the HTML spec that an image is decorative and should be skipped by screen
readers; it isn't the same failure as a missing attribute, where a screen
reader has no idea whether the image carries meaning. Stricter audit
tools sometimes flag `alt=""` too, on the theory that it's often used to
dodge a linter rather than intentionally — I chose the spec-accurate
reading instead, since a false positive here would flag legitimate,
accessible markup as broken.

## What I'd change with another day

See the Loom walkthrough for the specific piece of code and reasoning —
in short, I'd move URL fetching onto a virtual thread / async executor
with a hard request-level timeout wrapper, since right now a slow but
technically-still-connecting host can hold a request thread for close to
the full 8s Jsoup timeout, which won't scale well under concurrent load.

---

**Built for Digital Heroes Training Task**, linked to
[digitalheroesco.com](https://digitalheroesco.com).