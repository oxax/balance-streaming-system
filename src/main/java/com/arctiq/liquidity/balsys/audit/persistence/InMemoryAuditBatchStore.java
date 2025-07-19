package com.arctiq.liquidity.balsys.audit.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

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
        if (batch != null)
            batch.submitted = true;
    }

    @Override
    public List<String> findPendingBatchIds() {
        List<String> pending = new ArrayList<>();
        for (Map.Entry<String, StoredBatch> entry : store.entrySet()) {
            if (!entry.getValue().submitted)
                pending.add(entry.getKey());
        }
        return pending;
    }

    @Override
    public List<AuditBatch> fetchAll() {
        List<AuditBatch> allBatches = new ArrayList<>();

        for (Map.Entry<String, StoredBatch> entry : store.entrySet()) {
            String batchId = entry.getKey();
            StoredBatch stored = entry.getValue();

            double totalAmount = stored.transactions.stream()
                    .mapToDouble(tx -> tx.amount().amount().doubleValue())
                    .sum();

            AuditBatch batch = new AuditBatch(batchId, stored.transactions, Money.of(totalAmount));
            allBatches.add(batch);
        }

        return allBatches;
    }

    @Override
    public void clear() {
        store.clear();
    }
}