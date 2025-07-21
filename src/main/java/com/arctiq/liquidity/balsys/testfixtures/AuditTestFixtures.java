package com.arctiq.liquidity.balsys.testfixtures;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;
import com.arctiq.liquidity.balsys.transaction.core.TransactionId;

public final class AuditTestFixtures {

    private static final RandomGenerator generator = createSafeGenerator();

    private static RandomGenerator createSafeGenerator() {
        try {
            return RandomGeneratorFactory.of("Xoshiro256PlusPlus").create();
        } catch (IllegalArgumentException e) {
            // Fallback for Alpine or slim JVMs
            return RandomGeneratorFactory.getDefault().create();
        }
    }

    public static TransactionConfigProperties config() {
        TransactionConfigProperties config = new TransactionConfigProperties();
        config.setMinAmount(200.0);
        config.setMaxAmount(500_000.0);
        config.setMaxBatchValue(1_000_000.0);
        config.setSubmissionLimit(1000);
        config.setQueueCapacity(1000);
        config.setFlushIntervalMillis(1000);
        config.setAuditThreads(1);
        return config;
    }

    public static Transaction randomTransaction(TransactionConfigProperties config) {
        double value = config.getMinAmount() + generator.nextDouble() *
                (config.getMaxAmount() - config.getMinAmount());
        double signed = generator.nextBoolean() ? value : -value;
        return new Transaction(TransactionId.generate(), Money.of(signed));
    }

    public static Transaction randomCredit(TransactionConfigProperties config) {
        double amount = config.getMinAmount() + generator.nextDouble() *
                (config.getMaxAmount() - config.getMinAmount());
        return new Transaction(TransactionId.generate(), Money.of(Math.abs(amount)));
    }

    public static Transaction randomDebit(TransactionConfigProperties config) {
        double amount = config.getMinAmount() + generator.nextDouble() *
                (config.getMaxAmount() - config.getMinAmount());
        return new Transaction(TransactionId.generate(), Money.of(-Math.abs(amount)));
    }

    public static Transaction fixedTransaction(double amount) {
        return new Transaction(TransactionId.generate(), Money.of(amount));
    }

    public static List<Transaction> randomTransactions(int count, TransactionConfigProperties config) {
        return IntStream.range(0, count)
                .mapToObj(i -> randomTransaction(config))
                .toList();
    }

    public static List<Transaction> thresholdTransactions(int count, double amount) {
        return IntStream.range(0, count)
                .mapToObj(i -> fixedTransaction(amount))
                .toList();
    }

    public static List<Transaction> mockSmallTransactions() {
        return List.of(
                new Transaction(TransactionId.generate(), Money.of(-100_000.0), Instant.now()),
                new Transaction(TransactionId.generate(), Money.of(50_000.0), Instant.now()));
    }

    public static AuditBatch auditBatch(List<Transaction> txs, TransactionConfigProperties config) {
        return new AuditBatch("batch-" + UUID.randomUUID(), txs, Money.of(config.getMaxBatchValue()));
    }
}