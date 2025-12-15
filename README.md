# Transfer Orchestrator - Policy-Aware Data Transfer System

#Please open the "docs" folder from the project root directory for detailed documentation.
   1. API Documentation
   2. API Response format
   3. Architecture Overview
   4. Design Decisions
   5. System Design diagram
   6. Postman API Collection 

## Postman API Collection is available in the `docs` folder.
# Use application-dev.yml for development profile.
# Craete a database named `transferdb` in your local PostgreSQL server before running the application.
# Change the PostgreSQL username and password in `application-dev.yaml` as per your local setup.
# Both H2 and PostgreSQL configurations are provided. 
# By default, the application uses PostgreSQL. For easy installation, switch to H2 by updating the `application-dev.yaml` file. 
# Used flyway for database migration. The initial schema is in `src/main/resources/db/migration/V1__Initial_Schema.sql`.

## Application Metrics and Monitoring
# Actuator:
http://localhost:9090/actuator/health
# Prometheus:
http://localhost:9090/actuator/prometheus


## Overview

This is a production-ready implementation of a **Policy-Aware Data Transfer Orchestration System** built on top of Eclipse EDC for Catena-X dataspace compliance. The system manages complex data transfers between multiple EDC connectors while enforcing business policies, ensuring data sovereignty, and maintaining comprehensive audit trails.

## Features

### Core Components

1. **Policy Evaluation Engine** ⭐
   - Time-based access policies (business hours enforcement)
   - Rate limiting (100 requests/hour per consumer)
   - Geographic restrictions (GDPR compliance - EU only)
   - Certification requirements (ISO 9001, ISO 27001, TISAX)
   - Usage purpose validation
   - Composable policy evaluation with AND logic

2. **Transfer Orchestration Service** ⭐
   - Complete transfer lifecycle management
   - State machine: REQUESTED → POLICY_EVALUATION → APPROVED/DENIED → CONTRACT_NEGOTIATION → NEGOTIATED → TRANSFER_IN_PROGRESS → COMPLETED/FAILED/CANCELLED
   - Automatic retry with exponential backoff (max 3 attempts)
   - Async workflow execution
   - Transaction management

3. **Audit & Compliance Module** ⭐
   - Immutable, append-only audit logs
   - Complete transfer lifecycle tracking
   - Policy evaluation results logging
   - State transition tracking
   - Compliance reporting capabilities

4. **EDC Integration Layer** ⭐
   - Mock EDC connector client (production-ready interface)
   - Contract negotiation support
   - Transfer process management
   - Connection resilience

## Technology Stack

- **Java 25** - LTS version
- **Spring Boot 4.0.0 ** - Latest stable framework version
- **Maven** - Build tool
- **H2 Database** - In-memory database (easy to switch to PostgreSQL)
- **PostgresQL** - Recommended for production
- **Flyway** - Database migration
- **JUnit 5** - Testing framework
- **SpringDoc OpenAPI** - API documentation
- **Lombok** - Code generation
- **SLF4J/Logback** - Logging

## Project Structure

```
transfer-orchestrator/
├── src/
│   ├── main/
│   │   ├── java/com/company/orchestrator/
│   │   │   ├── api/                          # REST Controllers
│   │   │   │   ├── TransferController.java
│   │   │   │   ├── AnalyticsController.java
│   │   │   │   ├── PolicyController.java
│   │   │   │   └── dto/                      # Data Transfer Objects
│   │   │   ├── domain/
│   │   │   │   ├── model/                    # Domain Models
│   │   │   │   └── service/                  # Business Logic
│   │   │   │       ├── TransferOrchestrator.java
│   │   │   │       ├── PolicyEvaluationService.java
│   │   │   │       └── AuditService.java
│   │   │   ├── infrastructure/
│   │   │   │   ├── edc/                      # EDC Integration
│   │   │   │   │   ├── EdcConnectorClient.java
│   │   │   │   │   └── MockEdcConnectorClient.java
│   │   │   │   ├── persistence/              # Database Layer
│   │   │   │   │   ├── entity/
│   │   │   │   │   └── repository/
│   │   │   │   ├── policy/                   # Policy Evaluators
│   │   │   │   │   ├── TimeBasedPolicyEvaluator.java
│   │   │   │   │   ├── RateLimitPolicyEvaluator.java
│   │   │   │   │   ├── GeographicPolicyEvaluator.java
│   │   │   │   │   ├── CertificationPolicyEvaluator.java
│   │   │   │   │   └── UsagePolicyEvaluator.java
│   │   │   │   └── config/                   # Configuration
│   │   │   ├── exeption/                     # Exception Handling
│   │   │   └── TransferOrchestratorApplication.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── db/migration/
│   │           └── V1__Initial_Schema.sql
│   └── test/                                 # Comprehensive Tests
├── docs/                                     # Documentation
├── pom.xml
└── README.md
```

## Setup Instructions

### Prerequisites

- Java 25 (JDK 25)
- Maven 3.9+
- Git

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd transfer-orchestrator
```

2. **Build the project**
```bash
mvn clean install
```

3. **Run the application**
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:9090`

### Access Points

- **API Base URL**: `http://localhost:9090/api/v1`
- **Swagger UI**: `http://localhost:9090/swagger-ui.html`
- **OpenAPI Docs**: `http://localhost:9090/api-docs`
- **Actuator URL**: `http://localhost:9090/actuator`
- **Prometheus URL**: `http://localhost:9090/actuator/prometheus`
- **H2 Console**: `http://localhost:9090/h2-console`
  - JDBC URL: `jdbc:h2:mem:transferdb`
  - Username: `sa`
  - Password: (leave empty)
- **PostgreSQL**:
  - JDBC URL: `jdbc:postgresql://localhost:5432/transferdb`
  - Host: `localhost`
  - Port: `5432`
  - Database: `transferdb`
  - Username: `postgres`
  - Password: `your-password`

## Running Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn clean test jacoco:report

# Run specific test
mvn test -Dtest=TransferControllerIntegrationTest
```

## API Usage Examples

### 1. Initiate a Transfer

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

**Response:**
```json
{
  "transferId": "550e8400-e29b-41d4-a716-446655440000",
  "initialState": "APPROVED",
  "message": "Transfer approved, processing started",
  "success": true
}
```

### 2. Get Transfer Status

```bash
curl http://localhost:9090/api/v1/transfers/550e8400-e29b-41d4-a716-446655440000
```

**Response:**
```json
{
  "transferId": "550e8400-e29b-41d4-a716-446655440000",
  "currentState": "COMPLETED",
  "message": "Transfer completed successfully",
  "lastUpdated": "2025-12-12T10:30:45",
  "retryCount": 0,
  "edcTransferProcessId": "transfer-abc123"
}
```

### 3. Get Audit Log

```bash
curl http://localhost:9090/api/v1/transfers/550e8400-e29b-41d4-a716-446655440000/audit
```

### 4. List Transfers (Paginated)

```bash
curl "http://localhost:9090/api/v1/transfers?page=0&size=20&sortBy=lastUpdated&sortDir=DESC"
```

### 5. Get Analytics

```bash
curl http://localhost:9090/api/v1/analytics/transfers
```

**Response:**
```json
{
  "totalTransfers": 150,
  "completedTransfers": 120,
  "failedTransfers": 10,
  "inProgressTransfers": 5,
  "deniedTransfers": 15,
  "successRate": 80.0,
  "transfersByState": {
    "COMPLETED": 120,
    "FAILED": 10,
    "IN_PROGRESS": 5,
    "DENIED": 15
  }
}
```

### 6. Test Policy Evaluation

```bash
curl -X POST http://localhost:9090/api/v1/policies/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "consumerId": "consumer-1",
    "providerId": "provider-1",
    "assetId": "asset-123",
    "consumerRegion": "US",
    "consumerCertificationLevel": "ISO9001",
    "usagePurpose": "QUALITY_ANALYSIS"
  }'
```

### 7. Cancel Transfer

```bash
curl -X DELETE http://localhost:9090/api/v1/transfers/550e8400-e29b-41d4-a716-446655440000
```

## Policy Configuration

The system evaluates 5 types of policies:

1. **TIME_BASED**: Business hours only (8 AM - 6 PM)
2. **RATE_LIMIT**: Max 100 requests per hour per consumer
3. **GEOGRAPHIC**: EU/EEA regions only
4. **CERTIFICATION**: Requires ISO9001, ISO27001, or TISAX
5. **USAGE**: Approved purposes only (QUALITY_ANALYSIS, SUPPLY_CHAIN_OPTIMIZATION, COMPLIANCE_REPORTING)

All policies must pass for a transfer to be approved.

## Database Schema

The application uses Flyway for database migrations. The initial schema includes:

- `transfers` - Transfer records
- `audit_events` - Audit trail (append-only)
- `policies` - Policy definitions
- `rate_limit_tracking` - Rate limiting counters

## Known Limitations

1. **Mock EDC Integration**: The EDC connector client is mocked. In production, this would call actual EDC Management API endpoints.

3. **Async Processing**: Transfer workflows run asynchronously but lack distributed coordination. For production scale, consider adding message queues (Kafka/RabbitMQ).

4. **Time-based Policy**: Currently uses server time. Should use configurable timezone (CET) for Catena-X compliance.

5. **No Authentication**: API endpoints are not secured. Add Spring Security with OAuth2 for production.

## Future Enhancements

1. **Real EDC Integration**: Connect to actual Eclipse EDC connectors
2. **Distributed Tracing**: Add OpenTelemetry for observability
3. **Message Queue**: Kafka integration for event-driven architecture
4. **Metrics**: Prometheus metrics and Grafana dashboards
5. **Circuit Breaker**: Resilience4j for fault tolerance
6. **Caching**: Redis for policy and rate limit caching
7. **Security**: OAuth2/OpenID Connect authentication
8. **Multi-tenancy**: Support for multiple organizations
9. **Advanced Policies**: Custom policy DSL, time windows, dynamic limits
10. **Notifications**: Email/Webhook notifications for transfer events



## For questions or issues, please contact: nsu.tonmoy@gmail.com
## Prepared by: Tonmoy Sikder

