# Global API Response Format

## Overview

All API endpoints in the Transfer Orchestrator now return responses in a **standardized format** with the following structure:

```json
{
  "statusCode": 200,
  "message": "Success message",
  "data": { ... },
  "timestamp": "2025-12-12T18:30:00",
  "success": true
}
```

## Response Structure

| Field | Type | Description |
|-------|------|-------------|
| `statusCode` | Integer | HTTP status code (200, 201, 400, 404, 500, etc.) |
| `message` | String | Human-readable message describing the response |
| `data` | Object/Array/null | The actual response payload |
| `timestamp` | DateTime | ISO 8601 timestamp when the response was generated |
| `success` | Boolean | `true` for successful responses, `false` for errors |

## Success Response Examples

### 1. Create/Initiate Transfer (202 Accepted)

**Request:**
```bash
POST /api/v1/transfers
```

**Response:**
```json
{
  "statusCode": 202,
  "message": "Transfer initiated successfully",
  "data": {
    "transferId": "550e8400-e29b-41d4-a716-446655440000",
    "initialState": "APPROVED",
    "message": "Transfer approved, processing started",
    "success": true
  },
  "timestamp": "2025-12-12T18:30:00",
  "success": true
}
```

### 2. Get Transfer Status (200 OK)

**Request:**
```bash
GET /api/v1/transfers/550e8400-e29b-41d4-a716-446655440000
```

**Response:**
```json
{
  "statusCode": 200,
  "message": "Transfer status retrieved successfully",
  "data": {
    "transferId": "550e8400-e29b-41d4-a716-446655440000",
    "currentState": "TRANSFER_IN_PROGRESS",
    "message": "Data transfer in progress",
    "lastUpdated": "2025-12-12T18:30:00",
    "retryCount": 0,
    "edcTransferProcessId": "transfer-abc123"
  },
  "timestamp": "2025-12-12T18:30:05",
  "success": true
}
```

### 3. List Transfers with Pagination (200 OK)

**Request:**
```bash
GET /api/v1/transfers?page=0&size=10
```

**Response:**
```json
{
  "statusCode": 200,
  "message": "Transfers retrieved successfully",
  "data": {
    "content": [
      {
        "transferId": "550e8400-e29b-41d4-a716-446655440000",
        "currentState": "COMPLETED",
        "message": "Transfer completed successfully",
        "lastUpdated": "2025-12-12T18:25:00",
        "retryCount": 0
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2025-12-12T18:30:10",
  "success": true
}
```

### 4. Get Analytics (200 OK)

**Request:**
```bash
GET /api/v1/analytics/transfers
```

**Response:**
```json
{
  "statusCode": 200,
  "message": "Analytics retrieved successfully",
  "data": {
    "totalTransfers": 100,
    "completedTransfers": 85,
    "failedTransfers": 10,
    "inProgressTransfers": 3,
    "deniedTransfers": 2,
    "successRate": 85.0,
    "transfersByState": {
      "COMPLETED": 85,
      "FAILED": 10,
      "TRANSFER_IN_PROGRESS": 3,
      "DENIED": 2
    }
  },
  "timestamp": "2025-12-12T18:30:15",
  "success": true
}
```

### 5. Evaluate Policy (200 OK)

**Request:**
```bash
POST /api/v1/policies/evaluate
```

**Response:**
```json
{
  "statusCode": 200,
  "message": "Policy evaluation passed",
  "data": {
    "allowed": true,
    "reason": "All policies satisfied",
    "evaluatedPolicies": [
      "TIME_BASED",
      "GEOGRAPHIC",
      "CERTIFICATION"
    ],
    "violations": []
  },
  "timestamp": "2025-12-12T18:30:20",
  "success": true
}
```

### 6. Cancel Transfer (200 OK)

**Request:**
```bash
DELETE /api/v1/transfers/550e8400-e29b-41d4-a716-446655440000
```

**Response:**
```json
{
  "statusCode": 200,
  "message": "Transfer cancelled successfully",
  "data": null,
  "timestamp": "2025-12-12T18:30:25",
  "success": true
}
```

## Error Response Examples

### 1. Validation Error (400 Bad Request)

**Request:**
```bash
POST /api/v1/transfers
{
  "consumerId": "consumer-1"
  // Missing required fields
}
```

**Response:**
```json
{
  "statusCode": 400,
  "message": "Validation failed: providerId: Provider ID is required, assetId: Asset ID is required",
  "data": {
    "fieldErrors": {
      "providerId": "Provider ID is required",
      "assetId": "Asset ID is required",
      "dataType": "Data type is required"
    },
    "errorCount": 3
  },
  "timestamp": "2025-12-12T18:30:30",
  "success": false
}
```

### 2. Transfer Denied by Policy (400 Bad Request)

**Request:**
```bash
POST /api/v1/transfers
{
  "consumerId": "consumer-1",
  "providerId": "provider-1",
  "assetId": "asset-123",
  "dataType": "PRODUCTION_DATA",
  "consumerRegion": "US",
  "consumerCertificationLevel": "ISO9001",
  "usagePurpose": "QUALITY_ANALYSIS"
}
```

**Response:**
```json
{
  "statusCode": 400,
  "message": "Transfer initiation failed: Geographic policy violation: Data transfer outside EU region not allowed",
  "data": {
    "transferId": "550e8400-e29b-41d4-a716-446655440000",
    "initialState": "DENIED",
    "message": "Geographic policy violation: Data transfer outside EU region not allowed",
    "success": false
  },
  "timestamp": "2025-12-12T18:30:35",
  "success": false
}
```

### 3. Resource Not Found (404 Not Found)

**Request:**
```bash
GET /api/v1/transfers/non-existent-id
```

**Response:**
```json
{
  "statusCode": 404,
  "message": "Transfer not found with ID: non-existent-id",
  "data": null,
  "timestamp": "2025-12-12T18:30:40",
  "success": false
}
```

### 4. Internal Server Error (500)

**Response:**
```json
{
  "statusCode": 500,
  "message": "Internal server error occurred",
  "data": {
    "errorType": "NullPointerException",
    "errorDetails": "Cannot invoke method on null object"
  },
  "timestamp": "2025-12-12T18:30:45",
  "success": false
}
```

## HTTP Status Codes Used

| Status Code | Usage |
|-------------|-------|
| 200 OK | Successful GET, PUT, DELETE operations |
| 201 Created | Resource created successfully (if implemented) |
| 202 Accepted | Transfer initiated (asynchronous processing) |
| 204 No Content | Successful operation with no content to return |
| 400 Bad Request | Validation errors, policy violations |
| 404 Not Found | Resource not found |
| 500 Internal Server Error | Unexpected server errors |

## Integration Guidelines

### Frontend Integration

**TypeScript Interface:**
```typescript
interface ApiResponse<T> {
  statusCode: number;
  message: string;
  data: T | null;
  timestamp: string;
  success: boolean;
}

// Usage
async function getTransferStatus(id: string): Promise<ApiResponse<TransferStatus>> {
  const response = await fetch(`/api/v1/transfers/${id}`);
  return await response.json();
}

// Handle response
const result = await getTransferStatus('123');
if (result.success) {
  console.log('Transfer status:', result.data);
} else {
  console.error('Error:', result.message);
}
```

### Checking Success

Always check the `success` field:

```javascript
if (response.success) {
  // Handle successful response
  processData(response.data);
} else {
  // Handle error
  showError(response.message);
}
```

### Error Handling

```javascript
try {
  const response = await fetch('/api/v1/transfers', {
    method: 'POST',
    body: JSON.stringify(transferRequest)
  });
  
  const result = await response.json();
  
  if (result.success) {
    console.log('Transfer initiated:', result.data.transferId);
  } else {
    // Business logic error (e.g., policy violation)
    console.error('Transfer failed:', result.message);
    
    // Check if validation errors exist
    if (result.data?.fieldErrors) {
      displayValidationErrors(result.data.fieldErrors);
    }
  }
} catch (error) {
  // Network or parsing error
  console.error('Request failed:', error);
}
```

## Benefits

1. **Consistency**: All endpoints return the same response structure
2. **Clarity**: Clear distinction between success and error states
3. **Debugging**: Timestamps help with debugging and logging
4. **Type Safety**: Easy to create type-safe clients
5. **Error Handling**: Structured error information with details
6. **HTTP Compliance**: Status codes match HTTP standards

## Migration Notes

### Old Format
```json
{
  "transferId": "123",
  "success": true
}
```

### New Format
```json
{
  "statusCode": 202,
  "message": "Transfer initiated successfully",
  "data": {
    "transferId": "123",
    "success": true
  },
  "timestamp": "2025-12-12T18:30:00",
  "success": true
}
```

**Key Changes:**
- Actual data is now wrapped in the `data` field
- Additional metadata fields: `statusCode`, `message`, `timestamp`, `success`
- HTTP status code is included in the response body
- All responses follow the same structure

## API Testing

### Using cURL

```bash
# Success case
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
  }' | jq

# Error case (validation)
curl -X POST http://localhost:9090/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{"consumerId": "consumer-1"}' | jq
```

### Using Postman

All responses will now have the standardized structure. Update your test scripts to access data via `response.data` instead of the root level.

**Example Postman Test:**
```javascript
pm.test("Status code is 202", function () {
    pm.response.to.have.status(202);
});

pm.test("Response has standard format", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('statusCode');
    pm.expect(jsonData).to.have.property('message');
    pm.expect(jsonData).to.have.property('data');
    pm.expect(jsonData).to.have.property('timestamp');
    pm.expect(jsonData).to.have.property('success');
});

pm.test("Transfer was successful", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.be.true;
    pm.expect(jsonData.data.transferId).to.exist;
});
```

