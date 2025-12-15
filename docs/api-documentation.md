# API Documentation

## Base URL
```
http://localhost:9090/api/v1
```

## Interactive Documentation
- **Swagger UI**: http://localhost:9090/swagger-ui.html
- **OpenAPI Spec**: http://localhost:9090/api-docs

## Authentication
Currently, the API does not require authentication. For production, implement OAuth2/JWT.

---

## Transfer Endpoints

### 1. Initiate Transfer

Creates a new data transfer request with policy evaluation.

**Endpoint**: `POST /api/v1/transfers`

**Request Body**:
```json
{
  "consumerId": "consumer-1",
  "providerId": "provider-1",
  "assetId": "asset-123",
  "dataType": "PRODUCTION_DATA",
  "consumerRegion": "EU",
  "consumerCertificationLevel": "ISO9001",
  "usagePurpose": "QUALITY_ANALYSIS",
  "policyIds": ["policy-1", "policy-2"]
}
```

**Request Fields**:
- `consumerId` (required): Identifier of the data consumer
- `providerId` (required): Identifier of the data provider
- `assetId` (required): Identifier of the asset to transfer
- `dataType` (optional): Type of data being transferred
- `consumerRegion` (required): Geographic region of consumer (e.g., "EU", "US")
- `consumerCertificationLevel` (required): Consumer's certification (e.g., "ISO9001", "ISO27001", "TISAX")
- `usagePurpose` (required): Purpose of data usage (e.g., "QUALITY_ANALYSIS", "SUPPLY_CHAIN_OPTIMIZATION")
- `policyIds` (optional): Specific policies to evaluate

**Success Response** (202 Accepted):
```json
{
  "transferId": "550e8400-e29b-41d4-a716-446655440000",
  "initialState": "APPROVED",
  "message": "Transfer approved, processing started",
  "success": true
}
```

**Error Response** (400 Bad Request) - Policy Denied:
```json
{
  "transferId": "550e8400-e29b-41d4-a716-446655440000",
  "initialState": "DENIED",
  "message": "Policy violations: GEOGRAPHIC: EU/EEA Only",
  "success": false
}
```

**Error Response** (400 Bad Request) - Validation Error:
```json
{
  "status": 400,
  "message": "Validation Error",
  "details": "consumerId: must not be blank, assetId: must not be blank",
  "timestamp": "2025-12-12T10:30:45"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:9090/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "consumer-1",
    "providerId": "provider-1",
    "assetId": "asset-123",
    "dataType": "PRODUCTION_DATA",
    "consumerRegion": "EU",
    "consumerCertificationLevel": "ISO9001",
    "usagePurpose": "QUALITY_ANALYSIS"
  }'
```

---

### 2. Get Transfer Status

Retrieves the current status of a specific transfer.

**Endpoint**: `GET /api/v1/transfers/{id}`

**Path Parameters**:
- `id`: Transfer ID

**Success Response** (200 OK):
```json
{
  "transferId": "550e8400-e29b-41d4-a716-446655440000",
  "currentState": "COMPLETED",
  "message": "Transfer completed successfully",
  "lastUpdated": "2025-12-12T10:35:22",
  "retryCount": 0,
  "edcTransferProcessId": "transfer-abc123"
}
```

**Transfer States**:
- `REQUESTED`: Transfer request received
- `POLICY_EVALUATION`: Evaluating policies
- `APPROVED`: All policies passed
- `DENIED`: Policy violations found
- `CONTRACT_NEGOTIATION`: Negotiating with EDC
- `NEGOTIATED`: Contract agreement reached
- `TRANSFER_IN_PROGRESS`: Data transfer in progress
- `COMPLETED`: Transfer completed successfully
- `FAILED`: Transfer failed (after retries)
- `CANCELLED`: Transfer cancelled by user

**Error Response** (404 Not Found):
```json
{
  "status": 404,
  "message": "Transfer not found",
  "timestamp": "2025-12-12T10:30:45"
}
```

**cURL Example**:
```bash
curl http://localhost:9090/api/v1/transfers/550e8400-e29b-41d4-a716-446655440000
```

---

### 3. Cancel Transfer

Cancels an in-progress transfer.

**Endpoint**: `DELETE /api/v1/transfers/{id}`

**Path Parameters**:
- `id`: Transfer ID

**Success Response** (204 No Content)

**Error Response** (404 Not Found)

**cURL Example**:
```bash
curl -X DELETE http://localhost:9090/api/v1/transfers/550e8400-e29b-41d4-a716-446655440000
```

---

### 4. Get Transfer Audit Log

Retrieves complete audit trail for a transfer.

**Endpoint**: `GET /api/v1/transfers/{id}/audit`

**Path Parameters**:
- `id`: Transfer ID

**Success Response** (200 OK):
```json
[
  {
    "eventId": "event-1",
    "transferId": "550e8400-e29b-41d4-a716-446655440000",
    "eventType": "TRANSFER_REQUESTED",
    "actor": "consumer-1",
    "action": "REQUEST_TRANSFER",
    "timestamp": "2025-12-12T10:30:00",
    "details": "Transfer requested for asset: asset-123",
    "metadata": {
      "consumerId": "consumer-1",
      "assetId": "asset-123"
    }
  },
  {
    "eventId": "event-2",
    "transferId": "550e8400-e29b-41d4-a716-446655440000",
    "eventType": "POLICY_EVALUATION",
    "actor": "SYSTEM",
    "action": "POLICY_APPROVED",
    "timestamp": "2025-12-12T10:30:01",
    "details": "All policies satisfied",
    "metadata": {
      "allowed": true,
      "satisfiedPolicies": ["TIME_BASED", "GEOGRAPHIC", "CERTIFICATION"]
    }
  },
  {
    "eventId": "event-3",
    "transferId": "550e8400-e29b-41d4-a716-446655440000",
    "eventType": "STATE_TRANSITION",
    "actor": "SYSTEM",
    "action": "TRANSITION_APPROVED_TO_CONTRACT_NEGOTIATION",
    "timestamp": "2025-12-12T10:30:02",
    "details": "Starting contract negotiation"
  },
  {
    "eventId": "event-4",
    "transferId": "550e8400-e29b-41d4-a716-446655440000",
    "eventType": "TRANSFER_COMPLETED",
    "actor": "SYSTEM",
    "action": "TRANSFER_SUCCESS",
    "timestamp": "2025-12-12T10:35:22",
    "details": "Transfer completed successfully",
    "metadata": {
      "finalState": "COMPLETED",
      "success": true
    }
  }
]
```

**cURL Example**:
```bash
curl http://localhost:9090/api/v1/transfers/550e8400-e29b-41d4-a716-446655440000/audit
```

---

### 5. List Transfers

Retrieves paginated list of all transfers.

**Endpoint**: `GET /api/v1/transfers`

**Query Parameters**:
- `page` (optional, default: 0): Page number (0-based)
- `size` (optional, default: 20): Items per page (max: 100)
- `sortBy` (optional, default: "lastUpdated"): Field to sort by
- `sortDir` (optional, default: "DESC"): Sort direction (ASC/DESC)

**Success Response** (200 OK):
```json
{
  "content": [
    {
      "transferId": "550e8400-e29b-41d4-a716-446655440000",
      "currentState": "COMPLETED",
      "message": "Transfer completed successfully",
      "lastUpdated": "2025-12-12T10:35:22",
      "retryCount": 0,
      "edcTransferProcessId": "transfer-abc123"
    },
    {
      "transferId": "660e8400-e29b-41d4-a716-446655440001",
      "currentState": "IN_PROGRESS",
      "message": "Transfer in progress",
      "lastUpdated": "2025-12-12T10:40:15",
      "retryCount": 0,
      "edcTransferProcessId": "transfer-xyz789"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 150,
  "totalPages": 8,
  "last": false,
  "first": true,
  "numberOfElements": 20
}
```

**cURL Example**:
```bash
# Get first page
curl "http://localhost:9090/api/v1/transfers?page=0&size=20"

# Get second page, sorted by creation date ascending
curl "http://localhost:9090/api/v1/transfers?page=1&size=20&sortBy=createdAt&sortDir=ASC"
```

---

## Analytics Endpoints

### 6. Get Transfer Analytics

Retrieves aggregated transfer statistics and metrics.

**Endpoint**: `GET /api/v1/analytics/transfers`

**Success Response** (200 OK):
```json
{
  "totalTransfers": 150,
  "completedTransfers": 120,
  "failedTransfers": 10,
  "inProgressTransfers": 5,
  "deniedTransfers": 15,
  "successRate": 80.0,
  "transfersByState": {
    "REQUESTED": 0,
    "POLICY_EVALUATION": 0,
    "APPROVED": 0,
    "DENIED": 15,
    "CONTRACT_NEGOTIATION": 0,
    "NEGOTIATED": 0,
    "TRANSFER_IN_PROGRESS": 5,
    "COMPLETED": 120,
    "FAILED": 10,
    "CANCELLED": 0
  }
}
```

**cURL Example**:
```bash
curl http://localhost:9090/api/v1/analytics/transfers
```

---

## Policy Endpoints

### 7. Evaluate Policy (Test)

Tests policy evaluation without creating a transfer.

**Endpoint**: `POST /api/v1/policies/evaluate`

**Request Body**: Same as transfer request

**Success Response** (200 OK):
```json
{
  "allowed": true,
  "violatedPolicies": [],
  "satisfiedPolicies": [
    "TIME_BASED: Business Hours (8 AM - 6 PM)",
    "RATE_LIMIT: Max 100 requests/hour",
    "GEOGRAPHIC: EU/EEA Only",
    "CERTIFICATION: Required Quality Standards",
    "USAGE: Approved Purposes Only"
  ],
  "reason": "All policies satisfied"
}
```

**Policy Violation Response**:
```json
{
  "allowed": false,
  "violatedPolicies": [
    "GEOGRAPHIC: EU/EEA Only",
    "CERTIFICATION: Required Quality Standards"
  ],
  "satisfiedPolicies": [
    "TIME_BASED: Business Hours (8 AM - 6 PM)",
    "USAGE: Approved Purposes Only"
  ],
  "reason": "Policy violations: GEOGRAPHIC: EU/EEA Only, CERTIFICATION: Required Quality Standards"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:9090/api/v1/policies/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "consumer-1",
    "providerId": "provider-1",
    "assetId": "asset-123",
    "consumerRegion": "US",
    "consumerCertificationLevel": "UNKNOWN",
    "usagePurpose": "QUALITY_ANALYSIS"
  }'
```

---

### 8. Get Available Policy Types

Retrieves all available policy types.

**Endpoint**: `GET /api/v1/policies/types`

**Success Response** (200 OK):
```json
[
  "TIME_BASED",
  "RATE_LIMIT",
  "GEOGRAPHIC",
  "CERTIFICATION",
  "USAGE"
]
```

**cURL Example**:
```bash
curl http://localhost:9090/api/v1/policies/types
```

---

### 9. List All Policies

Retrieves all configured policies from the orchestrator, including full metadata and configuration.

**Endpoint**: `GET /api/v1/policies`

**Success Response** (200 OK):
```json
{
  "statusCode": 200,
  "message": "Policies retrieved successfully",
  "success": true,
  "data": [
    {
      "id": "1",
      "name": "Time based EU only",
      "type": "TIME_BASED",
      "description": "Allow only in business hours",
      "configuration": "{\"from\":\"08:00\",\"to\":\"18:00\"}",
      "active": true
    }
  ]
}
```

**Notes**:
- `type` values match the `PolicyType` enum and the values returned by `GET /api/v1/policies/types`.
- `configuration` is a JSON string whose structure depends on the specific policy type (e.g. time ranges, regions, rate limits).

**cURL Example**:
```bash
curl http://localhost:9090/api/v1/policies
```

---

## Policy Rules Reference

### TIME_BASED Policy
- **Rule**: Transfers only allowed during business hours (8 AM - 6 PM)
- **Timezone**: Server timezone (should be CET for Catena-X)
- **Violation**: Request outside business hours

### RATE_LIMIT Policy
- **Rule**: Maximum 100 requests per hour per consumer
- **Window**: Rolling 1-hour window
- **Tracking**: Stored in database
- **Violation**: Consumer exceeds 100 requests in last hour

### GEOGRAPHIC Policy
- **Rule**: Data must not leave EU/EEA region
- **Allowed Regions**: EU, EEA, GERMANY, FRANCE, ITALY
- **Validation**: Case-insensitive
- **Violation**: Consumer region not in allowed list

### CERTIFICATION Policy
- **Rule**: Consumer must have quality certification
- **Required Certifications**: ISO9001, ISO27001, TISAX
- **Validation**: Case-insensitive
- **Violation**: Consumer certification not in required list

### USAGE Policy
- **Rule**: Data can only be used for approved purposes
- **Allowed Purposes**: 
  - QUALITY_ANALYSIS
  - SUPPLY_CHAIN_OPTIMIZATION
  - COMPLIANCE_REPORTING
- **Validation**: Case-insensitive
- **Violation**: Usage purpose not in allowed list

---

## Error Response Format

All error responses follow this format:

```json
{
  "status": 400,
  "message": "Error Type",
  "details": "Detailed error description",
  "timestamp": "2025-12-12T10:30:45"
}
```

### HTTP Status Codes

- `200 OK`: Successful GET request
- `202 Accepted`: Transfer initiated successfully (async processing)
- `204 No Content`: Successful DELETE request
- `400 Bad Request`: Validation error or policy violation
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

---

## Postman Collection

Import this collection into Postman:

```json
{
  "info": {
    "name": "Transfer Orchestrator API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Transfers",
      "item": [
        {
          "name": "Initiate Transfer",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"consumerId\": \"consumer-1\",\n  \"providerId\": \"provider-1\",\n  \"assetId\": \"asset-123\",\n  \"dataType\": \"PRODUCTION_DATA\",\n  \"consumerRegion\": \"EU\",\n  \"consumerCertificationLevel\": \"ISO9001\",\n  \"usagePurpose\": \"QUALITY_ANALYSIS\"\n}"
            },
            "url": "{{baseUrl}}/api/v1/transfers"
          }
        },
        {
          "name": "Get Transfer Status",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/v1/transfers/{{transferId}}"
          }
        },
        {
          "name": "List Transfers",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/v1/transfers?page=0&size=20"
          }
        },
        {
          "name": "Get Audit Log",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/v1/transfers/{{transferId}}/audit"
          }
        },
        {
          "name": "Cancel Transfer",
          "request": {
            "method": "DELETE",
            "url": "{{baseUrl}}/api/v1/transfers/{{transferId}}"
          }
        }
      ]
    },
    {
      "name": "Analytics",
      "item": [
        {
          "name": "Get Transfer Analytics",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/v1/analytics/transfers"
          }
        }
      ]
    },
    {
      "name": "Policies",
      "item": [
        {
          "name": "Evaluate Policy",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"consumerId\": \"consumer-1\",\n  \"providerId\": \"provider-1\",\n  \"assetId\": \"asset-123\",\n  \"consumerRegion\": \"EU\",\n  \"consumerCertificationLevel\": \"ISO9001\",\n  \"usagePurpose\": \"QUALITY_ANALYSIS\"\n}"
            },
            "url": "{{baseUrl}}/api/v1/policies/evaluate"
          }
        },
        {
          "name": "Get Available Policy Types",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/v1/policies/types"
          }
        },
        {
          "name": "List All Policies",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/v1/policies"
          }
        }
      ]
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:9090"
    },
    {
      "key": "transferId",
      "value": "550e8400-e29b-41d4-a716-446655440000"
    }
  ]
}
```

---

## Rate Limiting

Current implementation tracks rate limits per consumer in the database. For production:

1. **View Current Usage**:
```sql
SELECT consumer_id, SUM(request_count) as total_requests
FROM rate_limit_tracking
WHERE window_start >= NOW() - INTERVAL '1 HOUR'
GROUP BY consumer_id;
```

2. **Reset Rate Limit** (for testing):
```sql
DELETE FROM rate_limit_tracking WHERE consumer_id = 'consumer-1';
```

---

## Testing Tips

### Test Successful Transfer
```bash
curl -X POST http://localhost:9090/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "consumer-test",
    "providerId": "provider-test",
    "assetId": "asset-test-001",
    "consumerRegion": "EU",
    "consumerCertificationLevel": "ISO9001",
    "usagePurpose": "QUALITY_ANALYSIS"
  }'
```

### Test Policy Violations

**Geographic Violation**:
```bash
curl -X POST http://localhost:9090/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "consumer-test",
    "providerId": "provider-test",
    "assetId": "asset-test-002",
    "consumerRegion": "US",
    "consumerCertificationLevel": "ISO9001",
    "usagePurpose": "QUALITY_ANALYSIS"
  }'
```

**Certification Violation**:
```bash
curl -X POST http://localhost:9090/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "consumer-test",
    "providerId": "provider-test",
    "assetId": "asset-test-003",
    "consumerRegion": "EU",
    "consumerCertificationLevel": "UNKNOWN",
    "usagePurpose": "QUALITY_ANALYSIS"
  }'
```

**Usage Violation**:
```bash
curl -X POST http://localhost:9090/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "consumer-test",
    "providerId": "provider-test",
    "assetId": "asset-test-004",
    "consumerRegion": "EU",
    "consumerCertificationLevel": "ISO9001",
    "usagePurpose": "MARKETING"
  }'
```
