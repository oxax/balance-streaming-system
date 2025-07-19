package com.arctiq.liquidity.balsys.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.account.application.BankAccountServiceImpl;
import com.arctiq.liquidity.balsys.audit.application.AuditStatsService;
import com.arctiq.liquidity.balsys.audit.dispatch.ConsoleAuditNotifier;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingAlgorithm;
import com.arctiq.liquidity.balsys.audit.ingestion.AuditProcessingService;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.audit.persistence.InMemoryAuditBatchStore;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class DomainConfig {

    @Bean
    public LinkedTransferQueue<Transaction> transactionQueue() {
        return new LinkedTransferQueue<>();
    }

    @Bean
    public BankAccountService bankAccountService(LinkedTransferQueue<Transaction> transactionQueue,
            MetricsCollector metricsCollector) {
        return new BankAccountServiceImpl(transactionQueue, new TransactionConfigProperties(), metricsCollector);
    }

    @Bean
    public AuditProcessingService auditService(BlockingQueue<Transaction> transactionQueue,
            TransactionConfigProperties config,
            AuditBatchPersistence auditBatchPersistence,
            AuditStatsService auditStatsService,
            MetricsCollector metricsCollector) {

        Money batchLimit = Money.of(config.getMaxBatchValue());

        BatchingAlgorithm batchingAlgorithm = new BatchingAlgorithm(batchLimit);
        AuditNotifier notifier = new ConsoleAuditNotifier(metricsCollector, auditStatsService);

        return new AuditProcessingService(
                transactionQueue,
                config,
                batchingAlgorithm,
                notifier,
                auditBatchPersistence,
                metricsCollector);
    }

    @Bean
    public AuditBatchPersistence auditBatchPersistence() {
        return new InMemoryAuditBatchStore(); // or future JpaAuditBatchStore()
    }

    @Bean
    public MetricsCollector metricsCollector(MeterRegistry meterRegistry) {
        return new MetricsCollector(meterRegistry);
    }

    @Bean
    public AuditStatsService auditStatsService() {
        return new AuditStatsService();
    }

}