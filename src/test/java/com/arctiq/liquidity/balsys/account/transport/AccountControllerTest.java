package com.arctiq.liquidity.balsys.account.transport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.when;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

@WebFluxTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BankAccountService bankAccountService;

    @MockBean
    private MetricsCollector metricsCollector;

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    @DisplayName("Exposes current balance as JSON via /balance for authorized USER")
    void shouldReturnBalanceForAuthorizedUser() {
        double mockedBalance = 350_000.0;
        when(bankAccountService.retrieveBalance()).thenReturn(mockedBalance);

        webTestClient.get()
                .uri("/account/balance")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.balance").isEqualTo(mockedBalance);
    }
}
