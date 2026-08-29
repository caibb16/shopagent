package com.example.shopagent.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class ChitchatAgent {
    private final ChatClient chatClient;

    public Flux<String> stream(String userText) {
        return chatClient.prompt()
                .system("你是友善的电商客服助手「小蜜」，与用户闲聊。请简洁、礼貌。")
                .user(userText)
                .stream().content();
    }
}