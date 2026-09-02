package com.example.shopagent.rag;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * No-op {@link VectorIndex} that does not compute embeddings.
 * The BM25 leg of {@link HybridRetriever} handles retrieval;
 * the vector leg contributes nothing and is gracefully ignored by RRF.
 */
@Component
public class InMemoryVectorIndex implements VectorIndex {
    private final Map<String, ScoredDoc> docs = new LinkedHashMap<>();

    @Override
    public void upsert(String id, String text, Map<String, Object> metadata) {
        docs.put(id, new ScoredDoc(id, text, 0.0, metadata == null ? Map.of() : metadata));
    }

    @Override
    public List<ScoredDoc> search(String query, int topK) {
        // Dev NoOp: vector leg contributes nothing; BM25 does the work.
        return List.of();
    }

    @Override
    public int size() {
        return docs.size();
    }
}
