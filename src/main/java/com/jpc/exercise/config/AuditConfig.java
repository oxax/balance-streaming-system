package com.jpc.exercise.config;

public class AuditConfig {
    private final int submissionThreshold;

    public AuditConfig(int submissionThreshold) {
        this.submissionThreshold = submissionThreshold;
    }

    public int getSubmissionThreshold() {
        return submissionThreshold;
    }
}
