package com.jpc.exercise.account.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.random.RandomGenerator;

import com.jpc.exercise.exception.TransactionValidationException;

public class Transaction {

    private final String id;
    private final double amount;
    private final Instant timestamp;

    private static final double MIN_AMOUNT = 200.0;
    private static final double MAX_AMOUNT = 500_000.0;

    /**
     * Constructs a Transaction with random ID and validated amount.
     */
    public Transaction(RandomGenerator generator, double amount) {
        this(generateId(generator), amount, Instant.now());
    }

    /**
     * Constructs a Transaction with provided ID and amount.
     */
    public Transaction(String id, double amount) {
        this(id, amount, Instant.now());
    }

    /**
     * Constructs a Transaction with provided ID, amount, and timestamp.
     * Used internally to ensure timestamp is always set at creation.
     */
    private Transaction(String id, double amount, Instant timestamp) {
        if (id == null || id.isBlank()) {
            throw new TransactionValidationException("Transaction ID cannot be null or blank");
        }
        if (!isValidAmount(amount)) {
            throw new TransactionValidationException("Transaction amount is out of range: must be between £200 and £500,000");
        }
        this.id = id;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    private static boolean isValidAmount(double value) {
        double abs = Math.abs(value);
        return abs >= MIN_AMOUNT && abs <= MAX_AMOUNT;
    }

    private static String generateId(RandomGenerator generator) {
        return Long.toHexString(generator.nextLong());
    }

    public String getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isCredit() {
        return amount > 0;
    }

    public boolean isDebit() {
        return amount < 0;
    }

    @Override
    public String toString() {
        return "Transaction{id='" + id + "', amount=" + amount + ", timestamp=" + timestamp + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction tx)) return false;
        return Double.compare(tx.amount, amount) == 0
                && id.equals(tx.id)
                && timestamp.equals(tx.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, amount, timestamp);
    }
}