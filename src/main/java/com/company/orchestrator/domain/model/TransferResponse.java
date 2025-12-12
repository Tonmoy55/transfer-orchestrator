package com.company.orchestrator.domain.model;

import com.company.orchestrator.domain.enums.TransferState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of transfer initiation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private String transferId;
    private TransferState initialState;
    private String message;
    private boolean success;
}

