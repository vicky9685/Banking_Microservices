# Banking Microservices Platform

Java 21 + Spring Boot 3.3 banking platform with Saga, Camunda BPM, Drools, Kafka, Solace, Vault, Redis, PostgreSQL, Helm, and a React UI.

## Services

| Service | Port | Stack | Role |
|---|---|---|---|
| `config-server` | 8888 | Spring Cloud Config | Centralized config (native classpath) |
| `discovery-server` | 8761 | Eureka | Service registry |
| `api-gateway` | 8080 | Spring Cloud Gateway | JWT auth, rate limit, circuit breaker |
| `auth-service` | 8085 | Boot + JPA + jjwt | User registration, JWT issuance |
| `customer-service` | 8081 | Boot + JPA + Redis | KYC, profile, daily expiry job |
| `account-service` | 8082 | Boot + JPA + Redis + Feign | Accounts, balance, dormancy job |
| `transaction-service` | 8083 | Boot + JPA + Kafka + Solace + Drools | Saga, outbox, fraud detection, stuck-saga job |
| `notification-service` | 8086 | Boot + Kafka + Solace | Event-driven notifications |
| `workflow-service` | 8087 | Boot + Camunda 7 CE + JPA | BPMN/DMN engine for loan apps + high-value approvals |
| `ui` | 3000 | React 18 + Vite + Tailwind | Responsive web UI |

## What's in the box

- **Saga (orchestration)** for cross-service transfers with explicit compensation
- **Camunda BPMN** for long-running, human-in-the-loop processes (loan applications, high-value approvals); **DMN** decision tables for credit scoring
- **Transactional Outbox** + Kafka relay (at-least-once)
- **Drools** rules engine for fraud detection
- **Solace** real-time fraud alerts (low-latency, guaranteed)
- **HashiCorp Vault** for DB password, JWT key, Solace creds — K8s ServiceAccount auth
- **Redis caching** with per-cache TTLs and explicit eviction
- **Scheduled jobs** for KYC expiry, dormant accounts, stuck-saga detection
- **JPA/Hibernate** aggregates with `@Version` optimistic locking + pessimistic `FOR UPDATE` for hot rows
- **Helm umbrella chart** with values-driven service iteration, NetworkPolicies, PSS, ResourceQuota, PDB, HPA, ServiceMonitor
- **kind** local cluster bootstrap + **docker-compose** parity
- **React UI** (responsive, mobile-first) wired to the gateway

## Documentation

- [`docs/architecture.md`](docs/architecture.md) — services, saga flow, failure modes
- [`docs/security-and-governance.md`](docs/security-and-governance.md) — controls, STRIDE, runbook
- [`docs/messaging-strategy.md`](docs/messaging-strategy.md) — Kafka vs. Solace decision
- [`docs/camunda-integration.md`](docs/camunda-integration.md) — when BPMN beats the saga, scenarios, ops
- [`docs/feature-flags-and-camunda-security.md`](docs/feature-flags-and-camunda-security.md) — Kafka/Solace/scheduler toggles, account-closure + dispute-resolution processes, Camunda role security, local light mode
- [`docs/runtime-additions.md`](docs/runtime-additions.md) — Drools, schedulers, caching, UI
- [`docs/local-setup.md`](docs/local-setup.md) — docker-compose and kind paths
- [`deploy/cloud-run-notes.md`](deploy/cloud-run-notes.md) — GCP free tier specifics

## Quick start

```bash
# Path A: docker-compose (fastest)
make compose-up
# UI: http://localhost:3000

# Path B: kind + Helm
make kind-up
```

## Layout

```
banking-microservices/
├── pom.xml                       # parent BOM (Spring Boot, Cloud, Solace, Drools)
├── common-lib/                   # DTOs, exceptions, JWT, events, cache config, Solace event types
├── config-server/                # native config with per-service yml
├── discovery-server/
├── api-gateway/
├── auth-service/                 # + Vault
├── customer-service/             # + JPA + Redis cache + KYC scheduler
├── account-service/              # + JPA + Redis cache + dormancy scheduler + Saga participant
├── transaction-service/          # + Saga orchestrator + Outbox + Drools + Solace + StuckSaga scheduler + workflow callback
├── notification-service/         # + Kafka + Solace consumers
├── workflow-service/             # + Camunda 7 CE engine + BPMN processes + DMN tables
├── ui/                           # React 18 + Vite + Tailwind
├── helm/banking-platform/        # umbrella chart + values-{local,gcp}.yaml
├── local-setup/                  # kind cluster + bootstrap.sh
├── deploy/                       # docker-compose, GCP deploy script
├── docs/                         # architecture, security, messaging, runtime
├── Dockerfile                    # multi-stage service build
├── docker-compose.yml            # full stack incl. Vault + Solace + UI
└── Makefile                      # one-liners
```
