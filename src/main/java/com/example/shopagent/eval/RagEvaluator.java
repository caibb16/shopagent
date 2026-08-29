package com.example.shopagent.eval;

import com.example.shopagent.rag.HybridRetriever;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RagEvaluator {
    private final HybridRetriever retriever;

    public EvalReport evaluate(List<TestCase> cases) {
        int hits = 0;
        for (TestCase c : cases) {
            var retrieved = retriever.retrieve(c.query(), 5);
            boolean any = retrieved.stream().anyMatch(h -> c.expectedDocIds().contains(h.id()));
            if (any) hits++;
        }
        return new EvalReport(hits * 1.0 / cases.size(), cases.size());
    }

    public record TestCase(String query, List<String> expectedDocIds, String category) {}
    public record EvalReport(double recallAt5, int totalCases) {}
}