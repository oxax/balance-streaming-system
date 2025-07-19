package com.arctiq.liquidity.balsys.audit.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

public class AuditBatch {

    private final String batchId;
    private List<Transaction> transactions = new ArrayList<>();
    private Money totalValue = Money.of(0.0);
    // private static final Money VALUE_LIMIT = Money.of(1_000_000.0);
    // public AuditBatch(String batchId) {
    // this.batchId = batchId;
    // }

    private final Money valueLimit;

    public AuditBatch(String batchId, List<Transaction> transactions, Money valueLimit) {
        this.batchId = batchId;
        this.transactions = List.copyOf(transactions);
        this.totalValue = transactions.stream()
                .map(Transaction::absoluteValue)
                .reduce(Money.of(0.0), Money::add);
        this.valueLimit = valueLimit;
    }

    public boolean canAccept(Transaction tx) {
        return totalValue.add(tx.absoluteValue()).amount().compareTo(valueLimit.amount()) <= 0;
    }

    // public void add(Transaction tx) {
    // if (!canAccept(tx))
    // throw new IllegalStateException("Batch limit exceeded");
    // transactions.add(tx);
    // totalValue = totalValue.add(tx.absoluteValue());
    // }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public Money getTotalValue() {
        return totalValue;
    }

    public int getTransactionCount() {
        return transactions.size();
    }

    public String getBatchId() {
        return batchId;
    }
}
