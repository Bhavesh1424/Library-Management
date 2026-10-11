package com.campus.library.service;

import com.campus.library.domain.AuditLog;
import com.campus.library.domain.User;
import com.campus.library.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * NFR-4 Audit Traceability: Appends an immutable audit entry recording state changes.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordAction(String entityName, Long entityId, String action,
                             String fromState, String toState, User operator, String details) {
        Long operatorId = (operator != null) ? operator.getId() : null;
        String operatorUsername = (operator != null) ? operator.getEmail() : "SYSTEM";

        AuditLog log = new AuditLog(
                entityName,
                entityId,
                action,
                fromState,
                toState,
                operatorId,
                operatorUsername,
                details
        );
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getEntityHistory(String entityName, Long entityId) {
        return auditLogRepository.findByEntityNameAndEntityIdOrderByRecordedAtDesc(entityName, entityId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentAuditLogs() {
        return auditLogRepository.findTop100ByOrderByRecordedAtDesc();
    }
}
