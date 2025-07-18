package com.jpc.exercise.audit.infrastructure.notifier;

import java.util.List;
import java.util.Map;

import com.jpc.exercise.audit.domain.model.AuditBatch;
import com.jpc.exercise.shared.audit.AuditNotifier;

public class ConsoleAuditNotifier implements AuditNotifier {

    @Override
    public void submit(List<AuditBatch> batches) {
        var submission = Map.of("submission", Map.of("batches", batches.stream()
            .map(batch -> Map.of(
                "totalValueOfAllTransactions", batch.getTotalValue(),
                "countOfTransactions", batch.getTransactionCount()))
            .toList()));

        System.out.println(prettyPrint(submission));
    }

    private String prettyPrint(Object obj) {
        return "{\n  \"submission\": {\n" +
            "    \"batches\": [\n" +
            ((List<?>) ((Map<?, ?>) ((Map<?, ?>) obj).get("submission")).get("batches")).stream()
                .map(b -> "      " + b.toString().replace("=", ":"))
                .reduce((a, b) -> a + ",\n" + b).orElse("") +
            "\n    ]\n  }\n}";
    }
}
