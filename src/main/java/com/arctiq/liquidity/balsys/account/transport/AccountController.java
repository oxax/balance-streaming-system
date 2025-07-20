package com.arctiq.liquidity.balsys.account.transport;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final BankAccountService bankAccountService;
    private final MetricsCollector metricsCollector;

    public AccountController(BankAccountService bankAccountService, MetricsCollector metricsCollector) {
        this.bankAccountService = bankAccountService;
        this.metricsCollector = metricsCollector;
    }

    @GetMapping(value = "/balance", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<BalanceResponse> getBalance() {
        double value = bankAccountService.retrieveBalance();
        metricsCollector.recordBalance(value);
        return Mono.fromSupplier(() -> new BalanceResponse(bankAccountService.retrieveBalance()));
    }

    public record BalanceResponse(double balance) {
    }
}