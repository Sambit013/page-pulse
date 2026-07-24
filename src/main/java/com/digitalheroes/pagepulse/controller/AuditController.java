package com.digitalheroes.pagepulse.controller;

import com.digitalheroes.pagepulse.model.AuditReport;
import com.digitalheroes.pagepulse.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * GET /api/audit?url=https://example.com
     *
     * Returns an AuditReport as JSON on success, or an ErrorResponse
     * (via GlobalExceptionHandler) with an appropriate HTTP status on
     * failure. See README "API Contract" for the full schema.
     */
    @GetMapping("/api/audit")
    public AuditReport audit(@RequestParam String url) {
        return auditService.audit(url);
    }
}
