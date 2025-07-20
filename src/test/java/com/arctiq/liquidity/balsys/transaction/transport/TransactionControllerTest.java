package com.arctiq.liquidity.balsys.transaction.transport;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.testfixtures.AuditTestFixtures;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;
import com.arctiq.liquidity.balsys.transaction.core.TransactionId;
import com.arctiq.liquidity.balsys.transaction.core.outcome.TransactionAccepted;
import com.arctiq.liquidity.balsys.exception.TransactionValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@WebFluxTest(TransactionController.class)
class TransactionControllerTest {

        @Autowired
        private WebTestClient webTestClient;

        @MockBean
        private BankAccountService accountService;

        @MockBean
        private MetricsCollector metricsCollector;

        @Test
        @DisplayName("Accepts valid transaction and returns status accepted")
        void shouldAcceptTransaction() {
                Transaction tx = AuditTestFixtures.fixedTransaction(300_000.0);

                // Simulate a successful transaction with no side effects verified externally
                doNothing().when(accountService).processTransaction(tx);

                webTestClient.post()
                                .uri("/transactions/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(tx)
                                .exchange()
                                .expectStatus().isAccepted()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo("accepted")
                                .jsonPath("$.detail").isEqualTo(String.valueOf(tx.id().value()));

                // No need to verify metricsCollector — handled internally by service
        }

        @Test
        @DisplayName("Rejects invalid transaction and returns status invalid")
        void shouldRejectInvalidTransaction() {
                Transaction tx = new Transaction(TransactionId.generate(), Money.of(199.0));
                doThrow(new TransactionValidationException("Transaction amount is out of range"))
                                .when(accountService).processTransaction(tx);

                webTestClient.post()
                                .uri("/transactions/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(tx)
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo("invalid")
                                .jsonPath("$.detail").value(val -> ((String) val).contains("out of range"));
        }

        @Test
        @DisplayName("Handles service failure with internal error")
        void shouldHandleUnexpectedError() {
                Transaction tx = AuditTestFixtures.fixedTransaction(500_000.0);

                doThrow(new RuntimeException("Database unreachable"))
                                .when(accountService).processTransaction(tx);

                webTestClient.post()
                                .uri("/transactions/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(tx)
                                .exchange()
                                .expectStatus().is5xxServerError()
                                .expectBody()
                                .jsonPath("$.status").isEqualTo("error")
                                .jsonPath("$.detail").value(val -> ((String) val).contains("Unhandled failure"));
        }

        @Test
        @DisplayName("Returns transactions between dates")
        void shouldReturnTransactionHistory() {
                Instant start = Instant.parse("2025-01-01T00:00:00Z");
                Instant end = Instant.parse("2025-01-31T23:59:59Z");

                List<Transaction> mockTxs = AuditTestFixtures.mockSmallTransactions();
                when(accountService.getTransactionHistory(start, end)).thenReturn(mockTxs);

                webTestClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/transactions/history")
                                                .queryParam("start", start.toString())
                                                .queryParam("end", end.toString())
                                                .build())
                                .exchange()
                                .expectStatus().isOk()
                                .expectBodyList(Transaction.class)
                                .hasSize(mockTxs.size());
        }
}