package com.jpc.exercise.account.domain.model;

import java.util.Objects;
import java.util.random.RandomGenerator;

import com.jpc.exercise.exception.TransactionValidationException;

public class Transaction {

    private final String id;
    private final long amount;

    private static final long MIN_AMOUNT = 200L;
    private static final long MAX_AMOUNT = 500_000L;



    /**
     * Constructs a Transaction with random ID and validated amount.
     */
    public Transaction(RandomGenerator generator, long amount) {
        this(generateId(generator), amount);
    }

    /**
     * Constructs a Transaction with provided ID and amount.
     */
    public Transaction(String id, long amount) {
        if (id == null || id.isBlank()) {
            throw new TransactionValidationException("Transaction ID cannot be null or blank");
        }
        if (!isValidAmount(amount)) {
            throw new TransactionValidationException("Transaction amount is out of range: must be between £200 and £500,000");
        }
        this.id = id;
        this.amount = amount;
    }

    private static boolean isValidAmount(long value) {
        long abs = Math.abs(value);
        return abs >= MIN_AMOUNT && abs <= MAX_AMOUNT;
    }

    private static String generateId(RandomGenerator generator) {
        return Long.toHexString(generator.nextLong());
    }

    public String getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }

    public boolean isCredit() {
        return amount > 0;
    }

    public boolean isDebit() {
        return amount < 0;
    }

    @Override
    public String toString() {
        return "Transaction{id='" + id + "', amount=" + amount + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction tx)) return false;
        return amount == tx.amount && id.equals(tx.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, amount);
    }
}