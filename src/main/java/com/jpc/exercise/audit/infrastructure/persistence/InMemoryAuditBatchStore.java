package com.jpc.exercise.audit.infrastructure.persistence;

import com.jpc.exercise.account.domain.model.Transaction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuditBatchStore implements AuditBatchPersistence {

    private static class StoredBatch {
        final List<Transaction> transactions;
        boolean submitted;

        StoredBatch(List<Transaction> txs) {
            this.transactions = txs;
            this.submitted = false;
        }
    }

    private final Map<String, StoredBatch> store = new ConcurrentHashMap<>();

    @Override
    public void save(String batchId, List<Transaction> transactions) {
        store.put(batchId, new StoredBatch(new ArrayList<>(transactions)));
    }

    @Override
    public Optional<List<Transaction>> load(String batchId) {
        StoredBatch batch = store.get(batchId);
        return batch == null ? Optional.empty() : Optional.of(batch.transactions);
    }

    @Override
    public void markSubmitted(String batchId) {
        StoredBatch batch = store.get(batchId);
        if (batch != null) batch.submitted = true;
    }

    @Override
    public List<String> findPendingBatchIds() {
        List<String> pending = new ArrayList<>();
        for (Map.Entry<String, StoredBatch> entry : store.entrySet()) {
            if (!entry.getValue().submitted) pending.add(entry.getKey());
        }
        return pending;
    }
}