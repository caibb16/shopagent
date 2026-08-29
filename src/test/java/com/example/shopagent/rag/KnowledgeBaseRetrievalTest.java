package com.example.shopagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Sanity check that the BM25-only dev retrieval stack returns sensible hits
 * against the real 52-entry FAQ knowledge base (not just synthetic docs).
 */
@SpringBootTest
class KnowledgeBaseRetrievalTest {

    @Autowired
    HybridRetriever retriever;

    @Autowired
    InMemoryBm25Index bm25;

    @Test
    void knowledgeBaseIsFullyIndexed() {
        assertThat(bm25.size()).isEqualTo(52);
    }

    @Test
    void chineseQueriesReturnHits() {
        for (String q : List.of("退货", "发货", "优惠券", "运费", "发票")) {
            assertThat(retriever.retrieve(q, 3))
                    .as("query '%s' should return at least one hit", q)
                    .isNotEmpty();
        }
    }
}
