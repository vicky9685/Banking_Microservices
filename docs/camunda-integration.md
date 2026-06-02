# Camunda BPM Integration

## When BPMN beats the saga

The platform uses three orchestration styles, each with a clear sweet spot:

| Pattern | Implemented by | Sweet spot |
|---|---|---|
| **Choreography (Kafka events)** | outbox + listeners | High-throughput, decoupled domain events with no single owner |
| **Saga (orchestration)** | `transaction-service` orchestrator | Short technical orchestrations measured in seconds — debit, credit, complete, compensate. No humans involved. |
| **Camunda BPMN** | `workflow-service` | Long-running business processes (minutes → weeks), human tasks, SLA timers, DMN-driven branching, visual diagrams stakeholders read |

The single-line decision rule:
> If a human or a clock is part of the happy path, use Camunda. If the path is purely machine-to-machine and bounded in seconds, the saga is enough.

## Scenarios shipped

### 1. High-value transfer approval (`high-value-approval.bpmn`)

Fires only when the Drools fraud detector emits a `CRITICAL` alert. The transfer is parked in status `AWAITING_APPROVAL` and a Camunda process instance starts. Flow:

```
Start → Lookup customer tier → User task: risk-officer approves
                                  │ SLA 30m → escalate to senior-risk-officers
        ┌────── approved? ──────┐
        ▼                       ▼
   Resume transfer       Cancel transfer
   (callback POST        (callback POST
    /transfers/{id}      /transfers/{id}
    /decision?           /decision?
    approved=true)       approved=false)
```

- Boundary timer with `PT30M` triggers an `escalationDelegate` that reassigns the task to a senior group instead of cancelling — the natural BPMN way to encode an SLA.
- All decisions are routed through the same `/decision` callback so the saga state machine remains the single source of truth for transfer status.

### 2. Loan application (`loan-application.bpmn` + `credit-decision.dmn`)

Multi-stage process with both automated and human steps:

```
Submit → Credit check (service task)
       → KYC check (service task)
       → Credit decision (DMN business rule task)
         ├── APPROVE → Open loan account & disburse → End: funded
         ├── REVIEW  → Underwriter user task
         │              ├── approve → disburse → End: funded
         │              └── reject  → End: rejected
         └── REJECT  → End: rejected
```

DMN table (`credit-decision.dmn`, FIRST hit policy):
- `kycStatus != "VERIFIED"` → REJECT
- `creditScore < 500` → REJECT
- `creditScore >= 750 AND amount <= 50000` → APPROVE
- otherwise → REVIEW (human underwriter)

Why DMN here:
- Business analysts can tune approval thresholds without touching code.
- The table is unit-testable in isolation and visualisable in Cockpit.
- Hit policies (FIRST, COLLECT, RULE ORDER) give us deterministic semantics.

## Architecture choices

- **Camunda 7 Community Edition** — embedded engine in Spring Boot. Free, OSS, no separate cluster to operate. Camunda 8 (Zeebe) is more scalable but adds a gateway, zeebe-broker, exporter, and Operate/Tasklist — overkill for a reference platform.
- **PostgreSQL backing store** (`workflowdb`) — Camunda manages its own schema (`schema-update: true`); separate DB keeps process state isolated from business data per the database-per-service rule.
- **`@EnableProcessApplication("workflow-service")`** — registers the deployment as a process application so Cockpit can correlate process instances back to this jar.
- **`history-level: full`** — every variable change and activity transition is persisted; satisfies audit/compliance.
- **Camunda webapps enabled** at `/camunda/` (Cockpit, Tasklist, Admin) but proxied through the API Gateway with the `/camunda/**` and `/engine-rest/**` paths excluded from gateway JWT (they have their own basic-auth + cookie session).
- **Vault-stored Camunda admin password** — bootstrapped to `secret/banking/workflow-service` so it never sits in `values.yaml`.

## Saga + Camunda interaction

```
TransferService.initiate()
   │
   ├── persist Transfer (INITIATED)
   ├── Drools fraud evaluation
   │      └── CRITICAL alert? ──── yes ──┐
   ├── no → publish TransferInitiated    │
   │        to Kafka, saga runs          │
   │                                     ▼
   │                            transfer → AWAITING_APPROVAL
   │                            workflow-service.startHighValueApproval()
   │                                     │
   │                                     ▼
   │                            Camunda runs process,
   │                            ends with /decision callback:
   │                              approved=true  → APPROVED → publish TransferInitiated
   │                              approved=false → FAILED  (terminal)
```

The saga doesn't need to know that humans were involved — it just sees a delayed `TransferInitiated`. Camunda doesn't need to know about Kafka or accounts — it just calls back via REST. Boundaries are clean.

## Operations

- **Cockpit**: `http://localhost:8087/camunda/app/cockpit/` (compose) — live process instances, incidents, variable history.
- **Tasklist**: `http://localhost:8087/camunda/app/tasklist/` — list of user tasks with auto-generated forms from the BPMN `<camunda:formData/>`.
- **REST**: `http://localhost:8087/engine-rest/` — the full engine API for external integration.
- **Process metrics**: scraped from `/actuator/prometheus` (engine job execution and history counters via the Camunda micrometer reporter).

To add a new process:
1. Drop a `.bpmn` (and optionally `.dmn`) into `workflow-service/src/main/resources/processes/`.
2. Add Java `@Component("xxxDelegate")` beans for each service task referenced as `${xxxDelegate}`.
3. Restart the service — `camunda.bpm.auto-deployment-enabled: true` redeploys on every startup, and Camunda diffs against the existing deployment.

## Why not embed Camunda in transaction-service?

Tempting, but it would tangle two life cycles: a BPMN deployment change shouldn't redeploy the transfer engine, and process state shouldn't share a DB with money-movement state. Keeping `workflow-service` separate preserves the database-per-service rule and lets the workflow team release independently.
