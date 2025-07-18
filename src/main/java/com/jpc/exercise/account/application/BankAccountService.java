package com.jpc.exercise.account.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.jpc.exercise.account.domain.model.Transaction;

public interface BankAccountService {
    /**
     * Processes a transaction, updating the account balance accordingly.
     *
     * @param transaction the transaction to process
     */
    void processTransaction(Transaction transaction);

    /**
     * Retrieves the current balance of the bank account.
     *
     * @return the current balance
     */
    double retrieveBalance();

    /**
     * Retrieves the "available" balance, which is the total balance
     * minus any funds currently on hold.
     *
     * @return The available balance.
     */
    double retrieveAvailableBalance();

    /**
     * Retrieves a list of transactions for a given date range.
     * This would require a persistence layer to be truly useful.
     *
     * @param start The start timestamp of the period.
     * @param end   The end timestamp of the period.
     * @return A list of transactions within that period.
     */
    List<Transaction> getTransactionHistory(Instant start, Instant end);

    /**
     * Places a temporary hold on funds (e.g., for a card authorization).
     *
     * @param amount The amount to hold.
     * @param reason A description for the hold.
     * @return A unique ID for the hold that can be used to release it.
     */
    String placeHold(BigDecimal amount, String reason);

    /**
     * Releases a previously placed hold on funds.
     *
     * @param holdId The unique ID of the hold to release.
     */
    void releaseHold(String holdId);
}
