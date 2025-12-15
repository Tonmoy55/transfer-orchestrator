package com.company.orchestrator.domain.dto;

import com.company.orchestrator.domain.enums.TransferState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for transfer status response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferStatusDto {
    private String transferId;
    private TransferState currentState;
    private String message;
    private LocalDateTime lastUpdated;
    private Integer retryCount;
    private String edcTransferProcessId;
}

