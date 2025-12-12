package com.company.orchestrator.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for transfer analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferAnalyticsDto {
    private Long totalTransfers;
    private Long completedTransfers;
    private Long failedTransfers;
    private Long inProgressTransfers;
    private Long deniedTransfers;
    private Double successRate;
    private Map<String, Long> transfersByState;
}

