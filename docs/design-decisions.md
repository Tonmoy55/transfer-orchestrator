# Design Decisions

This document explains key design decisions made during the implementation of the Transfer Orchestrator system.

## 1. Policy Engine Design

### Decision: Pluggable Policy Evaluator Pattern

**What**: Implemented a strategy pattern with individual policy evaluators implementing a common `PolicyEvaluator` interface.

**Why**:
- **Extensibility**: New policy types can be added without modifying existing code
- **Single Responsibility**: Each evaluator handles one policy type
- **Testability**: Each policy can be tested in isolation
- **Composability**: Policies can be combined with AND/OR logic

**Alternatives Considered**:
- **Rule Engine (Drools)**: Too heavyweight for current requirements
- **Hardcoded Logic**: Not extensible, violates Open/Closed Principle
- **Database-Driven Rules**: Adds complexity, harder to debug

**Trade-offs**:
- ✅ Clean, maintainable code
- ✅ Easy to add new policy types
- ❌ Requires code deployment for new policy types (vs. configuration-based)

### Decision: AND Logic for Policy Composition

**What**: All policies must pass for a transfer to be approved.

**Why**:
- **Security First**: Conservative approach for data sovereignty
- **Simplicity**: Easy to understand and implement
- **Compliance**: Ensures no policy violations slip through

**Future Enhancement**: Support for complex policy expressions (AND, OR, NOT) with a policy DSL.

## 2. State Management

### Decision: Database-Backed State with JPA

**What**: Transfer state is persisted in database using JPA entities.

**Why**:
- **Durability**: Survives application restarts
- **Consistency**: ACID transactions ensure data integrity
- **Queryability**: Easy to query and report on transfer states
- **Standard**: Well-known pattern in enterprise Java

**Alternatives Considered**:
- **In-Memory Only**: Fast but not durable
- **Event Sourcing**: More complex, overkill for MVP
- **External State Store (Redis)**: Adds infrastructure complexity

**Trade-offs**:
- ✅ Simple implementation
- ✅ Reliable and proven
- ❌ Database can become bottleneck at extreme scale
- ❌ Requires careful transaction management

### Decision: State Machine Pattern

**What**: Transfer follows a defined state machine with explicit transitions.

**Why**:
- **Clarity**: Clear understanding of transfer lifecycle
- **Validation**: Prevents invalid state transitions
- **Audit Trail**: Every transition is logged
- **Debugging**: Easy to understand where a transfer is stuck

**States**:
```
REQUESTED → POLICY_EVALUATION → APPROVED/DENIED → 
CONTRACT_NEGOTIATION → NEGOTIATED → TRANSFER_IN_PROGRESS → 
COMPLETED/FAILED/CANCELLED
```

## 3. Failure Handling

### Decision: Exponential Backoff with Max Retries

**What**: Failed transfers are retried up to 3 times with exponential backoff (1s, 2s, 4s).

**Why**:
- **Resilience**: Handles transient failures
- **Back Pressure**: Prevents overwhelming downstream services
- **Industry Standard**: Proven pattern for distributed systems

**Formula**: `backoff = initialDelay * 2^retryCount`

**Alternatives Considered**:
- **Immediate Retry**: Can overwhelm services
- **Fixed Delay**: Doesn't adapt to congestion
- **Infinite Retries**: Can tie up resources indefinitely

**Trade-offs**:
- ✅ Good balance between resilience and resource usage
- ✅ Configurable parameters
- ❌ Max 3 retries might not be enough for some scenarios

### Decision: No Circuit Breaker (MVP)

**What**: Direct calls to EDC without circuit breaker.

**Why**:
- **Simplicity**: Reduces complexity for MVP
- **Mock EDC**: Current mock doesn't fail

**Future Enhancement**: Add Resilience4j circuit breaker for production.

## 4. Scalability Design

### Decision: Async Processing with Spring @Async

**What**: Transfer workflows execute asynchronously using Spring's `@Async` with ThreadPoolTaskExecutor.

**Why**:
- **Responsiveness**: API returns immediately (202 Accepted)
- **Non-Blocking**: Doesn't tie up request threads
- **Simple**: Built into Spring, no external dependencies

**Configuration**:
- Core Pool: 5 threads
- Max Pool: 10 threads
- Queue: 100 tasks

**Alternatives Considered**:
- **Synchronous**: Blocks API requests, poor UX
- **Message Queue (Kafka)**: More robust but adds infrastructure
- **Reactive (WebFlux)**: Different programming model, more complex

**Trade-offs**:
- ✅ Simple to implement and understand
- ✅ Good for moderate load
- ❌ Limited scalability (single JVM)
- ❌ No distributed coordination

**Scaling Path**: Replace with Kafka for production scale.

### Decision: Single Database for All Concerns

**What**: One database stores transfers, audit logs, policies, and rate limits.

**Why**:
- **Simplicity**: Easy to set up and manage
- **Transactions**: Can use ACID transactions across entities
- **Development Speed**: Faster to implement

**Future Optimization**: Consider separate stores:
- PostgreSQL for transfers and policies
- TimescaleDB for audit logs (time-series data)
- Redis for rate limiting (in-memory counters)

## 5. Observability

### Decision: SLF4J/Logback with Structured Logging

**What**: Standard Java logging with meaningful log levels and context.

**Log Levels**:
- **ERROR**: System failures, exceptions
- **WARN**: Policy violations, retry attempts
- **INFO**: Transfer lifecycle events
- **DEBUG**: Detailed execution flow

**Why**:
- **Standard**: Works with all Java logging frameworks
- **Flexible**: Easy to configure and route logs
- **Integration**: Works with ELK, Splunk, CloudWatch

**Future Enhancement**: Add structured logging with JSON format for log aggregation.

### Decision: OpenAPI Documentation with SpringDoc

**What**: Automatic API documentation from code annotations.

**Why**:
- **Always Up-to-Date**: Generated from code
- **Interactive**: Swagger UI for testing
- **Standard**: OpenAPI 3.0 specification

## 6. Data Sovereignty & Compliance

### Decision: Immutable Audit Logs

**What**: Audit events are append-only with no updates or deletes.

**Why**:
- **Compliance**: Regulatory requirements (GDPR, GAIA-X)
- **Integrity**: Cannot be tampered with
- **Forensics**: Complete history for investigations

**Implementation**:
- No update/delete operations in repository
- Separate transaction for audit logging (`REQUIRES_NEW`)
- Timestamp and actor always recorded

### Decision: Policy Evaluation Before EDC

**What**: Policies are evaluated before initiating any EDC operations.

**Why**:
- **Efficiency**: No wasted EDC resources for denied transfers
- **Security**: Policies are enforced at orchestrator level
- **Audit**: Clear record of why transfers were denied

**Flow**: `Request → Policy Check → EDC Negotiation → Transfer`

## 7. EDC Integration

### Decision: Mock EDC Client with Production-Ready Interface

**What**: Created `EdcConnectorClient` interface with mock implementation.

**Why**:
- **Testability**: Can test without actual EDC
- **Development**: No EDC infrastructure needed for MVP
- **Abstraction**: Easy to swap with real implementation

**Interface Design**:
```java
- negotiateContract(ContractOffer)
- initiateTransfer(agreementId, request)
- getTransferState(transferProcessId)
- terminateTransfer(transferProcessId)
```

**Real Implementation Path**:
1. Add HTTP client (RestTemplate/WebClient)
2. Configure EDC Management API endpoints
3. Implement authentication (API keys/OAuth)
4. Add error handling and retries
5. Implement same interface

### Decision: RESTful HTTP for EDC Communication

**What**: Plan to use REST API for EDC communication.

**Why**:
- **Standard**: EDC Management API is REST-based
- **Simple**: Well-understood protocol
- **Tooling**: Excellent support and debugging tools

**Future Alternative**: Consider gRPC for better performance in high-throughput scenarios.

## 8. Testing Strategy

### Decision: Unit + Integration Tests with High Coverage

**What**: Comprehensive test suite targeting 75%+ coverage.

**Test Types**:
1. **Unit Tests**: Policy evaluators, business logic
2. **Integration Tests**: API endpoints, database operations
3. **Component Tests**: Service interactions

**Why**:
- **Confidence**: Catch regressions early
- **Documentation**: Tests serve as examples
- **Refactoring**: Safe to make changes

**Tools**:
- JUnit 5: Modern testing framework
- Spring Boot Test: Integration test support
- MockMvc: API testing without server
- H2: In-memory database for tests

### Decision: No Performance Tests (MVP)

**What**: Focused on functional correctness over performance testing.

**Why**: MVP priority, but noted for future work.

**Future Work**: Add JMeter/Gatling tests for:
- Concurrent transfer load
- Rate limit verification
- Database performance under load

## 9. Configuration Management

### Decision: application.yaml with Spring Profiles

**What**: YAML configuration with profile support.

**Profiles Planned**:
- `dev`: H2 database, verbose logging
- `test`: In-memory, fast startup
- `prod`: PostgreSQL, optimized settings

**Why**:
- **Environment Parity**: Same app, different configs
- **Spring Native**: Built-in support
- **Readable**: YAML is human-friendly

### Decision: No External Configuration Server (MVP)

**What**: Configuration bundled with application.

**Why**: Simpler for MVP, faster development.

**Future Enhancement**: Spring Cloud Config for:
- Centralized configuration
- Dynamic updates without restart
- Secrets management integration

## 10. Database Migration

### Decision: Flyway for Schema Management

**What**: Version-controlled SQL scripts with Flyway.

**Why**:
- **Version Control**: Schema in Git
- **Repeatable**: Consistent across environments
- **Team Collaboration**: No schema conflicts
- **Production Ready**: Safe migrations with rollback

**Alternative Considered**:
- **Liquibase**: More features but more complex
- **JPA auto-ddl**: Not suitable for production

## 11. Error Handling

### Decision: Global Exception Handler

**What**: `@RestControllerAdvice` for centralized error handling.

**Why**:
- **Consistency**: Uniform error response format
- **Separation**: Error handling logic separate from business logic
- **Client-Friendly**: Proper HTTP status codes and messages

**Error Response Format**:
```json
{
  "status": 400,
  "message": "Validation Error",
  "details": "Field errors...",
  "timestamp": "2025-12-12T10:30:00"
}
```

## 12. API Design

### Decision: RESTful API with Resource-Oriented URLs

**What**: Standard REST conventions with HTTP verbs.

**Endpoints**:
- `POST /api/v1/transfers` - Create
- `GET /api/v1/transfers/{id}` - Read
- `DELETE /api/v1/transfers/{id}` - Cancel
- `GET /api/v1/transfers` - List

**Why**:
- **Standard**: Industry best practice
- **Intuitive**: Easy for clients to understand
- **HTTP Semantics**: Proper use of methods and status codes

**Alternative Considered**: GraphQL - more flexible but adds complexity

### Decision: Pagination for List Endpoints

**What**: Spring Data pagination with page/size/sort parameters.

**Why**:
- **Performance**: Prevents large result sets
- **UX**: Better for UI pagination
- **Standard**: Spring Data Page interface

**Parameters**:
- `page`: Page number (0-based)
- `size`: Items per page
- `sortBy`: Field to sort by
- `sortDir`: ASC/DESC

## 13. Dependency Injection

### Decision: Constructor-Based Injection with Lombok

**What**: Required dependencies injected via constructor using `@RequiredArgsConstructor`.

**Why**:
- **Immutability**: Fields can be final
- **Testability**: Easy to create instances in tests
- **Null Safety**: No partially constructed objects
- **Lombok**: Reduces boilerplate

**Alternative Rejected**: Field injection (`@Autowired` on fields) - harder to test

## Key Takeaways

### What Went Well
✅ Clean separation of concerns
✅ Extensible policy engine
✅ Comprehensive audit trail
✅ Production-ready structure
✅ Good test coverage

### What Could Be Improved
🔄 Replace mock EDC with real integration
🔄 Add message queue for better scalability
🔄 Implement circuit breaker pattern
🔄 Add distributed tracing
🔄 Enhance rate limiting with Redis

### Design Principles Followed
- **SOLID Principles**: Single Responsibility, Open/Closed, Dependency Inversion
- **DRY**: Don't Repeat Yourself
- **KISS**: Keep It Simple, Stupid
- **YAGNI**: You Aren't Gonna Need It (avoided over-engineering)
- **Separation of Concerns**: Clean layered architecture

### Production Readiness Checklist
- ✅ Database migrations (Flyway)
- ✅ Error handling
- ✅ Input validation
- ✅ Audit logging
- ✅ API documentation
- ⚠️ Security (needs OAuth2)
- ⚠️ Monitoring (needs metrics)
- ⚠️ Rate limiting (needs Redis)
- ⚠️ Circuit breaker (needs Resilience4j)

## Time Spent Breakdown

- **Architecture & Design**: 1 hour
- **Core Implementation**: 3 hours
  - Policy Engine: 45 min
  - Transfer Orchestrator: 1 hour
  - EDC Integration: 30 min
  - API Layer: 45 min
- **Database & Persistence**: 45 min
- **Testing**: 1 hour
- **Documentation**: 30 min

**Total**: ~6 hours

## What I'd Do Differently With More Time

1. **Real EDC Integration**: Connect to actual Eclipse EDC connector
2. **Kafka Integration**: Event-driven architecture for better scalability
3. **Distributed Tracing**: OpenTelemetry for observability
4. **Metrics**: Prometheus metrics for monitoring
5. **Security**: OAuth2/JWT authentication
6. **Performance Tests**: Load testing with Gatling
7. **Advanced Policies**: Support for OR logic, time windows, custom expressions
8. **Caching**: Redis for policies and rate limits
9. **Health Checks**: Custom health indicators
10. **Docker Compose**: Containerization with dependencies

