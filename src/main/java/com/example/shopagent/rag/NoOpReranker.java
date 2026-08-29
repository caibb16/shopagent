package com.example.shopagent.rag;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default {@link Reranker} implementation: passthrough identity function.
 *
 * <p>Used in BOTH dev and prod profiles. Per MVP scope design doc, the real
 * Cross-Encoder reranker is deferred to v2 — for v1 every {@code Reranker}
 * dependency resolves to this no-op bean.
 */
@Component
public class NoOpReranker implements Reranker {
    @Override
    public List<HybridRetriever.Hit> rerank(String query, List<HybridRetriever.Hit> hits) {
        return hits;
    }
}
