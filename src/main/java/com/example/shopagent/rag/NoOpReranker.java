package com.example.shopagent.rag;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev")
public class NoOpReranker implements Reranker {
    @Override
    public List<HybridRetriever.Hit> rerank(String query, List<HybridRetriever.Hit> hits) {
        return hits;
    }
}
