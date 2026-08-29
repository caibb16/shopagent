package com.example.shopagent.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueryRewriter {
    private final ChatClient chatClient;

    public String rewrite(String query) {
        String rewritten = chatClient.prompt()
                .user(PromptTemplates.queryRewriter(query))
                .call()
                .content();
        return rewritten == null ? query : rewritten.trim();
    }
}
