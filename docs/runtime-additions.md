# Drools, Schedulers, Caching, UI — at a glance

## Drools rules engine (transaction-service)

- Rules live in `transaction-service/src/main/resources/rules/fraud-rules.drl`.
- `KieConfig` compiles every `.drl` on the classpath into an in-memory `KieContainer`.
- `FraudDetector.evaluate(...)` spawns a stateful `KieSession`, inserts a `FraudContext` fact, fires all rules, collects alerts, disposes.
- Rule salience controls ordering; future agenda-groups can split AML vs. velocity vs. geographic rules.
- Alerts emit on Solace topic `banking/fraud/alerts/v1/{severity}/{customerId}` — subscribers slice by severity wildcard.

To add a rule: drop a new `.drl` next to `fraud-rules.drl` and restart the service. To make rules hot-reloadable, expose `KieFileSystem` as a bean and rebuild on Spring `ContextRefreshedEvent` or a config-server `RefreshEvent`.

## Scheduled jobs

| Service | Job | Cadence | Behaviour |
|---|---|---|---|
| customer-service | `KycExpiryJob` | daily 02:00 | flips `VERIFIED → EXPIRED` after `app.kyc.expiry-days` |
| account-service | `DormantAccountJob` | daily 02:30 | flips `ACTIVE → DORMANT` after `app.account.dormant-months` |
| transaction-service | `StuckSagaJob` | every 60s | warns on transfers stuck in `INITIATED/DEBITED` past SLA |
| transaction-service | `OutboxRelay` | every 1s | drains the outbox to Kafka |

For multi-replica safety, wrap any `@Scheduled` with [ShedLock](https://github.com/lukas-krecan/ShedLock) backed by Postgres or Redis; the locks integrate cleanly with the existing datasources.

## Caching (Redis-backed)

- Shared config in `common-lib/cache/BankingCacheConfig.java` — per-cache TTL map.
- `customer-service`: `customers` cache (5 min), eviction on update/KYC change.
- `account-service`: `accounts:by-id` (5s), `accounts:by-customer` (10s); writes evict both via `@Caching`.
- `transaction-service`: `transfers:by-id` (30s); `fraud:daily-stats` (1 min); `fraud:known-counterparty` (15 min).
- Keys prefixed with `bank::` for clean Redis namespacing.

Tuning rationale: balance/transfer reads stay near-realtime (short TTLs); profile-style data caches longer because changes are rare and writes invalidate explicitly.

## UI

Vite + React 18 + TailwindCSS + React Query.
- `/login`, `/register` — auth screens.
- `/` Dashboard — total balance, account count, recent accounts.
- `/accounts` — list + inline deposit/withdraw + new-account modal.
- `/transfer` — saga-initiating transfer form with idempotency key.
- `/customer` — onboard new customer or view profile + force KYC verify (back-office demo button).

Responsive design via Tailwind utility classes — single column on mobile, two-column grid above the `sm` breakpoint, sidebar nav above `md`.
