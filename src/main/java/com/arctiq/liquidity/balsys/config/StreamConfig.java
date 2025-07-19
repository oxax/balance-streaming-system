package com.arctiq.liquidity.balsys.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.producer.channel.CreditProducer;
import com.arctiq.liquidity.balsys.producer.channel.DebitProducer;
import com.arctiq.liquidity.balsys.producer.config.ProducerConfig;
import com.arctiq.liquidity.balsys.producer.orchestration.TransactionProducerOrchestrator;

@Configuration
public class StreamConfig {

    @Bean
    public ExecutorService auditExecutor() {
        return Executors.newFixedThreadPool(2); // used by audit processing and producer orchestrator
    }

    @Bean
    public CreditProducer creditProducer() {
        return new CreditProducer();
    }

    @Bean
    public DebitProducer debitProducer() {
        return new DebitProducer();
    }

    @Bean
    public TransactionProducerOrchestrator producerOrchestrator(
            CreditProducer creditProducer,
            DebitProducer debitProducer,
            BankAccountService accountService,
            LinkedTransferQueue<Transaction> transactionQueue,
            ExecutorService auditExecutor) {
        return new TransactionProducerOrchestrator(
                creditProducer,
                debitProducer,
                accountService,
                transactionQueue,
                auditExecutor);
    }

    @Bean
    public CommandLineRunner ingestionStarter(TransactionProducerOrchestrator orchestrator) {
        return args -> {
            // emits 1000 transactions over 60 seconds per stream (≈16.67 tx/sec)
            orchestrator.startEmitLoops(new ProducerConfig(1000, 60));
        };
    }
}