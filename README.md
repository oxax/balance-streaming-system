# Balance Streaming System
**High-Throughput, Low-Latency Transaction Engine**
A performance-aware Java system that simulates streaming credit/debit ingestion, balance computation, and audit batching — designed with modular domain boundaries, observability, and microservice evolution in mind.

![Architecture Diagram](./docs/system-architecture.png)

## Problem Summary

Design an application that:

- Ingests credit/debit transactions at 50 TPS (25 credits + 25 debits).  
- Tracks a single account’s balance in real time, exposed via REST.  
- Every 1,000 transactions, forms batches whose absolute sum ≤ £1,000,000.  
- Minimizes the number of batches per submission to reduce costs.  
- Exposes rich metrics and supports performance validation.

---
## Features

- Real-time credit/debit ingestion (~50 TPS simulated)
- Atomic balance mutation with telemetry hooks
- Bin-packed audit batches under £1M constraint
- Pluggable batching algorithm
- In-memory persistence layer (swappable)
- REST APIs for transaction, balance, audit stats, and telemetry

---

## 📦 Package Structure

| Boundary     | Package Prefix                             | Responsibility                                  |
|--------------|---------------------------------------------|--------------------------------------------------|
| `account`    | `com.arctiq.liquidity.balsys.account`       | Balance tracking and transaction application     |
| `audit`      | `com.arctiq.liquidity.balsys.audit`         | Audit batching, submission, metrics, and persistence |
| `producer`   | `com.arctiq.liquidity.balsys.producer`      | Simulates credit/debit transaction flow (~50 TPS) |
| `shared`     | `com.arctiq.liquidity.balsys.shared`        | DTOs, factories, domain types, and observability tools |
| `telemetry`  | `com.arctiq.liquidity.balsys.telemetry`     | Audit stats tracking and runtime event logging     |
| `exception`  | `com.arctiq.liquidity.balsys.exception`     | Domain-safe exception handling with global mappers |
| `transaction`| `com.arctiq.liquidity.balsys.transaction`   | Immutable domain model (`Transaction` record, validation) |

---

## 🧭 Architecture Overview

> For full design details and cloud integration strategy, see [docs/system-notes.md](./docs/system-notes.md)

```
SimulatedProducer
        ↓
BankAccountService → LinkedTransferQueue
        ↓
AuditProcessingService → BatchingAlgorithm (bin-pack)
        ↓
AuditBatchPersistence + AuditNotifier
        ↓
AuditStatsService → REST API
```

---
## REST Endpoints

| Endpoint                      | Purpose                                                                            |
|------------------------       |------------------------------------------------------------------------------------|
| `/account/balance`            | Fetch the current account balance                                                  |
| `/transactions/history`       | Retrieve transactions with optional range                                          |
| `/transactions/submit`        | Submit a new transaction                                                           |
| `/simulation/start`           | Start transaction simulation with custom count and duration                        |
| `/simulation/stop`            | Stop active transaction emitters                                                   |
| `/audit/batches`              | Retrieve recently persisted audit batches                                          |
| `/audit/stats`                | Fetch total audit batches and transactions processed                               |
| `/audit/telemetry`            | View audit telemetry events and lifecycle signals                                  |
| `/audit/summary`              | View consolidated audit and ingest metrics (TPS, latency, outcomes, batch stats)   |

---


## ☁️ Cloud-Ready Design

| Concern             | AWS Service Suggestion            |
|---------------------|-----------------------------------|
| Queue ingestion     | Kafka (MSK) or Kinesis            |
| Persistence         | Aurora or DynamoDB               |
| Batch durability    | Amazon S3 or PostgreSQL          |
| External audit flow | EventBridge or SNS               |
| Observability       | CloudWatch, X-Ray, QuickSight    |

---

## 🧠 Architectural Highlights

- Clean separation of domains → microservice-ready
- Lock-free concurrency and backpressure via bounded queue
- Configurable batching threshold
- Observability-first with structured metrics
- Ready for async event ingestion via Kafka or streaming gateway

---

## 🧪 To Run

```bash
./mvnw spring-boot:run
```

Producers will begin streaming. Use REST endpoints to inspect runtime behavior.

---

## 📄 License

MIT
```

---
