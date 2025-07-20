package com.arctiq.liquidity.balsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.arctiq.liquidity.balsys.producer.config.ProducerSettings;

@SpringBootApplication
@EnableConfigurationProperties(ProducerSettings.class)
public class BalsysApplication {
    public static void main(String[] args) {
        SpringApplication.run(BalsysApplication.class, args);
    }
}