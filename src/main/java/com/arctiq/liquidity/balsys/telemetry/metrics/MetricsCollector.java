package com.arctiq.liquidity.balsys.telemetry.metrics;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class MetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollector.class);

    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final Counter droppedTxCounter;
    private final Timer auditLatencyTimer;

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
        logger.info("Queue size updated: {}", size);
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
        logger.info("Balance updated at {} | New Balance: {}",
                Instant.now(),
                String.format("%.2f", balance));
    }

    public void recordAuditSubmission(List<AuditBatch> batches) {
        logger.info("Audit submission at {} | Submitted {} batches",
                Instant.now(),
                batches.size());
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

        return Map.of(
                "mean", mean,
                "max", max,
                "p95", p95);
    }
}
