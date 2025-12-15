package com.company.orchestrator.domain.model;

import com.company.orchestrator.domain.enums.TransferState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Current status of a transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferStatus {
    private String transferId;
    private TransferState currentState;
    private String message;
    private LocalDateTime lastUpdated;
    private Integer retryCount;
    private String edcTransferProcessId;
}

