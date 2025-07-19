package com.arctiq.liquidity.balsys.account.domain.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;

public final class TransactionId {

    private static final AtomicLong counter = new AtomicLong(1);
    private static final RandomGenerator rng = RandomGenerator.getDefault();

    private final long value;

    public TransactionId(long value) {
        this.value = value;
    }

    public static TransactionId generate() {
        long base = counter.getAndIncrement();
        long noise = rng.nextInt(1000);
        return new TransactionId(base * 1000 + noise);
    }

    public long value() {
        return value;
    }

    @Override
    public String toString() {
        return "TX-" + value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TransactionId tid && tid.value == this.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
