package com.example.shopagent.rag;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid retriever that fuses results from a {@link VectorIndex} (semantic)
 * and an {@link InMemoryBm25Index} (keyword) via Reciprocal Rank Fusion (RRF).
 *
 * <p>RRF score for each doc: {@code sum(1 / (k + rank))} across all source
 * rankings, with {@code k=60}. Ranks start at 1.
 */
public class HybridRetriever {
    private final VectorIndex vectorIndex;
    private final InMemoryBm25Index bm25Index;
    private static final int RRF_K = 60;

    public HybridRetriever(VectorIndex vectorIndex, InMemoryBm25Index bm25Index) {
        this.vectorIndex = vectorIndex;
        this.bm25Index = bm25Index;
    }

    /**
     * Retrieve the topK most relevant documents for the given query.
     *
     * @param query user query (text)
     * @param topK  maximum number of hits to return
     * @return list of {@link Hit}s, sorted by descending RRF score
     */
    public List<Hit> retrieve(String query, int topK) {
        // Pull a wider candidate set so fusion has something to combine.
        List<ScoredDoc> vec = vectorIndex.search(query, Math.max(1, topK * 2));
        List<ScoredDoc> kw = bm25Index.search(query, Math.max(1, topK * 2));

        Map<String, Double> rrf = new HashMap<>();
        Map<String, ScoredDoc> docMap = new HashMap<>();
        for (int i = 0; i < vec.size(); i++) {
            rrf.merge(vec.get(i).id(), 1.0 / (RRF_K + i + 1), Double::sum);
            docMap.putIfAbsent(vec.get(i).id(), vec.get(i));
        }
        for (int i = 0; i < kw.size(); i++) {
            rrf.merge(kw.get(i).id(), 1.0 / (RRF_K + i + 1), Double::sum);
            docMap.putIfAbsent(kw.get(i).id(), kw.get(i));
        }
        return rrf.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new Hit(e.getKey(), docMap.get(e.getKey()).text(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * A retrieval hit. Score is the RRF fusion score (higher = more relevant).
     */
    public record Hit(String id, String text, double score) {}
}
