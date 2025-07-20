package com.arctiq.liquidity.balsys.telemetry.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;
import com.arctiq.liquidity.balsys.transaction.core.outcome.TransactionAccepted;
import com.arctiq.liquidity.balsys.transaction.core.outcome.TransactionInvalid;
import com.arctiq.liquidity.balsys.transaction.core.outcome.TransactionOutcome;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class MetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollector.class);

    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicInteger acceptedTxCount = new AtomicInteger();
    private final AtomicInteger invalidTxCount = new AtomicInteger();
    private final AtomicInteger auditBatchCount = new AtomicInteger();
    private final AtomicInteger totalAuditTransactions = new AtomicInteger();

    private final Counter droppedTxCounter;
    private final Timer auditLatencyTimer;
    private final Instant startTime = Instant.now();

    public MetricsCollector(MeterRegistry registry) {
        Gauge.builder("audit_queue_size", queueSize, AtomicInteger::get)
                .description("Current size of the audit transaction queue")
                .register(registry);

        this.droppedTxCounter = Counter.builder("dropped_tx_total")
                .description("Total dropped transactions due to saturation")
                .register(registry);

        this.auditLatencyTimer = Timer.builder("audit_latency_ms")
                .description("Latency of audit batch processing")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void updateQueueSize(int size) {
        queueSize.set(size);
        logger.debug("Queue size updated: {}", size);
    }

    public void incrementDropped(Transaction tx) {
        droppedTxCounter.increment();
        logger.warn("Transaction dropped: {}", tx);
    }

    public Timer.Sample startAuditLatencySample() {
        return Timer.start();
    }

    public void recordAuditLatency(Timer.Sample sample) {
        sample.stop(auditLatencyTimer);
    }

    public void recordTransaction(Transaction tx) {
        logger.info("Transaction recorded at {} | Type: {} | Amount: {}",
                Instant.now(),
                tx.isCredit() ? "Credit" : "Debit",
                String.format("%.2f", tx.amount().amount()));
    }

    public void recordBalance(double balance) {
        logger.info("Balance updated at {} | New Balance: {}", Instant.now(), String.format("%.2f", balance));
    }

    public void recordAuditSubmission(List<AuditBatch> batches) {
        logger.info("Audit submission at {} | Submitted {} batches", Instant.now(), batches.size());
        auditBatchCount.addAndGet(batches.size());
        batches.forEach(batch -> totalAuditTransactions.addAndGet(batch.getTransactionCount()));
    }

    public void recordTransactionOutcome(TransactionOutcome outcome) {
        if (outcome instanceof TransactionAccepted) {
            acceptedTxCount.incrementAndGet();
        } else if (outcome instanceof TransactionInvalid) {
            invalidTxCount.incrementAndGet();
        }

        String type = outcome instanceof TransactionAccepted ? "Accepted" : "Invalid";
        logger.debug("Transaction outcome [{}] at {} | Amount: {} | ID: {}",
                type,
                Instant.now(),
                outcome.transaction() != null ? outcome.transaction().amount().amount() : "N/A",
                outcome.transaction() != null ? outcome.transaction().id().value() : "N/A");
    }

    public void logRuntimeMetrics() {
        logger.info("🧮 Metrics Snapshot [{}]", Instant.now());
        logger.info(" - Queue Size: {}", getCurrentQueueSize());
        logger.info(" - Dropped Tx: {}", getDroppedTxCount());
        logger.info(" - Accepted Tx: {}", getAcceptedTxCount());
        logger.info(" - Invalid Tx: {}", getInvalidTxCount());
        logger.info(" - Audit Submissions: {}", getAuditBatchCount());
        logger.info(" - Average Batch Size: {}", String.format("%.2f", getAverageBatchSize()));
        getLatencySnapshot()
                .forEach((k, v) -> logger.info(" - Audit Latency [{}]: {} ms", k, String.format("%.2f", v)));
    }

    public int getCurrentQueueSize() {
        return queueSize.get();
    }

    public double getDroppedTxCount() {
        return droppedTxCounter.count();
    }

    public Map<String, Double> getLatencySnapshot() {
        var snapshot = auditLatencyTimer.takeSnapshot();

        double mean = Double.isFinite(auditLatencyTimer.mean(TimeUnit.MILLISECONDS))
                ? auditLatencyTimer.mean(TimeUnit.MILLISECONDS)
                : -1;

        double max = Double.isFinite(auditLatencyTimer.max(TimeUnit.MILLISECONDS))
                ? auditLatencyTimer.max(TimeUnit.MILLISECONDS)
                : -1;

        double p95 = Arrays.stream(snapshot.percentileValues())
                .filter(p -> p.percentile() == 0.95)
                .mapToDouble(p -> p.value(TimeUnit.MILLISECONDS))
                .findFirst()
                .orElse(-1.0);

        return Map.of("mean", mean, "max", max, "p95", p95);
    }

    public int getAcceptedTxCount() {
        return acceptedTxCount.get();
    }

    public int getInvalidTxCount() {
        return invalidTxCount.get();
    }

    public int getAuditBatchCount() {
        return auditBatchCount.get();
    }

    public double getAverageBatchSize() {
        int batchCount = auditBatchCount.get();
        return batchCount > 0 ? (double) totalAuditTransactions.get() / batchCount : 0.0;
    }

    public double getAverageTPS() {
        long elapsedMillis = Duration.between(startTime, Instant.now()).toMillis();
        return elapsedMillis > 0 ? (acceptedTxCount.get() * 1000.0) / elapsedMillis : 0.0;
    }

}
