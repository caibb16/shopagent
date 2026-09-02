package com.example.shopagent.rag;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Dev-profile {@link VectorIndex} that intentionally does NOT compute embeddings.
 *
 * <p>Rationale: Spring AI's bundled ONNX-based {@code TransformersEmbeddingModel}
 * requires an ONNX runtime load that fails on JDK 25 due to a model-protobuf
 * mismatch in {@code spring-ai-transformers:1.0.0-M6}. For dev we don't need a
 * real embedding model — the BM25 leg of {@link HybridRetriever} handles FAQ
 * retrieval well enough, and RRF gracefully ignores an empty vector leg.
 * Production (prod profile) uses QdrantVectorIndex with a real embedding
 * model — see Task 19.
 */
@Component
@Profile("!prod")
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
