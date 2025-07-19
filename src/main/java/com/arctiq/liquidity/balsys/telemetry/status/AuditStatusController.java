package com.arctiq.liquidity.balsys.telemetry.status;

import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuditStatusController {

    private final MetricsCollector metrics;

    public AuditStatusController(MetricsCollector metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/audit/status")
    public Map<String, Object> getAuditMetrics() {
        return Map.of(
                "queueSize", metrics.getCurrentQueueSize(),
                "droppedTransactions", metrics.getDroppedTxCount(),
                "auditLatencySummaryMs", metrics.getLatencySnapshot());
    }
}
