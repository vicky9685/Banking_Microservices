# Security & Governance

Defense in depth across application, platform, and operations. Every control here ships in the Helm chart and the local kind setup so dev parity is maintained.

## Control matrix

| Domain | Control | Where |
|---|---|---|
| Secrets | HashiCorp Vault KV v2 backend; K8s auth method binds ServiceAccount → Vault role → policy → KV path | `helm/banking-platform/templates/vault-bootstrap.yaml` |
| Secrets | Spring Cloud Vault fetches DB password, JWT key, Solace creds at bootstrap; placeholders `${db.password}` etc. resolved per service | `*/src/main/resources/bootstrap.yml`, `config-server/.../configs/*.yml` |
| Secrets | No plaintext credentials anywhere in `values.yaml` or compose files (dev-only seed exception is explicitly tagged) | repo-wide |
| AuthN | JWT (HS256, jjwt 0.12) issued by `auth-service`, signing key from Vault | `auth-service`, `common-lib/security/JwtTokenProvider.java` |
| AuthN | Gateway validates token, injects `X-User-Id` / `X-User-Roles` headers; downstream services trust those headers (perimeter model) | `api-gateway/.../JwtAuthenticationFilter.java` |
| AuthZ | Vault policies are least-privilege: each service reads only `secret/banking/common` + `secret/banking/<svc>` | `vault-bootstrap` Job |
| AuthZ | K8s RBAC: namespace-scoped ServiceAccount per service, no cluster-wide roles for app pods | `helm/banking-platform/templates/services.yaml` |
| Network | NetworkPolicy default-deny, with explicit allows: app-to-app intra-namespace, app-to-{Vault, Postgres, Kafka, Solace, Redis} | `templates/namespace-policies.yaml`, `templates/services.yaml` |
| Pod hardening | `runAsNonRoot: true`, `runAsUser: 10001`, `readOnlyRootFilesystem: true`, `allowPrivilegeEscalation: false`, drop `ALL` capabilities, `seccompProfile: RuntimeDefault` | every Deployment |
| Pod hardening | Namespace labelled `pod-security.kubernetes.io/enforce=baseline`, `warn=restricted` | `local-setup/bootstrap.sh` |
| Data | Aggregate-enforced invariants (`Account.debit/credit`) plus DB `CHECK (balance >= 0)` constraint | `account-service` |
| Data | Optimistic locking via `@Version`; pessimistic `SELECT ... FOR UPDATE` for balance writes | JPA entities + `findByIdForUpdate` |
| Data | Idempotency-Key on transfer initiation prevents double-spend on retries | `TransferController` |
| Eventing | Transactional Outbox + relay → at-least-once Kafka delivery without business-state loss | `transaction-service/outbox` |
| Eventing | Solace topic ACLs (configurable on broker) + persistent delivery for fraud alerts | `SolaceConfig` |
| Auditing | All entities have `@CreatedDate` / `@LastModifiedDate`; saga state transitions go through outbox = append-only audit log | `domain/*` |
| Rate limiting | Per-user rate limiter at the gateway (Redis token bucket) | `api-gateway/.../RateLimiterConfig.java` |
| Resilience | Resilience4j circuit breakers + retries on Feign and gateway routes | `application.yml` shared config |
| Resilience | PodDisruptionBudgets (`minAvailable: 1`) for any deployment with replicas > 1 | `templates/services.yaml` |
| Governance | ResourceQuota + LimitRange at the namespace level so a runaway service can't starve neighbors | `templates/namespace-policies.yaml` |
| Governance | All images run as non-root + read-only FS; container image scanning via `make scan` (Trivy) | `Makefile` |
| Observability | Actuator + Micrometer + Prometheus on every service; ServiceMonitor when monitoring enabled | shared config + Helm |
| Observability | MDC `traceId/spanId` baked into log pattern; ready for OTLP exporter | shared `application.yml` |
| Crypto in transit | Ingress TLS via cert-manager when `security.tls.enabled=true`; mTLS in-mesh recommended via Istio/Linkerd (left as integration point) | `templates/ingress.yaml` |
| Crypto at rest | Postgres encryption is provider-managed (Cloud SQL or PVC encryption); Vault uses Raft + disk encryption when standalone | provider config |

## Threat model (STRIDE highlights)

| Threat | Mitigation |
|---|---|
| Spoofing — forged user identity | JWT signature verified at edge; signing key in Vault; short TTL (15 min) + refresh token |
| Tampering — modified API request | TLS to gateway; integrity preserved in request envelope; saga state changes go through DB transactions |
| Repudiation — denying a transfer was made | Outbox + Kafka stream is an append-only audit log; each state transition has a timestamped event |
| Information disclosure — credential leak | Vault, no plaintext secrets in repo; logs never include token contents; read-only FS prevents writing credentials to disk |
| Denial of service — flooding | Rate limit at gateway; circuit breakers; pod resource limits prevent noisy-neighbor; HPA absorbs bursts |
| Elevation of privilege — pod escape | runAsNonRoot, drop ALL caps, readOnlyRootFilesystem, NetworkPolicies restrict lateral movement |

## Operational runbook (selected)

**Rotate DB password**
```bash
# 1. Generate + write to Vault
vault kv put secret/banking/account-service db.password="$(openssl rand -base64 32)"
# 2. Restart consumers so Spring Cloud Vault picks the new value
kubectl rollout restart deploy/banking-account-service -n banking
```

**Inspect saga state**
```bash
kubectl exec deploy/banking-postgresql -- psql -U postgres transactiondb \
  -c "select id, status, failure_reason, updated_at from transfers order by updated_at desc limit 20;"
```

**Replay a stuck transfer**
```bash
# Outbox rows for the transfer
kubectl exec ... -c "select * from outbox where aggregate_id='<transferId>' and processed=false;"
# Force re-publish:
kubectl exec ... -c "update outbox set processed=false, attempts=0 where aggregate_id='<transferId>';"
```

**Vault unseal (standalone, not dev)**
```bash
kubectl exec banking-vault-0 -- vault operator unseal $UNSEAL_KEY_1
kubectl exec banking-vault-0 -- vault operator unseal $UNSEAL_KEY_2
kubectl exec banking-vault-0 -- vault operator unseal $UNSEAL_KEY_3
```

## Data classification

- **PII**: customer email, phone, national ID, DOB → encrypted at rest by Postgres; cached in Redis with `customers` namespace + 5 min TTL.
- **Restricted**: account balances → never cached more than 5 seconds.
- **Public**: account types, currency catalog → no cache restrictions.

## Compliance hooks left as integration points

- Fluent Bit sidecar for log shipping to a SIEM (toggle `governance.audit.enabled=true`).
- OPA Gatekeeper constraint templates to enforce "no `:latest`", "must define resources", "must run as non-root" — drop into `templates/policies/` per environment.
- Vault dynamic database credentials (rotate per-pod) — flip `spring-cloud-vault-config-databases` to use the `database/creds/<role>` path instead of static KV.
