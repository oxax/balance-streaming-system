package com.jpc.exercise.config;

import java.util.concurrent.LinkedTransferQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jpc.exercise.account.application.BankAccountService;
import com.jpc.exercise.account.application.BankAccountServiceImpl;
import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.audit.application.AuditProcessingService;
import com.jpc.exercise.audit.application.AuditStatsService;
import com.jpc.exercise.audit.domain.service.BatchingAlgorithm;
import com.jpc.exercise.audit.infrastructure.notifier.ConsoleAuditNotifier;
import com.jpc.exercise.audit.infrastructure.persistence.AuditBatchPersistence;
import com.jpc.exercise.audit.infrastructure.persistence.InMemoryAuditBatchStore;
import com.jpc.exercise.shared.observability.MetricsCollector;

@Configuration
public class DomainConfig {

    @Bean
    public LinkedTransferQueue<Transaction> transactionQueue() {
        return new LinkedTransferQueue<>();
    }

    @Bean
    public BankAccountService bankAccountService(LinkedTransferQueue<Transaction> transactionQueue) {
        return new BankAccountServiceImpl(transactionQueue);
    }

    @Bean
    public AuditProcessingService auditService(
            LinkedTransferQueue<Transaction> transactionQueue,
            AuditBatchPersistence auditBatchPersistence,
            MetricsCollector metricsCollector,
            AuditStatsService auditStatsService,
            int submissionLimit) {

        return new AuditProcessingService(
                transactionQueue,
                new BatchingAlgorithm(),
                new ConsoleAuditNotifier(metricsCollector, auditStatsService),
                auditBatchPersistence,
                submissionLimit);
    }

    @Bean
    public AuditBatchPersistence auditBatchPersistence() {
        return new InMemoryAuditBatchStore(); // or future JpaAuditBatchStore()
    }

    @Bean
    public MetricsCollector metricsCollector() {
        return new MetricsCollector();
    }

    @Bean
    public AuditStatsService auditStatsService() {
        return new AuditStatsService();
    }

}