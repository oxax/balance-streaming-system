package com.arctiq.liquidity.balsys.audit.grouping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

public class BatchingAlgorithm {

    private final Money maxBatchValue;

    public BatchingAlgorithm(TransactionConfigProperties config) {
        this.maxBatchValue = Money.of(config.getMaxBatchValue());
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

                if (batchTotal.add(txValue).amount().compareTo(maxBatchValue.amount()) <= 0) {
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
            auditBatches.add(new AuditBatch(batchId, batch, maxBatchValue));
        }

        return auditBatches;
    }
}
