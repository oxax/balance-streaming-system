package com.arctiq.liquidity.balsys.audit.dispatch;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ConsoleAuditNotifier implements AuditNotifier {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleAuditNotifier.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public ConsoleAuditNotifier() {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void submit(List<AuditBatch> batches) {
        var batchSummaries = batches.stream()
                .map(batch -> {
                    Map<String, Object> ordered = new LinkedHashMap<>();
                    ordered.put("totalValueOfAllTransactions", batch.getTotalValue().roundedTo3Decimals());
                    ordered.put("countOfTransactions", batch.getTransactionCount());
                    return ordered;
                })
                .toList();

        Map<String, Object> payload = Map.of("submission", Map.of("batches", batchSummaries));

        try {
            String json = mapper.writeValueAsString(payload);
            logger.info("Audit Submission:\n{}", json);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize audit payload", e);
        }
    }
}