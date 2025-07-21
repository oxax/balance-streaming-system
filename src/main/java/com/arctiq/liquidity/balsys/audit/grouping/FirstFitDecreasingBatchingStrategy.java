package com.arctiq.liquidity.balsys.audit.grouping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

public class FirstFitDecreasingBatchingStrategy implements BatchingStrategy {

    private final Money valueLimit;

    public FirstFitDecreasingBatchingStrategy(Money valueLimit) {
        this.valueLimit = valueLimit;
    }

    @Override
    public List<AuditBatch> groupIntoBatches(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Transaction list must not be empty.");
        }

        // 1. Sort transactions descending by absolute value
        List<Transaction> sorted = transactions.stream()
                .sorted(Comparator.comparingDouble(tx -> -tx.absoluteValue().asDouble()))
                .toList();

        // 2. First-fit placement with Money
        List<List<Transaction>> rawBatches = new ArrayList<>();
        List<Money> batchTotals = new ArrayList<>();

        for (Transaction tx : sorted) {
            Money txValue = tx.absoluteValue();
            boolean placed = false;

            for (int i = 0; i < rawBatches.size(); i++) {
                Money currentTotal = batchTotals.get(i);
                if (currentTotal.add(txValue).amount().compareTo(valueLimit.amount()) <= 0) {
                    rawBatches.get(i).add(tx);
                    batchTotals.set(i, currentTotal.add(txValue));
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                List<Transaction> newBatch = new ArrayList<>();
                newBatch.add(tx);
                rawBatches.add(newBatch);
                batchTotals.add(txValue);
            }
        }

        // 3. Convert raw batches into AuditBatch objects
        List<AuditBatch> auditBatches = new ArrayList<>();
        for (int i = 0; i < rawBatches.size(); i++) {
            String batchId = "batch-" + UUID.randomUUID();
            Money batchTotal = batchTotals.get(i);
            auditBatches.add(new AuditBatch(batchId, rawBatches.get(i), batchTotal));
        }

        return auditBatches;
    }
}
