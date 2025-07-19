package com.arctiq.liquidity.balsys.transaction.core;

import com.arctiq.liquidity.balsys.exception.TransactionValidationException;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.testfixtures.AuditTestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private TransactionValidator validator;

    @BeforeEach
    void setup() {
        validator = new TransactionValidator(AuditTestFixtures.config());
    }

    @Test
    @DisplayName("Valid credit transaction is accepted")
    void shouldCreateValidCreditTransaction() {
        Transaction tx = Transaction.validated(TransactionId.generate(), Money.of(300_000.0), validator);
        assertNotNull(tx.id());
        assertEquals(300_000.0, tx.amount().amount().doubleValue());
        assertTrue(tx.isCredit());
        assertFalse(tx.isDebit());
        assertNotNull(tx.timestamp());
    }

    @Test
    @DisplayName("Valid debit transaction is accepted")
    void shouldCreateValidDebitTransaction() {
        Transaction tx = Transaction.validated(TransactionId.generate(), Money.of(-450_000.0), validator);
        assertTrue(tx.isDebit());
        assertFalse(tx.isCredit());
    }

    @Test
    @DisplayName("Rejects null transaction ID")
    void shouldThrowForNullId() {
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> Transaction.validated(null, Money.of(250_000.0), validator));
        assertTrue(ex.getMessage().contains("Transaction ID must not be null"));
    }

    @Test
    @DisplayName("Rejects null money amount")
    void shouldThrowForNullMoney() {
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> Transaction.validated(TransactionId.generate(), null, validator));
        assertTrue(ex.getMessage().contains("Transaction amount must not be null"));
    }

    @Test
    @DisplayName("Rejects transaction below allowed range")
    void shouldRejectTooSmallAmount() {
        BigDecimal tooSmall = BigDecimal.valueOf(199.0);
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> Transaction.validated(TransactionId.generate(), new Money(tooSmall), validator));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    @DisplayName("Rejects transaction above allowed range")
    void shouldRejectTooLargeAmount() {
        BigDecimal tooLarge = BigDecimal.valueOf(1_000_001.0);
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> Transaction.validated(TransactionId.generate(), new Money(tooLarge), validator));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    @DisplayName("Rejects zero transaction amount")
    void shouldRejectZeroTransaction() {
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> Transaction.validated(TransactionId.generate(), Money.of(0.0), validator));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    @DisplayName("Rejects absurdly precise amount")
    void shouldRejectNanoPrecision() {
        BigDecimal nano = new BigDecimal("0.00000000001");
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> Transaction.validated(TransactionId.generate(), new Money(nano), validator));
        assertTrue(ex.getMessage().contains("out of range"));
    }
}