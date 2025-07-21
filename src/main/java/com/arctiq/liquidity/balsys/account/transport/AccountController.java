package com.arctiq.liquidity.balsys.account.transport;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@Tag(name = "Account", description = "Balance tracking and transaction history")
@RestController
@RequestMapping("/account")
public class AccountController {

    private final BankAccountService bankAccountService;
    private final MetricsCollector metricsCollector;

    public AccountController(BankAccountService bankAccountService, MetricsCollector metricsCollector) {
        this.bankAccountService = bankAccountService;
        this.metricsCollector = metricsCollector;
    }

    // @PreAuthorize("hasAnyRole('USER', 'OPS')")
    @Operation(summary = "Get current account balance", description = "Returns the latest computed balance after applying all ingested transactions.")
    @ApiResponse(responseCode = "200", description = "Balance retrieved successfully")
    @GetMapping(value = "/balance", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<BalanceResponse> getBalance() {
        double value = bankAccountService.retrieveBalance();
        metricsCollector.recordBalance(value);
        return Mono.fromSupplier(() -> new BalanceResponse(value));
    }

    @Schema(description = "Response containing the current account balance")
    public record BalanceResponse(double balance) {
    }
}
