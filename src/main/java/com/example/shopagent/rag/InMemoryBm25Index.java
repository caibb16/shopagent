package com.example.shopagent.rag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Plain-Java (no Spring annotations) keyword index using BM25 ranking.
 *
 * <p>Intended only for the dev/test profile; production would use a dedicated
 * full-text engine. Tokenization lowercases and splits on whitespace, then
 * expands CJK runs into character unigrams + bigrams so that Chinese queries
 * match substrings of unsegmented text. Production should use jieba
 * segmentation (see Task notes).
 */
public class InMemoryBm25Index {
    /** id -> raw text. */
    private final Map<String, String> docs = new ConcurrentHashMap<>();
    /** term -> document frequency (number of docs containing the term). */
    private final Map<String, Integer> df = new ConcurrentHashMap<>();
    /** id -> token list for that document. */
    private final Map<String, List<String>> tokens = new ConcurrentHashMap<>();
    /** Running average document length, recomputed on each {@link #add}. */
    private double avgDocLen = 1;

    /** BM25 parameter: term-saturation. */
    private static final double K1 = 1.2;
    /** BM25 parameter: length-normalization. */
    private static final double B = 0.75;

    /**
     * Add (or overwrite) a document in the index.
     *
     * @param id   unique identifier
     * @param text raw text content
     * @param meta metadata (currently unused; kept for symmetry with VectorIndex)
     */
    public void add(String id, String text, Map<String, Object> meta) {
        docs.put(id, text);
        List<String> toks = tokenize(text);
        tokens.put(id, toks);
        Set<String> uniq = new HashSet<>(toks);
        for (String t : uniq) {
            df.merge(t, 1, Integer::sum);
        }
        // Recompute average doc length across all stored docs.
        avgDocLen = docs.values().stream()
                .mapToInt(d -> tokenize(d).size())
                .average()
                .orElse(1);
    }

    /**
     * BM25 search. Returns at most {@code topK} docs, sorted by descending score.
     *
     * @param query free-text query
     * @param topK  maximum number of results
     */
    public List<ScoredDoc> search(String query, int topK) {
        List<String> qTokens = tokenize(query);
        if (qTokens.isEmpty() || docs.isEmpty()) {
            return List.of();
        }
        Map<String, Double> scores = new HashMap<>();
        int n = docs.size();
        for (String t : qTokens) {
            int dft = df.getOrDefault(t, 0);
            if (dft == 0) continue;
            // IDF with the +1 smoothing (Lucene BM25 variant).
            double idf = Math.log(1 + (n - dft + 0.5) / (dft + 0.5));
            for (var entry : tokens.entrySet()) {
                int tf = Collections.frequency(entry.getValue(), t);
                if (tf == 0) continue;
                int dl = entry.getValue().size();
                // BM25 length normalization.
                double norm = (1 - B) + B * dl / avgDocLen;
                double s = idf * (tf * (K1 + 1)) / (tf + K1 * norm);
                scores.merge(entry.getKey(), s, Double::sum);
            }
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new ScoredDoc(e.getKey(), docs.get(e.getKey()), e.getValue(), Map.of()))
                .collect(Collectors.toList());
    }

    /** Total documents in the index. */
    public int size() {
        return docs.size();
    }

    /**
     * Lowercase, split on whitespace, then expand CJK runs into character
     * unigrams + bigrams.
     *
     * <p>Whitespace splitting alone is wrong for Chinese, which is written
     * without word delimiters: the doc {@code "支持7天无理由退货 拆封 不退"}
     * would yield the single token {@code "支持7天无理由退货"}, which a query
     * of {@code "退货"} could never match even though it is a substring.
     * Emitting character n-grams for CJK runs restores substring matchability
     * while keeping ASCII words whole (so {@code "顺丰"} and {@code "sf"} both
     * behave sensibly).
     *
     * <p>Bigrams carry the useful signal — they approximate words and keep
     * precision — while unigrams provide recall fallback for single-character
     * queries. Production should swap this for jieba segmentation.
     */
    private List<String> tokenize(String s) {
        if (s == null || s.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String chunk : s.toLowerCase().split("\\s+")) {
            if (chunk.isBlank()) continue;
            // Walk the chunk, accumulating maximal runs of CJK characters.
            int i = 0;
            while (i < chunk.length()) {
                if (isCjk(chunk.charAt(i))) {
                    int start = i;
                    while (i < chunk.length() && isCjk(chunk.charAt(i))) i++;
                    String run = chunk.substring(start, i);
                    for (int j = 0; j < run.length(); j++) {
                        out.add(String.valueOf(run.charAt(j)));
                        if (j + 1 < run.length()) {
                            out.add(run.substring(j, j + 2));
                        }
                    }
                } else {
                    int start = i;
                    while (i < chunk.length() && !isCjk(chunk.charAt(i))) i++;
                    out.add(chunk.substring(start, i));
                }
            }
        }
        return out;
    }

    /** True for CJK Unified Ideographs (the range that covers common Chinese). */
    private static boolean isCjk(char c) {
        return c >= '一' && c <= '鿿';
    }
}
