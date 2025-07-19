package com.arctiq.liquidity.balsys.account.transport;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.account.domain.model.Transaction;


@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final BankAccountService bankAccountService;

    public TransactionController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping
    public List<Transaction> getTransactions(
            @RequestParam(value = "start", required = false) Instant start,
            @RequestParam(value = "end", required = false) Instant end) {
        return bankAccountService.getTransactionHistory(start, end);
    }

    @PostMapping
    public ResponseEntity<Void> submitTransaction(@RequestBody Transaction tx) {
        bankAccountService.processTransaction(tx);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/balance")
    public double getBalance() {
        return bankAccountService.retrieveBalance();
    }
}

