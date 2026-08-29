package com.example.shopagent.agent;

import com.example.shopagent.rag.PromptTemplates;
import com.example.shopagent.session.SessionStore;
import com.example.shopagent.tool.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolAgent {
    private final ChatClient chatClient;
    private final OrderTool orderTool;
    private final LogisticsTool logisticsTool;
    private final RefundTool refundTool;
    private final CouponTool couponTool;
    private final RecommendTool recommendTool;
    private final EscalateTool escalateTool;
    private final SessionStore sessionStore;

    public Flux<String> stream(long userId, String sessionId, String userText) {
        String summary = sessionStore.getSummary(sessionId).orElse("");
        String kb = "(工具型请求，优先调用工具)";
        return chatClient.prompt()
                .system(PromptTemplates.customerService(kb, summary))
                .user(userText)
                .tools(orderTool, logisticsTool, refundTool, couponTool, recommendTool, escalateTool)
                .stream().content();
    }
}