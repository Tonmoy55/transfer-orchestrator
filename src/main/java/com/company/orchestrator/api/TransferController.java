package com.company.orchestrator.api;

import com.company.orchestrator.api.dto.ApiResponse;
import com.company.orchestrator.api.util.ResponseEntityBuilder;
import com.company.orchestrator.domain.dto.TransferRequestDto;
import com.company.orchestrator.domain.dto.TransferStatusDto;
import com.company.orchestrator.domain.model.*;
import com.company.orchestrator.domain.service.TransferOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Transfer Operations
 */
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferOrchestrator transferOrchestrator;

    /**
     * Initiates a new transfer
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> initiateTransfer(
            @Valid @RequestBody TransferRequestDto requestDto) {

        log.info("Received transfer request for asset: {}", requestDto.getAssetId());

        TransferRequest request = TransferRequest.builder()
            .consumerId(requestDto.getConsumerId())
            .providerId(requestDto.getProviderId())
            .assetId(requestDto.getAssetId())
            .dataType(requestDto.getDataType())
            .policyIds(requestDto.getPolicyIds())
            .consumerRegion(requestDto.getConsumerRegion())
            .consumerCertificationLevel(requestDto.getConsumerCertificationLevel())
            .usagePurpose(requestDto.getUsagePurpose())
            .build();

        TransferResponse response = transferOrchestrator.initiateTransfer(request);

        if (response.isSuccess()) {
            return ResponseEntityBuilder.accepted(response, "Transfer initiated successfully");
        } else {
            return ResponseEntityBuilder.badRequest(response, "Transfer initiation failed: " + response.getMessage());
        }
    }

    /**
     * Gets transfer status by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransferStatusDto>> getTransferStatus(@PathVariable String id) {
        log.debug("Getting transfer status for ID: {}", id);

        try {
            TransferStatus status = transferOrchestrator.getTransferStatus(id);

            TransferStatusDto dto = TransferStatusDto.builder()
                .transferId(status.getTransferId())
                .currentState(status.getCurrentState())
                .message(status.getMessage())
                .lastUpdated(status.getLastUpdated())
                .retryCount(status.getRetryCount())
                .edcTransferProcessId(status.getEdcTransferProcessId())
                .build();

            return ResponseEntityBuilder.ok(dto, "Transfer status retrieved successfully");

        } catch (IllegalArgumentException e) {
            return ResponseEntityBuilder.notFound("Transfer not found with ID: " + id);
        }
    }

    /**
     * Cancels a transfer
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelTransfer(@PathVariable String id) {
        log.info("Cancelling transfer: {}", id);

        try {
            transferOrchestrator.cancelTransfer(id);
            return ResponseEntityBuilder.ok(null, "Transfer cancelled successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntityBuilder.notFound("Transfer not found with ID: " + id);
        }
    }

    /**
     * Gets audit log for a transfer
     */
    @GetMapping("/{id}/audit")
    public ResponseEntity<ApiResponse<List<AuditEvent>>> getTransferAuditLog(@PathVariable String id) {
        log.debug("Getting audit log for transfer: {}", id);

        List<AuditEvent> auditLog = transferOrchestrator.getTransferAuditLog(id);
        return ResponseEntityBuilder.ok(auditLog, "Audit log retrieved successfully");
    }

    /**
     * Lists all transfers with pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransferStatusDto>>> listTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastUpdated") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("Listing transfers - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
            Sort.by(sortBy).ascending() :
            Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TransferStatus> transfers = transferOrchestrator.listTransfers(pageable);

        Page<TransferStatusDto> dtos = transfers.map(status -> TransferStatusDto.builder()
            .transferId(status.getTransferId())
            .currentState(status.getCurrentState())
            .message(status.getMessage())
            .lastUpdated(status.getLastUpdated())
            .retryCount(status.getRetryCount())
            .edcTransferProcessId(status.getEdcTransferProcessId())
            .build());

        return ResponseEntityBuilder.ok(dtos, "Transfers retrieved successfully");
    }
}

