package com.jpc.exercise.account.application;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/balance")
public class AccountController {

    private final BankAccountService bankAccountService;

    public AccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<BalanceResponse> getBalance() {
        double value = bankAccountService.retrieveBalance();
        return Mono.just(new BalanceResponse(value));
    }

    public record BalanceResponse(double balance) {}
}
