package com.example.shopagent.rag;

import java.util.List;

public interface Reranker {
    List<HybridRetriever.Hit> rerank(String query, List<HybridRetriever.Hit> hits);
}
