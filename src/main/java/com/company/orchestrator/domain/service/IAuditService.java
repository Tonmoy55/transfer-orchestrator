package com.company.orchestrator.domain.service;

import com.company.orchestrator.domain.model.AuditEvent;
import com.company.orchestrator.domain.model.PolicyEvaluationResult;
import com.company.orchestrator.domain.model.TransferRequest;
import com.company.orchestrator.domain.enums.TransferState;

import java.util.List;

/**
 * Audit Service Interface
 * Defines contract for comprehensive audit logging
 */
public interface IAuditService {

    /**
     * Logs transfer request initiation
     * @param request The transfer request details
     */
    void logTransferRequest(TransferRequest request);

    /**
     * Logs policy evaluation result
     * @param transferId The transfer ID
     * @param result The policy evaluation result
     */
    void logPolicyEvaluation(String transferId, PolicyEvaluationResult result);

    /**
     * Logs state transition
     * @param transferId The transfer ID
     * @param from The previous state
     * @param to The new state
     */
    void logStateTransition(String transferId, TransferState from, TransferState to);


    /**
     * Logs state transition
     * @param transferId The transfer ID
     * @param from The previous state
     * @param to The new state
     */
    void logStateTransition(String transferId, TransferState from, TransferState to, String reason);

    /**
     * Logs transfer completion
     * @param transferId The transfer ID
     * @param finalState The final state
     * @param message Completion message
     */
    void logTransferCompletion(String transferId, TransferState finalState, String message);

    /**
     * Retrieves audit trail for a specific transfer
     * Query capability for compliance reporting
     * @param transferId The transfer ID
     * @return List of audit events
     */
    List<AuditEvent> getAuditTrail(String transferId);

    //TODO: Add method for generating compliance reports. As this is not in the requirement doc, leaving it commented for now.
//    ComplianceReport generateComplianceReport(DateRange range);
}

