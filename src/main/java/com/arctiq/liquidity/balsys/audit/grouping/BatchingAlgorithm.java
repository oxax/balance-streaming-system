package com.arctiq.liquidity.balsys.audit.grouping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

public class BatchingAlgorithm {

    private final Money valueLimit;

    public BatchingAlgorithm(Money valueLimit) {
        this.valueLimit = valueLimit;
    }

    public List<AuditBatch> groupIntoBatches(List<Transaction> transactions) {
        List<AuditBatch> auditBatches = new ArrayList<>();
        List<List<Transaction>> rawBatches = new ArrayList<>();

        List<Transaction> sorted = transactions.stream()
                .sorted(Comparator.comparingDouble(t -> -t.absoluteValue().asDouble()))
                .toList();

        for (Transaction tx : sorted) {
            boolean placed = false;
            Money txValue = tx.absoluteValue();

            for (List<Transaction> batch : rawBatches) {
                Money batchTotal = batch.stream()
                        .map(Transaction::absoluteValue)
                        .reduce(Money.of(0.0), Money::add);

                if (batchTotal.add(txValue).amount().compareTo(valueLimit.amount()) <= 0) {
                    batch.add(tx);
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                List<Transaction> newBatch = new ArrayList<>();
                newBatch.add(tx);
                rawBatches.add(newBatch);
            }
        }

        for (List<Transaction> batch : rawBatches) {
            String batchId = "batch-" + UUID.randomUUID();
            auditBatches.add(new AuditBatch(batchId, batch, valueLimit));
        }

        return auditBatches;
    }
}