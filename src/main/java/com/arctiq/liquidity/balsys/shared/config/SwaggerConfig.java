package com.arctiq.liquidity.balsys.shared.config;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI balsysOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Liquidity Balance Streaming API")
                        .version("1.0.0")
                        .description("""
                                    This system exposes real-time credit/debit transaction ingestion,
                                    balance tracking, audit batching, and operational metrics endpoints.

                                    Audit outcomes and performance stats are available through REST,
                                    with domain boundaries enforced via modular packaging.
                                """)
                        .contact(new Contact()
                                .name("Arctiq Engineering")
                                .email("tobiosa.dev@gmail.com")
                                .url("https://github.com/oxax/liquidity-balsys"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}