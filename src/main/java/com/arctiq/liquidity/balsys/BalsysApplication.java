package com.arctiq.liquidity.balsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BalsysApplication {
    public static void main(String[] args) {
        SpringApplication.run(BalsysApplication.class, args);
    }
}