package com.arctiq.liquidity.balsys.config;
public class AuditConfig {
    private final int submissionThreshold;

    public AuditConfig(int submissionThreshold) {
        this.submissionThreshold = submissionThreshold;
    }

    public int getSubmissionThreshold() {
        return submissionThreshold;
    }
}
