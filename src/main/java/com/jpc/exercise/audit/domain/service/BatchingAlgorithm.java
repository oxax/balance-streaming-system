package com.jpc.exercise.audit.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.audit.domain.model.AuditBatch;

public class BatchingAlgorithm {

    private static final double MAX_BATCH_TOTAL = 1_000_000;

    public List<AuditBatch> groupIntoBatches(List<Transaction> transactions) {
        List<AuditBatch> auditBatches = new ArrayList<>();
        List<List<Transaction>> rawBatches = new ArrayList<>();

        List<Transaction> sorted = transactions.stream()
                .sorted(Comparator.comparingDouble(t -> -Math.abs(t.getAmount())))
                .toList();

        for (Transaction tx : sorted) {
            boolean placed = false;
            for (List<Transaction> batch : rawBatches) {
                double batchTotal = batch.stream()
                        .mapToDouble(t -> Math.abs(t.getAmount()))
                        .sum();
                if (batchTotal + Math.abs(tx.getAmount()) <= MAX_BATCH_TOTAL) {
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
            String batchId = UUID.randomUUID().toString();
            auditBatches.add(new AuditBatch(batchId, batch));
        }

        return auditBatches;
    }

}
