package com.arctiq.liquidity.balsys.account.application;

import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;
import com.arctiq.liquidity.balsys.transaction.core.TransactionValidator;
import com.arctiq.liquidity.balsys.transaction.core.outcome.TransactionOutcome;
import com.arctiq.liquidity.balsys.transaction.core.outcome.TransactionAccepted;
import com.arctiq.liquidity.balsys.transaction.core.outcome.TransactionInvalid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BankAccountServiceImpl implements BankAccountService {

    private final AtomicReference<Money> balance;
    private final LinkedTransferQueue<Transaction> transactionQueue;
    private final List<Transaction> transactionHistory;
    private final TransactionValidator validator;
    private final MetricsCollector metricsCollector;

    public BankAccountServiceImpl(LinkedTransferQueue<Transaction> transactionQueue,
            TransactionConfigProperties config,
            MetricsCollector metricsCollector) {
        this.transactionQueue = transactionQueue;
        this.balance = new AtomicReference<>(Money.of(config.getDefaultBalance()));
        this.transactionHistory = new CopyOnWriteArrayList<>();
        this.validator = new TransactionValidator(config);
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void processTransaction(Transaction transaction) {
        if (transaction == null) {
            metricsCollector.recordTransactionOutcome(new TransactionInvalid(null, "Transaction must not be null"));
            throw new IllegalArgumentException("Transaction must not be null");
        }

        try {
            validator.validate(transaction.id(), transaction.amount());
        } catch (Exception ex) {
            metricsCollector.recordTransactionOutcome(new TransactionInvalid(transaction, ex.getMessage()));
            throw ex;
        }

        balance.updateAndGet(b -> b.add(transaction.amount()));
        transactionQueue.offer(transaction);
        transactionHistory.add(transaction);

        metricsCollector.recordTransactionOutcome(new TransactionAccepted(transaction));
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