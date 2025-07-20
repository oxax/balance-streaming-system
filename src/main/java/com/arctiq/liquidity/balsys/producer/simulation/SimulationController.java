package com.arctiq.liquidity.balsys.producer.simulation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simulation")
public class SimulationController {

    private final TransactionSimulationManager simulationManager;

    public SimulationController(TransactionSimulationManager simulationManager) {
        this.simulationManager = simulationManager;
    }

    @PostMapping("/start")
    public ResponseEntity<String> startSimulation(@RequestBody SimulationStartRequest request) {
        simulationManager.startSimulation(request.getTransactionCount(), request.getDurationSeconds());
        return ResponseEntity.ok("Simulation started");
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopSimulation() {
        simulationManager.stopSimulation();
        return ResponseEntity.ok("Simulation stopped");
    }
}
