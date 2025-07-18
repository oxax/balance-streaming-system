package com.jpc.exercise.audit.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.audit.domain.model.AuditBatch;

public class BatchingAlgorithm {

    private static final long MAX_BATCH_TOTAL = 1_000_000L;

    public List<AuditBatch> groupIntoBatches(List<Transaction> transactions) {
        List<Transaction> sorted = new ArrayList<>(transactions);
        sorted.sort((a, b) -> Long.compare(Math.abs(b.getAmount()), Math.abs(a.getAmount())));

        List<AuditBatch> batches = new ArrayList<>();
        List<Transaction> current = new ArrayList<>();
        long currentTotal = 0;

        for (Transaction tx : sorted) {
            long absAmount = Math.abs(tx.getAmount());
            if (currentTotal + absAmount <= MAX_BATCH_TOTAL) {
                current.add(tx);
                currentTotal += absAmount;
            } else {
                batches.add(new AuditBatch(UUID.randomUUID().toString(), current));
                current = new ArrayList<>(List.of(tx));
                currentTotal = absAmount;
            }
        }

        if (!current.isEmpty()) {
            batches.add(new AuditBatch(UUID.randomUUID().toString(), current));
        }

        return batches;
    }
}
