package com.example.shopagent.eval;

import com.example.shopagent.rag.HybridRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagEvaluatorTest {
    private HybridRetriever retriever;
    private RagEvaluator evaluator;

    @BeforeEach
    void setUp() {
        retriever = mock(HybridRetriever.class);
        when(retriever.retrieve(anyString(), anyInt()))
                .thenReturn(List.of(new HybridRetriever.Hit("F001", "退货", 1.0)));
        evaluator = new RagEvaluator(retriever);
    }

    @Test
    void recallAt5IsComputed() {
        var cases = List.of(
                new RagEvaluator.TestCase("退货", List.of("F001"), "退货"),
                new RagEvaluator.TestCase("物流", List.of("F002"), "物流"));
        var report = evaluator.evaluate(cases);
        assertThat(report.recallAt5()).isEqualTo(0.5);
    }
}