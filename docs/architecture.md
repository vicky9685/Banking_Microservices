# Architecture

## Logical diagram

```
                          ┌────────────┐
            Clients ─────▶│ API Gateway│─── JWT validate, rate limit, CB
                          └─────┬──────┘
                                │ lb://service-name
              ┌─────────────────┼─────────────────────┐
              ▼                 ▼                     ▼
     ┌────────────────┐  ┌────────────────┐  ┌────────────────────┐
     │ auth-service   │  │customer-service│  │ account-service    │
     │ (JWT issue)    │  │ (KYC, profile) │  │ (balance, locking) │
     └──────┬─────────┘  └──────┬─────────┘  └──────────┬─────────┘
            │                   │                       │
            │ Postgres          │ Postgres + Redis      │ Postgres
            ▼                   ▼                       ▼
        authdb              customerdb              accountdb
                                                       ▲
                                                       │ Saga
                                       ┌───────────────┴───────────────┐
                                       │      transaction-service       │
                                       │  (orchestrator + outbox relay) │
                                       └───────────────┬────────────────┘
                                                       │ Kafka
                                            ┌──────────┴──────────┐
                                            ▼                     ▼
                                   notification-service   account-service
```

## Why these patterns

### Saga (orchestration) over choreography
Banking transfers must be auditable end-to-end. A central orchestrator means a single state machine in one DB; choreography would scatter that state across services and make audit trails painful. The cost is coupling to one service for the flow definition, accepted here.

### Outbox + Kafka
We never publish to Kafka inside a JPA transaction directly — broker outages would either roll back business state or lose the event. The outbox writes to Postgres atomically with the domain change; a polling relay guarantees at-least-once delivery. Consumers stay idempotent via event IDs.

### Aggregate-enforced invariants
Balance rules live in `Account.debit()` and `Account.credit()`. Service code cannot accidentally bypass the non-negative-balance check, and the Postgres `CHECK (balance >= 0)` constraint is the last line of defense.

### Optimistic + pessimistic locking
`@Version` is sufficient for low-contention writes (KYC update). Hot rows (balance) use `SELECT ... FOR UPDATE` via `findByIdForUpdate` — short-lived, scoped to the service method.

### JWT at the edge, header trust inside
The gateway validates JWT once and injects `X-User-Id` / `X-User-Roles`. Internal services trust those headers (the gateway is the only ingress). This avoids signature verification on every hop and keeps tokens out of internal logs.

### Database per service
Each bounded context owns its schema. Cross-service joins are forbidden; reads go through APIs or projected views. Trade-off: distributed transactions become sagas (already accepted).

## Failure modes considered

| Failure | Behaviour |
|---|---|
| Customer service down during account open | Feign + Resilience4j fallback → `CUSTOMER_SERVICE_DOWN` |
| Kafka unavailable | Outbox accumulates rows; relay retries; no business impact until backlog grows |
| Debit succeeds, credit fails | Orchestrator transitions `DEBITED → COMPENSATING` and emits credit-back command |
| Duplicate transfer request | Idempotency-Key returns the existing transfer row |
| Concurrent balance mutation | `@Version` causes one txn to retry; pessimistic lock serializes |

## Observability stack (local & prod)

- `/actuator/health`, `/actuator/info`, `/actuator/prometheus` on every service.
- Micrometer adds HTTP histogram for latency percentiles.
- MDC `traceId/spanId` ready for Sleuth/OTLP exporter when added.
