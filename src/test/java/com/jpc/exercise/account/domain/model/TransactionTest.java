package com.jpc.exercise.account.domain.model;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jpc.exercise.exception.TransactionValidationException;

class TransactionTest {

    private RandomGenerator generator;

    @BeforeEach
    void setup() {
        generator = RandomGenerator.getDefault();
    }

    @Test
    @DisplayName("Valid credit transaction is accepted")
    void shouldCreateValidCreditTransaction() {

        long amount = 300_000L;
        Transaction tx = new Transaction(generator, amount);

        assertTrue(tx.getId() != null && !tx.getId().isEmpty());
        assertEquals(300_000L, tx.getAmount());
        assertTrue(tx.isCredit());
        assertFalse(tx.isDebit());
    }

    @Test
    @DisplayName("Valid debit transaction is accepted")
    void shouldCreateValidDebitTransaction() {
        long amount = -450_000L;
        Transaction tx = new Transaction(generator, amount);

        assertEquals(generator, tx.getId());
        assertEquals(-450_000L, tx.getAmount());
        assertTrue(tx.isDebit());
        assertFalse(tx.isCredit());
    }

    @Test
    @DisplayName("Rejects null transaction ID")
    void shouldThrowForNullId() {
        long amount = 250_000L;

        TransactionValidationException ex = assertThrows(TransactionValidationException.class,
            () -> new Transaction((RandomGenerator) null, amount));

        assertTrue(ex.getMessage().contains("Transaction ID cannot be null"));
    }

    @Test
    @DisplayName("Rejects amount below minimum threshold")
    void shouldRejectTooSmallAmount() {
        long amount = 100L;

        TransactionValidationException ex = assertThrows(TransactionValidationException.class,
            () -> new Transaction(generator, amount));

        assertTrue(ex.getMessage().contains("Transaction amount is out of range"));
    }

    @Test
    @DisplayName("Rejects amount above maximum threshold")
    void shouldRejectTooLargeAmount() {
        long amount = 1_000_000L;

        TransactionValidationException ex = assertThrows(TransactionValidationException.class,
            () -> new Transaction(generator, amount));

        assertTrue(ex.getMessage().contains("Transaction amount is out of range"));
    }
}
