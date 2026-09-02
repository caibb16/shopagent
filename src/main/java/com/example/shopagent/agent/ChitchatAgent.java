package com.example.shopagent.agent;

import com.example.shopagent.session.SessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class ChitchatAgent {
    private final ChatClient chatClient;
    private final SessionStore sessionStore;

    public Flux<String> stream(String sessionId, String userText) {
        String history = sessionStore.formatRecentHistory(sessionId, 6);
        String systemPrompt = "你是友善的电商客服助手「小蜜」，与用户闲聊。请简洁、礼貌。\n\n【近期对话】\n" + history;
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userText)
                .stream().content();
    }
}
