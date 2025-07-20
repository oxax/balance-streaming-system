package com.arctiq.liquidity.balsys.audit.transport;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arctiq.liquidity.balsys.audit.application.AuditStatsService;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditStatsService statsService;
    private final MetricsCollector metrics;

    public AuditController(AuditStatsService statsService, MetricsCollector metrics) {
        this.statsService = statsService;
        this.metrics = metrics;
    }

    @GetMapping("/summary")
    public AuditStatusResponse getAuditSummary() {
        return new AuditStatusResponse(
                statsService.getStatsSnapshot(),
                metrics.getCurrentQueueSize(),
                metrics.getDroppedTxCount(),
                metrics.getLatencySnapshot(),
                statsService.getTelemetryEvents(),
                metrics.getAcceptedTxCount(),
                metrics.getInvalidTxCount(),
                metrics.getAuditBatchCount(),
                metrics.getAverageBatchSize(),
                metrics.getAverageTPS());
    }

    @GetMapping("/batches")
    public List<AuditBatch> getRecentBatches() {
        return statsService.getRecentBatches();
    }

    @GetMapping("/stats")
    public AuditStatsService.Stats getStats() {
        return statsService.getStatsSnapshot();
    }

    @GetMapping("/telemetry")
    public List<String> getTelemetryEvents() {
        return statsService.getTelemetryEvents();
    }

    public record AuditStatusResponse(
            AuditStatsService.Stats domainStats,
            int queueSize,
            double droppedTransactions,
            Map<String, Double> auditLatencySummaryMs,
            List<String> telemetryEvents,
            int acceptedTransactions,
            int invalidTransactions,
            int auditSubmissions,
            double averageBatchSize,
            double averageTPS) {
    }
}
