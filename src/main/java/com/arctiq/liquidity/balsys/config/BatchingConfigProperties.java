package com.arctiq.liquidity.balsys.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "transaction.batching")
public class BatchingConfigProperties {
    private String strategy;
    private double valueLimit;

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public double getValueLimit() {
        return valueLimit;
    }

    public void setValueLimit(double valueLimit) {
        this.valueLimit = valueLimit;
    }

}