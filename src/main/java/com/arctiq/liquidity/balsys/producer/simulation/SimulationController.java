package com.arctiq.liquidity.balsys.producer.simulation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Simulation", description = "Controls transaction emitter lifecycle")
@RestController
@RequestMapping("/simulation")
public class SimulationController {

    private final TransactionSimulationManager simulationManager;

    public SimulationController(TransactionSimulationManager simulationManager) {
        this.simulationManager = simulationManager;
    }

    @PreAuthorize("hasRole('OPS')")
    @Operation(summary = "Start transaction simulation", description = "Begins emitting credit/debit transactions at ~50 TPS for a given duration.", requestBody = @RequestBody(description = "Simulation configuration", required = true, content = @Content(schema = @Schema(implementation = SimulationStartRequest.class))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation started"),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters")
    })
    @PostMapping("/start")
    public ResponseEntity<String> startSimulation(@RequestBody SimulationStartRequest request) {
        simulationManager.startSimulation(request.getTransactionCount(), request.getDurationSeconds());
        return ResponseEntity.ok("Simulation started");
    }

    @PreAuthorize("hasRole('OPS')")
    @Operation(summary = "Stop active simulation", description = "Halts all running transaction emitters.")
    @ApiResponse(responseCode = "200", description = "Simulation stopped")
    @PostMapping("/stop")
    public ResponseEntity<String> stopSimulation() {
        simulationManager.stopSimulation();
        return ResponseEntity.ok("Simulation stopped");
    }
}
