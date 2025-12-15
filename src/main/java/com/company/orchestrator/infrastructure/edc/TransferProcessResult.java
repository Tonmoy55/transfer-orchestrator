package com.company.orchestrator.infrastructure.edc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of EDC transfer process initiation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferProcessResult {
    private String transferProcessId;
    private boolean success;
    private String message;
}

