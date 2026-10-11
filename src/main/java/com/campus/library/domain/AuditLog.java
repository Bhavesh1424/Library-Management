package com.campus.library.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Immutable audit log record ensuring full chain-of-custody traceability (NFR-4).
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_name, entity_id"),
        @Index(name = "idx_audit_recorded_at", columnList = "recorded_at")
    }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "entity_name", nullable = false, length = 50)
    private String entityName;

    @NotNull
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @NotBlank
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "from_state", length = 30)
    private String fromState;

    @Column(name = "to_state", length = 30)
    private String toState;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_username", length = 100)
    private String operatorUsername;

    @NotNull
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    @Column(name = "details", length = 500)
    private String details;

    public AuditLog() {
    }

    public AuditLog(String entityName, Long entityId, String action, String fromState, String toState, 
                    Long operatorId, String operatorUsername, String details) {
        this.entityName = entityName;
        this.entityId = entityId;
        this.action = action;
        this.fromState = fromState;
        this.toState = toState;
        this.operatorId = operatorId;
        this.operatorUsername = operatorUsername;
        this.recordedAt = LocalDateTime.now();
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFromState() {
        return fromState;
    }

    public void setFromState(String fromState) {
        this.fromState = fromState;
    }

    public String getToState() {
        return toState;
    }

    public void setToState(String toState) {
        this.toState = toState;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
