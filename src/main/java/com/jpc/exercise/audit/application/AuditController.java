package com.jpc.exercise.audit.application;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpc.exercise.audit.domain.model.AuditBatch;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditStatsService statsService;

    public AuditController(AuditStatsService statsService) {
        this.statsService = statsService;
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
        return statsService.getTelemetryEvents(); // Add this method to statsService
    }
}
