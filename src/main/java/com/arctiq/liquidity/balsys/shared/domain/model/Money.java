package com.arctiq.liquidity.balsys.shared.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount) {

    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public Money abs() {
        return new Money(amount.abs());
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public double asDouble() {
        return amount.doubleValue();
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }
}
