package com.arctiq.liquidity.balsys.audit.domain;

import java.util.List;

import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

public final class AuditBatch {
    private final String batchId;
    private final List<Transaction> transactions;
    private final Money totalValue;
    private final Money valueLimit;

    public AuditBatch(String batchId, List<Transaction> transactions, Money valueLimit) {
        this.batchId = batchId;
        this.transactions = List.copyOf(transactions);
        this.totalValue = transactions.stream()
                .map(Transaction::absoluteValue)
                .reduce(Money.of(0.0), Money::add);
        this.valueLimit = valueLimit;

        if (this.totalValue.amount().compareTo(valueLimit.amount()) > 0) {
            throw new IllegalStateException("Batch exceeds maximum allowed value");
        }
    }

    public boolean isWithinLimit() {
        return totalValue.amount().compareTo(valueLimit.amount()) <= 0;
    }

    public String getBatchId() {
        return batchId;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public Money getTotalValue() {
        return totalValue;
    }

    public int getTransactionCount() {
        return transactions.size();
    }

    @Override
    public String toString() {
        return String.format("AuditBatch[id=%s, txCount=%d, totalValue=%.2f, valueLimit=%.2f, withinLimit=%s]",
                batchId,
                getTransactionCount(),
                totalValue.amount().doubleValue(),
                valueLimit.amount().doubleValue(),
                isWithinLimit());
    }
}