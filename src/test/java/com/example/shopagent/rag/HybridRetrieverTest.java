package com.example.shopagent.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class HybridRetrieverTest {
    private HybridRetriever retriever;

    @BeforeEach
    void setUp() {
        InMemoryBm25Index bm25 = new InMemoryBm25Index();
        var vec = new InMemoryVectorIndex();
        retriever = new HybridRetriever(vec, bm25);
        bm25.add("D1", "支持7天无理由退货 拆封 不退", Map.of());
        bm25.add("D2", "下单后48小时内发货 顺丰 圆通", Map.of());
        bm25.add("D3", "优惠券过期 自动作废", Map.of());
    }

    @Test
    void rrfFusionCombinesResults() {
        List<String> ids = retriever.retrieve("退货", 3).stream().map(HybridRetriever.Hit::id).toList();
        assertThat(ids).contains("D1");
    }

    @Test
    void rrfFusionReturnsAtMostTopK() {
        List<HybridRetriever.Hit> hits = retriever.retrieve("发货", 2);
        assertThat(hits.size()).isLessThanOrEqualTo(2);
    }
}
