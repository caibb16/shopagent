package com.example.shopagent.rag;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * No-op {@link Reranker}: passthrough identity function.
 * Every {@code Reranker} dependency resolves to this bean until a real
 * Cross-Encoder reranker is introduced.
 */
@Component
public class NoOpReranker implements Reranker {
    @Override
    public List<HybridRetriever.Hit> rerank(String query, List<HybridRetriever.Hit> hits) {
        return hits;
    }
}
