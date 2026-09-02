package com.example.shopagent.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AgentService {
    private final IntentClassifier intentClassifier;
    private final ChitchatAgent chitchatAgent;
    private final RagAgent ragAgent;
    private final ToolAgent toolAgent;
    private final HandoffService handoffService;

    public Flux<String> handle(long userId, String sessionId, String userText) {
        Intent intent = intentClassifier.classifyWithUserContext(userText, userId);
        return switch (intent) {
            case CHITCHAT -> chitchatAgent.stream(sessionId, userText);
            case INQUIRY  -> ragAgent.stream(sessionId, userText);
            case ACTION   -> toolAgent.stream(userId, sessionId, userText);
            case COMPLAINT -> handoffService.handoff(userId, sessionId, userText);
        };
    }
}