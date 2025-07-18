# Balance Streaming System
**High-Throughput, Low-Latency Transaction Engine**
 A latency-sensitive Java service that simulates streaming credit/debit events, maintains accurate balance state, and submits cost-constrained audit batches for external review. Designed with microservice-ready modularity and performance-aware architecture.

 ---

## Problem Summary

Design an application that:

- Ingests credit/debit transactions at 50 TPS (25 credits + 25 debits).  
- Tracks a single account’s balance in real time, exposed via REST.  
- Every 1,000 transactions, forms batches whose absolute sum ≤ £1,000,000.  
- Minimizes the number of batches per submission to reduce costs.  
- Exposes rich metrics and supports performance validation.

---