# Messaging: Kafka + Solace Side-by-Side

We deliberately use two brokers because their strengths differ. Choosing one would force a compromise on either throughput/replay or latency/routing.

## Kafka (event streaming backbone)

Used for: **saga events, outbox relay, audit log, customer/account domain events**.

| Property | Why it matters here |
|---|---|
| Append-only partitioned log | Saga state transitions are an audit trail; replay during recovery is a first-class operation |
| Consumer groups + offset commits | New downstream services (analytics, regulatory reporting) can join later and replay history |
| At-least-once with idempotent producers | Pairs naturally with the Transactional Outbox to guarantee no event loss |
| High throughput | Domain event volume scales with customer count + transaction velocity |

Topics:
- `banking.transfer.commands` — orchestrator → participants
- `banking.transfer.events` — participants → orchestrator
- `banking.transfer.credit.commands` — dedicated credit topic (independent scaling)
- `banking.notifications` — completed transfers fan-out
- `banking.customer.events` — KYC and profile changes

## Solace (real-time messaging fabric)

Used for: **fraud alerts, real-time customer notifications, request/reply (future), hybrid-cloud routing (future)**.

| Property | Why it matters here |
|---|---|
| Low-latency push (typically sub-ms) | Fraud alerts must reach risk operators and SMS gateways immediately — every second of delay is reputational/financial risk |
| Hierarchical topics + wildcards (`banking/fraud/alerts/v1/CRITICAL/>`) | Subscribers slice by severity, geography, or customer without coordinating producer changes |
| Guaranteed messaging with per-message acks | Per-alert correctness, not log-replay correctness |
| Multi-protocol (JMS, MQTT, REST, AMQP) | Lets us reach SMS gateways, mobile push services, and partner banks without bridge code |
| Topic ACLs at broker level | Fits with the zero-trust model for high-sensitivity flows |

Topics:
- `banking/fraud/alerts/v1/{severity}/{customerId}` — fraud detector → multiple downstreams
- `banking/customer/alerts/v1/{customerId}/{channel}` — future, real-time customer push

## Decision shortcut

> If the consumer needs **history and replay**, use Kafka.
> If the consumer needs **the latest event now**, use Solace.

That single rule has driven every binding above and gives engineers a clear path when adding new event types.
