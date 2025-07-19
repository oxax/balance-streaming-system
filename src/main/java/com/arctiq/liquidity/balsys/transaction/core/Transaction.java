package com.arctiq.liquidity.balsys.transaction.core;

import java.time.Instant;

import com.arctiq.liquidity.balsys.shared.domain.model.Money;

public record Transaction(TransactionId id, Money amount, Instant timestamp) {

    public Transaction(TransactionId id, Money amount) {
        this(id, amount, Instant.now());
    }

    public static Transaction validated(TransactionId id, Money amount, TransactionValidator validator) {
        validator.validate(id, amount);
        return new Transaction(id, amount);
    }

    public boolean isDebit() {
        return amount.isNegative();
    }

    public boolean isCredit() {
        return amount.isPositive();
    }

    public Money absoluteValue() {
        return amount.abs();
    }
}
