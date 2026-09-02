package com.example.shopagent.session;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface SessionStore {
    void appendMessage(String sessionId, Message msg);
    List<Message> getHistory(String sessionId, int limit);
    void setSummary(String sessionId, String summary);
    Optional<String> getSummary(String sessionId);

    /** Build a recent-history string for prompt injection. */
    default String formatRecentHistory(String sessionId, int maxTurns) {
        var history = getHistory(sessionId, maxTurns);
        if (history.isEmpty()) return "(无历史对话)";
        return history.stream()
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n"));
    }
}