package com.arctiq.liquidity.balsys.config;

import java.util.concurrent.LinkedTransferQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.account.application.BankAccountServiceImpl;
import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.audit.application.AuditStatsService;
import com.arctiq.liquidity.balsys.audit.dispatch.ConsoleAuditNotifier;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingAlgorithm;
import com.arctiq.liquidity.balsys.audit.ingestion.AuditProcessingService;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.audit.persistence.InMemoryAuditBatchStore;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class DomainConfig {

    @Bean
    public LinkedTransferQueue<Transaction> transactionQueue() {
        return new LinkedTransferQueue<>();
    }

    @Bean
    public BankAccountService bankAccountService(LinkedTransferQueue<Transaction> transactionQueue) {
        return new BankAccountServiceImpl(transactionQueue, new TransactionConfigProperties());
    }

    @Bean
    public AuditProcessingService auditService(
            int queueCapacity,
            int submissionLimit,
            long flushIntervalMillis,
            int auditThreads,
            AuditBatchPersistence auditBatchPersistence,
            AuditStatsService auditStatsService,
            MetricsCollector metricsCollector) {

        return new AuditProcessingService(queueCapacity, submissionLimit, flushIntervalMillis, auditThreads,
                new BatchingAlgorithm(new TransactionConfigProperties()),
                new ConsoleAuditNotifier(metricsCollector, auditStatsService),
                auditBatchPersistence, metricsCollector);
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