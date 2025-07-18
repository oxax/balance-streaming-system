package com.jpc.exercise.audit.domain.model;

import java.util.List;

import com.jpc.exercise.account.domain.model.Transaction;

public class AuditBatch {
    private final String batchId;
    private final List<Transaction> transactions;

    public AuditBatch(String batchId, List<Transaction> transactions) {
        this.batchId = batchId;
        this.transactions = List.copyOf(transactions);
    }

    public String getBatchId() {
        return batchId;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public long getTotalValue() {
        return transactions.stream()
                .mapToLong(tx -> Math.abs(tx.getAmount()))
                .sum();
    }

    public int getTransactionCount() {
        return transactions.size();
    }
}
