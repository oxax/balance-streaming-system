package com.arctiq.liquidity.balsys.account.domain.model;

import java.time.Instant;

import com.arctiq.liquidity.balsys.exception.TransactionValidationException;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

public record Transaction(
        TransactionId id,
        Money amount,
        Instant timestamp) {
    public Transaction(TransactionId id, Money amount) {
        this(id, amount, Instant.now());
    }

    public Transaction {
        if (id == null) {
            throw new TransactionValidationException("Transaction ID must not be null");
        }
        if (amount == null) {
            throw new TransactionValidationException("Transaction amount must not be null");
        }
        if (timestamp == null) {
            throw new TransactionValidationException("Transaction timestamp must not be null");
        }
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
