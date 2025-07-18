package com.jpc.exercise.account.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.jpc.exercise.account.domain.model.Transaction;

public class BankAccountServiceImpl implements BankAccountService {
    
    private final AtomicReference<Double> balance = new AtomicReference<>(0.0);
    // Queue to hold transactions for processing
    // This queue will be shared between producers and consumers
    // Producers will add transactions to this queue
    private final LinkedTransferQueue<Transaction> transactionQueue;
    private final List<Transaction> transactionHistory = new CopyOnWriteArrayList<>();




    // Using LinkedTransferQueue for its thread-safe properties and ability to
    // handle concurrent access
    public BankAccountServiceImpl(LinkedTransferQueue<Transaction> transactionQueue) {
        this.transactionQueue = transactionQueue;
    }

    // Bean definition removed; should be placed in a @Configuration class.

    @Override
    public void processTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction must not be null");
        }
        balance.updateAndGet(b -> b + transaction.getAmount());
        transactionQueue.offer(transaction);
        transactionHistory.add(transaction);
    }

    @Override
    public double retrieveBalance() {
        return balance.get();
    }

    @Override
    public double retrieveAvailableBalance() {
        throw new UnsupportedOperationException("Unimplemented method 'retrieveAvailableBalance'");
    }

    @Override
    public List<Transaction> getTransactionHistory(Instant start, Instant end) {
        return transactionHistory.stream()
                .filter(tx -> !tx.getTimestamp().isBefore(start) && !tx.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    @Override
    public String placeHold(BigDecimal amount, String reason) {
        throw new UnsupportedOperationException("Unimplemented method 'placeHold'");
    }

    @Override
    public void releaseHold(String holdId) {
        throw new UnsupportedOperationException("Unimplemented method 'releaseHold'");
    }
}
