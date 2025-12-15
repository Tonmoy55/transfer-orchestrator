# System Architecture

## Overview

The Transfer Orchestrator is designed as a layered application that sits between client applications and Eclipse EDC connectors, providing policy enforcement, orchestration, and audit capabilities.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Applications                      │
└────────────────────────────┬────────────────────────────────────┘
                             │ REST API
┌────────────────────────────▼────────────────────────────────────┐
│                      Transfer Orchestrator                      │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────┐    │
│  │   API Layer  │  │Policy Engine │  │  Audit Service      │    │
│  │  (REST)      │  │              │  │                     │    │
│  └──────┬───────┘  └───────┬──────┘  └────────┬────────────┘    │
│         │                  │                  │                 │
│  ┌──────▼──────────────────▼──────────────────▼──────────────┐  │
│  │         Transfer Orchestration Service                    │  │
│  │   (State Machine, Retry Logic, Workflow Management)       │  │
│  └──────┬────────────────────────────────────┬───────────────┘  │
│         │                                    │                  │
│  ┌──────▼──────────┐                  ┌──────▼──────────┐       │
│  │  EDC Client     │                  │  Persistence    │       │
│  │  Integration    │                  │  (H2/Postgres)  │       │
│  └──────┬──────────┘                  └─────────────────┘       │
└─────────┼───────────────────────────────────────────────────────┘
          │
┌─────────▼─────────────────────────────────────────────────────┐
│                    Eclipse EDC Connectors                     │
│  ┌─────────────┐              ┌─────────────┐                 │
│  │  Provider   │◄────────────►│  Consumer   │                 │
│  │  Connector  │              │  Connector  │                 │
│  └─────────────┘              └─────────────┘                 │
└───────────────────────────────────────────────────────────────┘
```

## Component Architecture

### 1. API Layer
- **REST Controllers**: TransferController, PolicyController, AnalyticsController
- **DTOs**: Request/Response data transfer objects
- **Validation**: Jakarta Validation for input validation
- **Error Handling**: Global exception handler

### 2. Domain Layer
- **Models**: Transfer, Policy, AuditEvent domain models
- **Services**: Core business logic
  - TransferOrchestrator
  - PolicyEvaluationService
  - AuditService

### 3. Infrastructure Layer
- **EDC Integration**: Client for EDC Management API
- **Persistence**: JPA entities and repositories
- **Policy Engine**: Pluggable policy evaluators
- **Configuration**: Spring configuration classes

## Sequence Diagram: Complete Transfer Flow

```
Client          API          Orchestrator     PolicyEngine     AuditService     EDC Client      Database
  │              │               │                 │               │               │               │
  │─────────────►│               │                 │               │               │               │
  │  POST        │               │                 │               │               │               │
  │  /transfers  │               │                 │               │               │               │
  │              │──────────────►│                 │               │               │               │
  │              │  initiate     │                 │               │               │               │
  │              │  Transfer     │                 │               │               │               │
  │              │               │────────────────────────────────────────────────►│               │
  │              │               │                 │               │ Save REQUESTED│               │
  │              │               │────────────────────────────────►│               │               │
  │              │               │                 │  Log Request  │               │               │
  │              │               │────────────────►│               │               │               │
  │              │               │  evaluateAll()  │               │               │               │
  │              │               │                 │               │               │               │
  │              │               │                 │──────────────►│               │               │
  │              │               │                 │  Check Rate   │               │               │
  │              │               │                 │  Limit        │               │               │
  │              │               │◄────────────────│               │               │               │
  │              │               │  Policy Result  │               │               │               │
  │              │               │────────────────────────────────►│               │               │
  │              │               │                 │  Log Eval     │               │               │
  │              │               │                 │               │               │               │
  │              │               │────────────────────────────────────────────────►│               │
  │              │               │                 │               │  Update State │               │
  │              │               │                 │               │  APPROVED     │               │
  │              │◄──────────────│                 │               │               │               │
  │◄─────────────│               │                 │               │               │               │
  │  202 Accepted│               │                 │               │               │               │
  │              │               │                 │               │               │               │
  │              │               │────────────────────────────────────────────────►│               │
  │              │               │  [ASYNC]        │               │  Negotiate    │               │
  │              │               │                 │               │  Contract     │               │
  │              │               │                 │               │◄──────────────┤               │
  │              │               │                 │               │  Agreement    │               │
  │              │               │────────────────────────────────────────────────►│               │
  │              │               │                 │               │  Initiate     │               │
  │              │               │                 │               │  Transfer     │               │
  │              │               │                 │               │◄──────────────┤               │
  │              │               │                 │               │  Process ID   │               │
  │              │               │────────────────────────────────────────────────►│               │
  │              │               │                 │               │  Save         │               │
  │              │               │                 │               │  COMPLETED    │               │
  │              │               │────────────────────────────────►│               │               │
  │              │               │                 │  Log Complete │               │               │
```

## Data Flow Diagram

```
┌──────────────┐
│  Client API  │
│   Request    │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────────┐
│         Input Validation Layer               │
│  (Jakarta Validation - @NotBlank, etc.)      │
└──────┬───────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────┐
│         Policy Evaluation Point              │
│  ├─ Time-based Policy                        │
│  ├─ Rate Limit Policy                        │
│  ├─ Geographic Policy                        │
│  ├─ Certification Policy                     │
│  └─ Usage Policy                             │
└──────┬───────────────────────────────────────┘
       │
       ├─────► APPROVED ───────┐
       │                       │
       └─────► DENIED          │
                (Return 400)   │
                               ▼
                    ┌───────────────────────────┐
                    │  Contract Negotiation     │
                    │  (EDC API Call)           │
                    └───────────┬───────────────┘
                                │
                                ▼
                    ┌───────────────────────────┐
                    │  Transfer Initiation      │
                    │  (EDC API Call)           │
                    └───────────┬───────────────┘
                                │
                                ▼
                    ┌───────────────────────────┐
                    │  Transfer Monitoring      │
                    │  (Poll EDC State)         │
                    └───────────┬───────────────┘
                                │
                ┌───────────────┴───────────────┐
                ▼                               ▼
        ┌───────────────┐             ┌─────────────────┐
        │   COMPLETED   │             │     FAILED      │
        │   (Success)   │             │  (Retry Logic)  │
        └───────────────┘             └─────────────────┘
                                              │
                                              ▼
                                    ┌──────────────────────┐
                                    │  Exponential Backoff │
                                    │  Max 3 Retries       │
                                    └──────────────────────┘
```

## State Machine

Transfer lifecycle states:

```
    START
      │
      ▼
  REQUESTED ──────────► POLICY_EVALUATION
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
                APPROVED             DENIED
                    │                (END)
                    ▼
          CONTRACT_NEGOTIATION
                    │
            ┌───────┴────────┐
            ▼                ▼
        NEGOTIATED        FAILED
            │                │
            ▼                │
    TRANSFER_IN_PROGRESS     │
            │                │
    ┌───────┼────────┐       │
    ▼       ▼        ▼       ▼
COMPLETED  FAILED  CANCELLED
  (END)    (Retry)  (END)
```

## Database Schema

```sql
┌─────────────────────────────────────────┐
│              TRANSFERS                  │
├─────────────────────────────────────────┤
│ id (PK)                    VARCHAR(255) │
│ consumer_id                VARCHAR(255) │
│ provider_id                VARCHAR(255) │
│ asset_id                   VARCHAR(255) │
│ current_state              VARCHAR(50)  │
│ message                    VARCHAR(1000)│
│ created_at                 TIMESTAMP    │
│ last_updated               TIMESTAMP    │
│ retry_count                INT          │
│ edc_transfer_process_id    VARCHAR(255) │
│ edc_contract_agreement_id  VARCHAR(255) │
│ consumer_region            VARCHAR(100) │
│ consumer_certification_level VARCHAR(100)│
│ usage_purpose              VARCHAR(100) │
└─────────────────────────────────────────┘
              │
              │ 1:N
              ▼
┌─────────────────────────────────────────┐
│            AUDIT_EVENTS                 │
├─────────────────────────────────────────┤
│ id (PK)                    VARCHAR(255) │
│ transfer_id (FK)           VARCHAR(255) │
│ event_type                 VARCHAR(100) │
│ actor                      VARCHAR(255) │
│ action                     VARCHAR(255) │
│ timestamp                  TIMESTAMP    │
│ details                    VARCHAR(2000)│
│ metadata                   VARCHAR(5000)│
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│              POLICIES                   │
├─────────────────────────────────────────┤
│ id (PK)                    VARCHAR(255) │
│ name                       VARCHAR(255) │
│ type                       VARCHAR(50)  │
│ description                VARCHAR(1000)│
│ configuration              VARCHAR(2000)│
│ active                     BOOLEAN      │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│        RATE_LIMIT_TRACKING              │
├─────────────────────────────────────────┤
│ id (PK)                    VARCHAR(255) │
│ consumer_id                VARCHAR(255) │
│ window_start               TIMESTAMP    │
│ request_count              INT          │
│ last_request               TIMESTAMP    │
└─────────────────────────────────────────┘
```

## Scalability Considerations

### Current Implementation (Single Instance)
- In-memory async processing with ThreadPoolTaskExecutor
- Database persistence with JPA
- Rate limiting tracked in database

### Scaling to 10,000 Concurrent Transfers

1. **Horizontal Scaling**
   - Deploy multiple instances behind load balancer
   - Use distributed session management
   - External message queue (Kafka/RabbitMQ) for async workflows

2. **Database Optimization**
   - Migrate to PostgreSQL with connection pooling
   - Read replicas for query operations
   - Partitioning for audit_events table by date
   - Indexing strategy for common queries

3. **Caching Layer**
   - Redis for policy definitions
   - Cache rate limit counters with TTL
   - Distributed cache for transfer states

4. **Async Processing**
   - Replace in-memory executor with message queue
   - Separate worker processes for transfer workflows
   - Dead letter queue for failed transfers

5. **Observability**
   - Distributed tracing with OpenTelemetry
   - Metrics collection with Prometheus
   - Log aggregation with ELK stack

## Security Considerations

1. **Authentication & Authorization**
   - OAuth2/OpenID Connect
   - JWT tokens for API access
   - Role-based access control (RBAC)

2. **Data Protection**
   - Encryption at rest for sensitive data
   - TLS for all communications
   - Audit log integrity verification

3. **Rate Limiting**
   - API rate limiting per client
   - DDoS protection
   - Circuit breaker for EDC calls

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Load Balancer                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼──────┐ ┌───▼───────┐
│ Orchestrator │ │Orchestrator│ │Orchestrator│
│  Instance 1  │ │Instance 2  │ │Instance 3 │
└───────┬──────┘ └────┬───────┘ └───┬───────┘
        │             │             │
        └─────────────┼─────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼──────┐ ┌───▼───────┐
│  PostgreSQL  │ │   Redis   │ │   Kafka   │
│   Primary    │ │   Cache   │ │   Queue   │
└───────┬──────┘ └───────────┘ └───────────┘
        │
┌───────▼──────┐
│  PostgreSQL  │
│   Replica    │
└──────────────┘
```

## Technology Choices Justification

### Spring Boot 4.0.0
- Latest stable version with modern features
- Excellent ecosystem and community support
- Built-in observability with Micrometer
- Easy integration with various technologies

### H2 Database (Development) / PostgreSQL (Production)
- H2: Zero configuration, perfect for demos and tests
- PostgreSQL: Production-grade, excellent JSON support, mature
- Easy migration path from H2 to PostgreSQL

### Flyway
- Database version control
- Repeatable migrations
- Team collaboration on schema changes

### JPA/Hibernate
- Standard ORM solution
- Reduces boilerplate code
- Good performance with proper configuration

### Async Processing. Used virtual thread
- Simple implementation for MVP
- Can be replaced with message queue for production scale

### Mock EDC Client
- Allows testing without actual EDC infrastructure
- Clear interface for real implementation
- Production-ready API design

## Future Architecture Enhancements

1. **Event-Driven Architecture**
   - Kafka for transfer state changes
   - Event sourcing for complete audit trail
   - CQRS pattern for read/write separation

2. **Microservices Split**
   - Policy Service
   - Transfer Service
   - Audit Service
   - Analytics Service

3. **Advanced Monitoring**
   - Custom Grafana dashboards
   - Alert rules for SLA violations
   - Performance monitoring with APM tools

