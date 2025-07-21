package com.arctiq.liquidity.balsys.audit.transport;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arctiq.liquidity.balsys.audit.application.AuditStatsService;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Audit", description = "Audit batch retrieval and telemetry")
@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditStatsService statsService;
    private final MetricsCollector metrics;

    public AuditController(AuditStatsService statsService, MetricsCollector metrics) {
        this.statsService = statsService;
        this.metrics = metrics;
    }

    @PreAuthorize("hasRole('OPS')")
    @Operation(summary = "Get audit summary", description = "Returns TPS, latency distribution, batch composition, and transaction outcomes.")
    @ApiResponse(responseCode = "200", description = "Audit summary retrieved")
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

    @PreAuthorize("hasRole('OPS')")
    @Operation(summary = "Get recent audit batches", description = "Returns a list of recently persisted audit batches with batch ID, count, and total value.")
    @ApiResponse(responseCode = "200", description = "Audit batches retrieved")
    @GetMapping("/batches")
    public List<AuditBatch> getRecentBatches() {
        return statsService.getRecentBatches();
    }

    @Schema(description = "Audit summary response with metrics and telemetry")
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
