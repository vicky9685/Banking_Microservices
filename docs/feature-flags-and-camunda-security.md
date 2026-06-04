# Feature Flags, More Camunda Use Cases, Camunda Security, Local Modes

## 1. Feature flags

Four runtime toggles, all default ON. Each gates one or more beans via `@ConditionalOnProperty`. Flip via env var on the workload — no rebuild.

| Flag | Env var | Beans gated |
|---|---|---|
| `app.features.kafka.enabled` | `APP_FEATURES_KAFKA_ENABLED` | `TransferSagaOrchestrator`, `OutboxRelay`, `TransferSagaListener`, `CreditCommandListener`, `NotificationListener` |
| `app.features.solace.enabled` | `APP_FEATURES_SOLACE_ENABLED` | `SolaceConfig` (template), `FraudAlertPublisher`, `SolaceConsumerConfig`, `FraudAlertListener` |
| `app.features.scheduler.enabled` | `APP_FEATURES_SCHEDULER_ENABLED` | `KycExpiryJob`, `DormantAccountJob`, `StuckSagaJob` (and consequently the relay if Kafka flag is also off) |
| `app.features.camunda.enabled` | `APP_FEATURES_CAMUNDA_ENABLED` | `CamundaIdentityBootstrap` (engine itself stays up so REST keeps working) |

`TransferService` injects `ObjectProvider<FraudAlertPublisher>` and no-ops when the bean is absent — the saga continues to run with Solace off.

Defaults live in `config-server/configs/application.yml`:

```yaml
app:
  features:
    kafka:     { enabled: ${APP_FEATURES_KAFKA_ENABLED:true} }
    solace:    { enabled: ${APP_FEATURES_SOLACE_ENABLED:true} }
    scheduler: { enabled: ${APP_FEATURES_SCHEDULER_ENABLED:true} }
    camunda:   { enabled: ${APP_FEATURES_CAMUNDA_ENABLED:true} }
```

## 2. New Camunda banking use cases

### Account closure (`account-closure.bpmn`)
Regulator-friendly closure with cooling-off:

```
Start → Freeze account → Check residual balance
  ├── balance > 0 → User task: payout target → Disburse residual ─┐
  └── balance = 0 ─────────────────────────────────────────────────┤
                                                                   ▼
                            Cooling-off timer (P7D)
                              │ ─ Message: CancelClosure → Unfreeze → End: cancelled
                              ▼
                       Mark account CLOSED → End: closed
```

Highlights:
- Boundary message event (`CancelClosure`) correlates by business key `close-{accountId}` so customers can change their mind during the cooling-off window — a banking compliance staple.
- All money-moving steps go through `account-service` via Feign — no direct DB writes from the workflow.
- Cooling-off duration is a process variable (`P7D` default), so the same BPMN works for retail (7 days) and test scenarios (`PT1M`).

### Dispute resolution (`dispute-resolution.bpmn`)
Customer files a dispute over a transaction:

```
Start → Open case → Analyst investigation (5-day SLA boundary timer)
  ├── REFUND   → Issue refund → End: resolved
  ├── REJECT   → Close case → End: resolved
  └── ESCALATE → Legal review → End: resolved

SLA timer (P5D, non-interrupting) → Notify manager
```

Highlights:
- Non-interrupting timer keeps the analyst task active but emits an audit/notification event when SLA is breached — exactly what compliance teams ask for.
- Enum form field on the analyst task means the UI auto-generates radio buttons (REFUND/REJECT/ESCALATE).

### Existing processes recap
- `high-value-approval.bpmn` — fires when Drools raises CRITICAL; paused saga resumes via REST callback.
- `loan-application.bpmn` — DMN-driven credit decision; auto-approve, REVIEW (underwriter task), or auto-reject; disbursement opens a LOAN account.

## 3. Camunda security

### Engine-level
- `camunda.bpm.authorization.enabled: true` so the engine enforces ACLs on every operation.
- Admin user + password sourced from Vault (`secret/banking/workflow-service`).
- Camunda webapps (Cockpit/Tasklist) are reachable through the gateway but excluded from JWT validation; they use Camunda's own basic-auth + cookie session.

### Group bootstrap (`CamundaIdentityBootstrap`)
Creates the six groups referenced in BPMN candidate-group attributes (`customers`, `risk-officers`, `senior-risk-officers`, `underwriters`, `dispute-analysts`, `legal`) the first time the service starts. Grants each group **READ + UPDATE + TASK_WORK on TASK** and **READ + READ_INSTANCE on PROCESS_DEFINITION**, nothing more. No global ADMIN grants for app groups.

### JWT-to-group bridge (`JwtRoleGroupFilter`)
Servlet filter on `/api/workflows/**` and `/engine-rest/**`. Reads the gateway-injected `X-User-Id` and `X-User-Roles` headers, maps each role to a Camunda group, and calls `IdentityService.setAuthentication(userId, groups)` for the request. The engine then naturally:
- filters `taskCandidateGroupIn(...)` queries so users only see tasks for their groups,
- applies the engine ACLs to every action,
- writes user-attributed history entries (auditable).

Role → group mapping:

| JWT role | Camunda group |
|---|---|
| CUSTOMER | customers |
| RISK_OFFICER | risk-officers |
| SENIOR_RISK_OFFICER | senior-risk-officers |
| UNDERWRITER | underwriters |
| DISPUTE_ANALYST | dispute-analysts |
| LEGAL | legal |

### Controller-level
`WorkflowController#listMyTasks` only queries tasks for the caller's groups — anonymous calls receive an empty list rather than the global queue.

## 4. Local execution

Three local recipes; pick by laptop power and what you want to demo.

| Recipe | What runs | When to use |
|---|---|---|
| `make compose-up` | Everything (Postgres, Redis, Vault, Kafka, Solace, Camunda, services, UI) | Full demo, all flows |
| `make compose-up-light` | Postgres, Redis, Vault, Camunda, services, UI (env flags disable Kafka/Solace/schedulers) | UI dev, Camunda-only demo, smaller machines |
| `make deploy-local-light` | Same as light, on kind | K8s-shaped demos without Kafka/Solace footprint |

`values-local-light.yaml` carries the env-var overrides so the chart re-renders deployments without messaging dependencies — the chart sub-deps for Kafka and Solace are disabled entirely.

### Verifying flag effects
```bash
# Confirm a bean was skipped:
kubectl logs deploy/banking-transaction-service | grep -i "fraud-alert\|OutboxRelay"
# Should be silent in light mode, except a single "Solace disabled — n fraud alert(s) not published" warning when a transfer would have alerted.
```

### Starting a Camunda flow from curl
```bash
# Get a JWT (any role)
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"Sup3rSecret!"}' | jq -r .data.accessToken)

# Start account closure
curl -X POST localhost:8080/api/workflows/account-closures \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"accountId":"<UUID>","customerId":"<UUID>","coolingOffDuration":"PT1M"}'

# Cancel during cooling-off (within 1 minute)
curl -X POST localhost:8080/api/workflows/account-closures/<accountId>/cancel \
  -H "Authorization: Bearer $TOKEN"

# Open a dispute
curl -X POST localhost:8080/api/workflows/disputes \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"transferId":"<UUID>","customerId":"<UUID>","amount":250,"currency":"USD","reason":"Unauthorized"}'

# Inspect via Camunda Cockpit
open http://localhost:8087/camunda/app/cockpit/
```
