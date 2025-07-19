package com.arctiq.liquidity.balsys.account.transport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.when;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;

@WebFluxTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BankAccountService bankAccountService;

    @Test
    void shouldReturnBalanceViaRest() {
        when(bankAccountService.retrieveBalance()).thenReturn(350_000.0);
        webTestClient.get().uri("/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(350_000.0);
    }
}