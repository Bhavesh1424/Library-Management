package com.campus.library.controller;

import com.campus.library.domain.AuditLog;
import com.campus.library.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Administration & Audit Trail", description = "Endpoints for chief administrator oversight and immutable chain-of-custody logs (NFR-4)")
public class AdminAuditController {

    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Get recent audit trail", description = "Returns latest 100 immutable audit trail entries across the entire campus")
    public ResponseEntity<List<AuditLog>> getRecentLogs() {
        return ResponseEntity.ok(auditService.getRecentAuditLogs());
    }

    @GetMapping("/entity/{entityName}/{entityId}")
    @Operation(summary = "Get entity change history", description = "Returns full lifecycle change history for a given entity (e.g. BranchCopy, TransferRequest)")
    public ResponseEntity<List<AuditLog>> getEntityHistory(
            @PathVariable String entityName,
            @PathVariable Long entityId
    ) {
        return ResponseEntity.ok(auditService.getEntityHistory(entityName, entityId));
    }
}
