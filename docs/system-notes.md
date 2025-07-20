# System Notes – Internal Architecture & Design Trade-Offs

This document details the internal mechanics, design rationale, concurrency model, durability concerns, and evolution strategy for the Balance Streaming System.

---

## 📷 Architecture Diagram

![Architecture Diagram](./docs/system-architecture.png)

## Domain Boundaries

| Domain     | Package Prefix                              | Notes                                              |
|------------|---------------------------------------------|----------------------------------------------------|
| `account`  | `com.arctiq.liquidity.balsys.account`       | Applies transactions and computes running balance  |
| `audit`    | `com.arctiq.liquidity.balsys.audit`         | Forms and persists audit batches via configurable rules |
| `producer` | `com.arctiq.liquidity.balsys.producer`      | Streams transactions (credit/debit) via lifecycle hooks |
| `shared`   | `com.arctiq.liquidity.balsys.shared`        | Cross-cutting domain models, validation, factories |
| `telemetry`| `com.arctiq.liquidity.balsys.telemetry`     | AuditStats, telemetry registry, event timeline     |

---


## 🔧 Concurrency Model

- Atomic balance via `AtomicReference<Double>`
- Queue with backpressure via `LinkedTransferQueue` or `LinkedBlockingQueue`
- History captured via `CopyOnWriteArrayList`
- No thread management inside domain logic (delegated to orchestration service)

---

## Principal Concerns Addressed

| Concern                   | Resolution |
|---------------------------|------------|
| **Consistency**           | Immutable batches via `List.copyOf`, atomic balance updates |
| **Durability**            | Persisted batches via `AuditBatchPersistence`; swappable with Aurora or S3 |
| **Ingestion Guarantees**  | Queue-based delivery with bounded capacity; future Kafka swap |
| **Traceability**          | `AuditStatsService` + REST endpoints + timestamped telemetry |
| **Reviewer Empathy**      | Clean layering, descriptive artifacts, modular naming |

---

## 4. Audit Batching

- Queue: `LinkedTransferQueue<Transaction>` held in `AuditProcessingService` (audit package)  
- Trigger: On each enqueue, if `queue.size() ≥ auditConfig.getBatchSize()` (1 000), start batch formation  
- Algorithm: `GreedyBatchingStrategy` (implements `BatchingAlgorithm`)
  - Sorts transactions by descending absolute value (`|amount|`)
  - Groups them into the smallest number of batches possible
  - Each batch total constrained to `config.getMaxBatchValue()` (e.g. £1,000,000) 
- Design Intent:
  - Optimized for packing efficiency (first-fit greedy)
  - Favors large transactions first to prevent early overflow
  - Modular—can be swapped via Spring config for alternate strategies
- Submission: `AuditSubmitter` persists each `AuditBatch` via `AuditBatchPersistence` then forwards to external audit via `AuditNotifier`  

---
- Generates structured `AuditBatch` objects with batchId, count, and total
---

## Observability

| Metric Point           | Recorded By                                      |
|------------------------|--------------------------------------------------|
| Transaction ingestion  | `MetricsCollector.recordTransaction`             |
| Balance mutation       | `MetricsCollector.recordBalanceMutation`         |
| Audit submission       | `MetricsCollector.recordAuditSubmission`         |
| Runtime telemetry      | `AuditStatsService.recordTelemetryEvent`         |
| Simulation status      | `SimulationManager.getLifecycleMetrics()`        |

> Observability exposed via:
> - `/simulation/status`
> - `/simulation/metrics`
> - `/simulation/telemetry`
> - Console logs with structured batch and ingestion events
```

---

## Cloud Integration Plan

| Concern           | Suggested AWS Service        |
|-------------------|------------------------------|
| Queue ingestion   | Amazon MSK (Kafka) or Kinesis|
| Persistence       | Amazon Aurora (Postgres)     |
| Batch durability  | Amazon S3                    |
| Audit submission  | AWS EventBridge              |
| API Gateway       | API Gateway + JWT + Lambda   |
| Observability     | CloudWatch, OpenTelemetry    |

---

## Producers

- Located in `com.arctiq.liquidity.balsys.producer`
- Emit ~50 transactions/sec (25 credit + 25 debit)
- Timestamped with randomized amounts
- Future: Replace with Kafka producer or external ingestion façade

---

## Migration Strategy

1. Swap internal queue with Kafka topic (low-effort)
2. Extract `account` and `audit` into standalone services
3. Add Kafka event schemas for `TransactionEvent` and `AuditSubmissionEvent`
4. Replace in-memory stores with Aurora or S3
5. Introduce OpenTelemetry + Prometheus dashboards

---

## Status

- ✅ Modular domains in place
- ✅ REST API exposed
- ✅ Metrics + telemetry wired
- ✅ Ready for microservice and cloud evolution
```

---