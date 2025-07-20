package com.arctiq.liquidity.balsys.transaction.transport;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;
import com.arctiq.liquidity.balsys.exception.TransactionValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final BankAccountService bankAccountService;

    public TransactionController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping
    public List<Transaction> getTransactions(
            @RequestParam(value = "start", required = false) Instant start,
            @RequestParam(value = "end", required = false) Instant end) {

        Instant effectiveStart = start != null ? start : Instant.EPOCH;
        Instant effectiveEnd = end != null ? end : Instant.now();

        return bankAccountService.getTransactionHistory(effectiveStart, effectiveEnd);
    }

    @PostMapping
    public ResponseEntity<OutcomeResponse> submitTransaction(@RequestBody Transaction tx) {
        try {
            bankAccountService.processTransaction(tx);
            return ResponseEntity.accepted().body(new OutcomeResponse("accepted", String.valueOf(tx.id().value())));
        } catch (TransactionValidationException ex) {
            logger.warn("Rejected transaction [{}]: {}", tx.id().value(), ex.getMessage());
            return ResponseEntity.badRequest().body(new OutcomeResponse("invalid", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Unexpected transaction error [{}]: {}", tx.id().value(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OutcomeResponse("error", "Unhandled failure: " + ex.getMessage()));
        }
    }

    public record OutcomeResponse(String status, String detail) {
    }
}