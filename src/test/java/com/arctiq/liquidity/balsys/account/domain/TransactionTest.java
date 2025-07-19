package com.arctiq.liquidity.balsys.account.domain;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.account.domain.model.TransactionId;
import com.arctiq.liquidity.balsys.exception.TransactionValidationException;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

class TransactionTest {

    @Test
    @DisplayName("Valid credit transaction is accepted")
    void shouldCreateValidCreditTransaction() {
        Transaction tx = new Transaction(TransactionId.generate(), Money.of(300_000.0));

        assertNotNull(tx.id());
        assertEquals(300_000.0, tx.amount().amount().doubleValue());
        assertTrue(tx.isCredit());
        assertFalse(tx.isDebit());
        assertNotNull(tx.timestamp());
    }

    @Test
    @DisplayName("Valid debit transaction is accepted")
    void shouldCreateValidDebitTransaction() {
        Transaction tx = new Transaction(TransactionId.generate(), Money.of(-450_000.0));

        assertTrue(tx.isDebit());
        assertFalse(tx.isCredit());
    }

    @Test
    @DisplayName("Rejects null transaction ID")
    void shouldThrowForNullId() {
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> new Transaction(null, Money.of(250_000.0)));
        assertTrue(ex.getMessage().contains("Transaction ID must not be null"));
    }

    @Test
    @DisplayName("Rejects null money amount")
    void shouldThrowForNullMoney() {
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> new Transaction(TransactionId.generate(), null));
        assertTrue(ex.getMessage().contains("Transaction amount must not be null"));
    }

    @Test
    @DisplayName("Rejects transaction below allowed range")
    void shouldRejectTooSmallAmount() {
        BigDecimal tooSmall = BigDecimal.valueOf(199.0);
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> new Transaction(TransactionId.generate(), new Money(tooSmall)));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    @DisplayName("Rejects transaction above allowed range")
    void shouldRejectTooLargeAmount() {
        BigDecimal tooLarge = BigDecimal.valueOf(1_000_001.0);
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> new Transaction(TransactionId.generate(), new Money(tooLarge)));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    @DisplayName("Handles transaction with zero amount")
    void shouldRejectZeroTransaction() {
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> new Transaction(TransactionId.generate(), Money.of(0.0)));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    @DisplayName("Rejects absurdly precise amount")
    void shouldRejectNanoprecisionAmount() {
        BigDecimal nanoAmount = new BigDecimal("0.00000000001");
        TransactionValidationException ex = assertThrows(
                TransactionValidationException.class,
                () -> new Transaction(TransactionId.generate(), new Money(nanoAmount)));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    @DisplayName("Rejects mixed-currency construction (if extended)")
    void shouldRejectMixedCurrencyInMoneyAdd() {
        Money british = new Money(BigDecimal.valueOf(1000));
        Money american = new Money(BigDecimal.valueOf(2000));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> british.add(american));
        assertTrue(ex.getMessage().contains("different currencies"));
    }
}
