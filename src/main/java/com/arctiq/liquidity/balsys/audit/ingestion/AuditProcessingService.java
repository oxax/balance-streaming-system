package com.arctiq.liquidity.balsys.audit.ingestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingAlgorithm;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

import io.micrometer.core.instrument.Timer;

public class AuditProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AuditProcessingService.class);

    private final BlockingQueue<Transaction> transactionQueue;
    private final BatchingAlgorithm batchingAlgorithm;
    private final AuditNotifier notifier;
    private final AuditBatchPersistence auditBatchPersistence;
    private final MetricsCollector metrics;

    private final ExecutorService auditExecutor;
    private final ScheduledExecutorService scheduler;

    private final int submissionLimit;
    private final int queueCapacity;
    private final long flushIntervalMillis;
    private final AtomicBoolean submitting = new AtomicBoolean(false);

    public AuditProcessingService(
            int queueCapacity,
            int submissionLimit,
            long flushIntervalMillis,
            int auditThreads,
            BatchingAlgorithm batchingAlgorithm,
            AuditNotifier notifier,
            AuditBatchPersistence auditBatchPersistence,
            MetricsCollector metrics) {

        this.queueCapacity = queueCapacity;
        this.submissionLimit = submissionLimit;
        this.flushIntervalMillis = flushIntervalMillis;
        this.batchingAlgorithm = batchingAlgorithm;
        this.notifier = notifier;
        this.auditBatchPersistence = auditBatchPersistence;
        this.metrics = metrics;

        this.transactionQueue = new ArrayBlockingQueue<>(queueCapacity);
        this.auditExecutor = Executors.newFixedThreadPool(auditThreads);
        this.scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(this::flushDueToTimeout, flushIntervalMillis, flushIntervalMillis,
                TimeUnit.MILLISECONDS);

        logger.info(
                "AuditProcessingService initialized: capacity={}, submissionLimit={}, flushInterval={}ms, threads={}",
                queueCapacity, submissionLimit, flushIntervalMillis, auditThreads);
    }

    public boolean ingest(Transaction tx) {
        boolean accepted = transactionQueue.offer(tx);
        metrics.updateQueueSize(transactionQueue.size());

        if (!accepted) {
            logger.warn("Queue saturated. Dropping transaction: {}", tx);
            persistDropped(tx);
            metrics.incrementDropped(tx);
        }
        return accepted;
    }

    private void flushDueToTimeout() {
        if (!transactionQueue.isEmpty() && submitting.compareAndSet(false, true)) {
            auditExecutor.submit(this::runAuditCycle);
        }
    }

    public void flushIfThresholdMet() {
        if (transactionQueue.size() >= submissionLimit && submitting.compareAndSet(false, true)) {
            auditExecutor.submit(this::runAuditCycle);
        }
    }

    private void runAuditCycle() {
        Timer.Sample latencySample = metrics.startAuditLatencySample();

        try {
            List<Transaction> drained = new ArrayList<>();
            transactionQueue.drainTo(drained, submissionLimit);
            metrics.updateQueueSize(transactionQueue.size());

            if (drained.isEmpty())
                return;

            String batchId = UUID.randomUUID().toString();
            auditBatchPersistence.save(batchId, drained);

            List<AuditBatch> grouped = batchingAlgorithm.groupIntoBatches(drained);
            notifier.submit(grouped);

            auditBatchPersistence.markSubmitted(batchId);

            logger.info("Audit batch [{}] submitted: {} transactions, {} sub-batches", batchId, drained.size(),
                    grouped.size());

        } catch (Exception e) {
            logger.error("Audit cycle failure: {}", e.getMessage(), e);
        } finally {
            submitting.set(false);
            metrics.recordAuditLatency(latencySample);
        }
    }

    private void persistDropped(Transaction tx) {
        String dropId = "dropped-" + UUID.randomUUID();
        try {
            auditBatchPersistence.save(dropId, Collections.singletonList(tx));
        } catch (Exception e) {
            logger.error("Failed to persist dropped transaction [{}]: {}", dropId, e.getMessage(), e);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        auditExecutor.shutdown();
        logger.info("AuditProcessingService shutting down gracefully.");
    }
}
