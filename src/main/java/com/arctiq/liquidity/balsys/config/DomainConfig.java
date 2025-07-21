package com.arctiq.liquidity.balsys.config;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.account.application.BankAccountServiceImpl;
import com.arctiq.liquidity.balsys.audit.application.AuditStatsService;
import com.arctiq.liquidity.balsys.audit.dispatch.ConsoleAuditNotifier;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingStrategy;
import com.arctiq.liquidity.balsys.audit.grouping.FirstFitDecreasingBatchingStrategy;
import com.arctiq.liquidity.balsys.audit.ingestion.AuditProcessingService;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.audit.persistence.InMemoryAuditBatchStore;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

@Configuration
public class DomainConfig {

    @Bean
    public LinkedTransferQueue<Transaction> transactionQueue() {
        return new LinkedTransferQueue<>();
    }

    @Bean
    public BankAccountService bankAccountService(LinkedTransferQueue<Transaction> transactionQueue,
            MetricsCollector metricsCollector,
            TransactionConfigProperties config) {
        return new BankAccountServiceImpl(transactionQueue, config, metricsCollector);
    }

    @Bean
    public BatchingStrategy batchingStrategy(TransactionConfigProperties config) {
        Money batchLimit = Money.of(config.getMaxBatchValue());
        return new FirstFitDecreasingBatchingStrategy(batchLimit);
    }

    @Bean
    public AuditNotifier auditNotifier(MetricsCollector metricsCollector, AuditStatsService auditStatsService) {
        return new ConsoleAuditNotifier();
    }

    @Bean
    public AuditProcessingService auditService(BlockingQueue<Transaction> transactionQueue,
            TransactionConfigProperties config,
            BatchingStrategy batchingStrategy,
            AuditNotifier auditNotifier,
            AuditBatchPersistence auditBatchPersistence,
            MetricsCollector metricsCollector) {

        return new AuditProcessingService(
                transactionQueue,
                config,
                batchingStrategy,
                auditNotifier,
                auditBatchPersistence,
                metricsCollector);
    }

    @Bean
    public AuditBatchPersistence auditBatchPersistence() {
        return new InMemoryAuditBatchStore(); // or JpaAuditBatchStore in production
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