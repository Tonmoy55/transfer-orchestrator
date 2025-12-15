package com.company.orchestrator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Audit event model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String eventId;
    private String transferId;
    private String eventType;
    private String actor;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    private String details;
}

