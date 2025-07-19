package com.arctiq.liquidity.balsys.transaction.core;

import java.math.BigDecimal;

import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.exception.TransactionValidationException;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

public class TransactionValidator {

    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;

    public TransactionValidator(TransactionConfigProperties config) {
        this.minAmount = BigDecimal.valueOf(config.getMinAmount());
        this.maxAmount = BigDecimal.valueOf(config.getMaxAmount());
    }

    public void validate(TransactionId id, Money amount) {
        if (id == null) {
            throw new TransactionValidationException("Transaction ID must not be null");
        }
        validate(amount);
    }

    public void validate(Money amount) {
        if (amount == null) {
            throw new TransactionValidationException("Transaction amount must not be null");
        }

        BigDecimal abs = amount.amount().abs();
        if (abs.compareTo(minAmount) < 0 || abs.compareTo(maxAmount) > 0) {
            throw new TransactionValidationException(
                    "Transaction amount is out of range: " + amount.amount() +
                            " (expected between " + minAmount + " and " + maxAmount + ")");
        }
    }
}