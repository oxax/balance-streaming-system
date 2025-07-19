package com.arctiq.liquidity.balsys.audit.dispatch;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.arctiq.liquidity.balsys.audit.application.AuditStatsService;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

public class ConsoleAuditNotifier implements AuditNotifier {

    private final MetricsCollector metrics;
    private final AuditStatsService statsService;

    public ConsoleAuditNotifier(MetricsCollector metrics, AuditStatsService statsService) {
        this.metrics = metrics;
        this.statsService = statsService;
    }

    @Override
    public void submit(List<AuditBatch> batches) {

        metrics.recordAuditSubmission(batches);
        statsService.record(batches); // stores for REST exposure
        statsService.recordTelemetryEvent("Submitted " + batches.size() + " batches at " + Instant.now());

        var submission = Map.of("submission", Map.of("batches", batches.stream()
                .map(batch -> Map.of(
                        "totalValueOfAllTransactions", batch.getTotalValue(),
                        "countOfTransactions", batch.getTransactionCount()))
                .toList()));

        System.out.println(prettyPrint(submission));
    }

    private String prettyPrint(Object obj) {
        return """
                {
                  "submission": {
                    "batches": [
                """ +
                ((List<?>) ((Map<?, ?>) ((Map<?, ?>) obj).get("submission")).get("batches")).stream()
                        .map(b -> "      " + b.toString().replace("=", ":"))
                        .reduce((a, b) -> a + ",\n" + b).orElse("")
                +
                "\n    ]\n  }\n}";
    }
}
