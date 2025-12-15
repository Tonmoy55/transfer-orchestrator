package com.company.orchestrator.api;

import com.company.orchestrator.api.dto.ApiResponse;
import com.company.orchestrator.api.util.ResponseEntityBuilder;
import com.company.orchestrator.domain.dto.TransferAnalyticsDto;
import com.company.orchestrator.domain.enums.TransferState;
import com.company.orchestrator.infrastructure.persistence.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for Transfer Analytics
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final TransferRepository transferRepository;

    /**
     * Gets transfer analytics and statistics
     */
    @GetMapping("/transfers")
    public ResponseEntity<ApiResponse<TransferAnalyticsDto>> getTransferAnalytics() {
        log.debug("Getting transfer analytics");

        Long totalTransfers = transferRepository.count();
        Long completedTransfers = transferRepository.countByState(TransferState.COMPLETED);
        Long failedTransfers = transferRepository.countByState(TransferState.FAILED);
        Long inProgressTransfers = transferRepository.countByState(TransferState.TRANSFER_IN_PROGRESS);
        Long deniedTransfers = transferRepository.countByState(TransferState.DENIED);

        Double successRate = totalTransfers > 0 ?
            (completedTransfers.doubleValue() / totalTransfers.doubleValue() * 100) : 0.0;

        // Get count by state
        Map<String, Long> transfersByState = Arrays.stream(TransferState.values())
            .collect(Collectors.toMap(
                TransferState::name,
                    transferRepository::countByState
            ));

        TransferAnalyticsDto analytics = TransferAnalyticsDto.builder()
            .totalTransfers(totalTransfers)
            .completedTransfers(completedTransfers)
            .failedTransfers(failedTransfers)
            .inProgressTransfers(inProgressTransfers)
            .deniedTransfers(deniedTransfers)
            .successRate(successRate)
            .transfersByState(transfersByState)
            .build();

        return ResponseEntityBuilder.ok(analytics, "Analytics retrieved successfully");
    }
}

