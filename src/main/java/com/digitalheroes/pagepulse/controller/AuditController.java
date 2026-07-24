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
     * Handles GET /api/audit?url=https://example.com
     *
     * Returns an AuditReport if the request succeeds.
     * If an error occurs, GlobalExceptionHandler returns
     * the appropriate HTTP status and error response.
     */
    @GetMapping("/api/audit")
    public AuditReport audit(@RequestParam String url) {
        return auditService.audit(url);
    }
}
