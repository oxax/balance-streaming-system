package com.arctiq.liquidity.balsys.account.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

public class BankAccountServiceImpl implements BankAccountService {

    private final AtomicReference<Money> balance;
    private final LinkedTransferQueue<Transaction> transactionQueue;
    private final List<Transaction> transactionHistory;

    public BankAccountServiceImpl(LinkedTransferQueue<Transaction> transactionQueue,
            TransactionConfigProperties config) {
        this.transactionQueue = transactionQueue;
        this.balance = new AtomicReference<>(Money.of(config.getDefaultBalance()));
        this.transactionHistory = new CopyOnWriteArrayList<>();
    }

    @Override
    public void processTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction must not be null");
        }

        // Atomically apply transaction amount
        balance.updateAndGet(b -> b.add(transaction.amount()));
        transactionQueue.offer(transaction);
        transactionHistory.add(transaction);
    }

    @Override
    public double retrieveBalance() {
        return balance.get().asDouble();
    }

    @Override
    public double retrieveAvailableBalance() {
        throw new UnsupportedOperationException("Unimplemented method 'retrieveAvailableBalance'");
    }

    @Override
    public List<Transaction> getTransactionHistory(Instant start, Instant end) {
        return transactionHistory.stream()
                .filter(tx -> !tx.timestamp().isBefore(start) && !tx.timestamp().isAfter(end))
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
