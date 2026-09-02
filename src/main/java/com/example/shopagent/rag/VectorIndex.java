package com.example.shopagent.rag;

import java.util.List;
import java.util.Map;

/**
 * Abstraction over a vector store used by the RAG layer.
 *
 * <p>Implementation: in-memory ({@link InMemoryVectorIndex}).
 */
public interface VectorIndex {

    /** Upsert a document. Embedding is computed internally. */
    void upsert(String id, String text, Map<String, Object> metadata);

    /** Search topK by similarity. */
    List<ScoredDoc> search(String query, int topK);

    /** Total docs (for sanity / metrics). */
    int size();
}
