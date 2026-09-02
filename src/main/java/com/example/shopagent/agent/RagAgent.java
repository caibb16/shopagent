package com.example.shopagent.agent;

import com.example.shopagent.rag.HybridRetriever;
import com.example.shopagent.rag.PromptTemplates;
import com.example.shopagent.rag.QueryRewriter;
import com.example.shopagent.rag.Reranker;
import com.example.shopagent.session.SessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RagAgent {
    private final ChatClient chatClient;
    private final HybridRetriever retriever;
    private final QueryRewriter queryRewriter;
    private final Reranker reranker;
    private final SessionStore sessionStore;

    public Flux<String> stream(String sessionId, String userText) {
        String rewritten = queryRewriter.rewrite(userText);
        var hits = retriever.retrieve(rewritten, 5);
        var reranked = reranker.rerank(rewritten, hits);
        String kb = reranked.stream()
                .map(h -> "- " + h.text().replace("\n", " "))
                .collect(Collectors.joining("\n"));
        if (kb.isBlank()) kb = "(无相关知识)";
        String summary = sessionStore.getSummary(sessionId).orElse("");
        String history = sessionStore.formatRecentHistory(sessionId, 6);

        return chatClient.prompt()
                .system(PromptTemplates.customerService(kb, summary, history))
                .user(userText)
                .stream().content();
    }
}
