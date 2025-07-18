# Balance Streaming System
**High-Throughput, Low-Latency Transaction Engine**
 A latency-sensitive Java service that simulates streaming credit/debit events, maintains accurate balance state, and submits cost-constrained audit batches for external review. Designed with microservice-ready modularity and performance-aware architecture.
 This system is designed to be modular and testable, with clear pathways to scale: transaction ingestion can later be offloaded to a durable event broker (e.g. Kafka), and batching can be distributed via consumer groups without redesigning core interfaces.

 ---

## Problem Summary

Design an application that:

- Ingests credit/debit transactions at 50 TPS (25 credits + 25 debits).  
- Tracks a single account’s balance in real time, exposed via REST.  
- Every 1,000 transactions, forms batches whose absolute sum ≤ £1,000,000.  
- Minimizes the number of batches per submission to reduce costs.  
- Exposes rich metrics and supports performance validation.

---


## Evolution Strategy

This implementation reflects a modular, production-aware design for tracking bank account balances and audit batching. Key enhancements include:

- **Bin-Packing Audit Optimization**: Uses First Fit Decreasing to minimize batch count under £1M constraints.
- **Configurable Audit Trigger**: Submission threshold is externally configurable via `AuditConfig`, enabling performance tuning.
- **Observability Hooks**: `MetricsCollector` timestamps ingestion, balance updates, and audit submissions for latency profiling.
- **Thread Safety**: `processTransaction` is synchronized to ensure atomic updates under concurrent load.
- **DDD Alignment**: Domain logic is encapsulated in `BankAccountService`, with implementation in the application layer.
- **Scalability Readiness**: Batching logic is isolated and ready for evolution toward event-driven or distributed durability.

### Future Enhancements

- Introduce Prometheus-compatible metrics and MDC-based structured logging.
- Evolve batching toward event sourcing or Kafka-based durability.
- Simulate high-volume ingestion (e.g. 100k transactions) and benchmark batch performance.
- Add REST endpoints for audit traceability and batch introspection.

This README is designed to guide reviewers through architectural decisions, trade-offs, and future evolution paths.

## Architectural Evolution & Future-Proofing Strategy

This system is designed with modularity, durability, and event-driven evolution in mind. Key architectural decisions include:

- **Queue-Based Ingestion**: Transactions are ingested via `LinkedTransferQueue`, decoupling producers from consumers. This abstraction allows seamless evolution to Kafka or other event brokers.

- **Pluggable Batching Strategy**: Audit grouping is delegated to a `BatchingAlgorithm` interface, enabling bin-packing optimization and future experimentation with time-windowed or cost-aware strategies.

- **Persistence Isolation**: Audit durability is handled by `AuditBatchPersistence`, a domain port that can evolve toward database, file, or event-sourced backends.

- **External Submission via Notifier**: `AuditNotifier` encapsulates integration with external audit systems. It also handles observability concerns such as metrics and structured logging.

- **Observability Decoupling**: Metrics are emitted from the notifier, not the core service, ensuring clean separation of concerns and future compatibility with MDC, Prometheus, or OpenTelemetry.

### Kafka-Ready Design

- `LinkedTransferQueue` can be replaced with a Kafka consumer loop.
- `Transaction` events can be serialized and published to a topic.
- `AuditProcessingService` becomes a consumer group member, enabling horizontal scaling.
- `AuditNotifier` can evolve into a Kafka producer or sink connector.

### Microservice Evolution

Each domain (`Banking`, `Audit`, `Observability`) is isolated and package-aligned for bounded context extraction. This enables:

- Independent deployment
- Domain-specific scaling
- Event-driven choreography

This README is intended to guide reviewers through the architectural decisions, trade-offs, and future evolution paths of the system.
