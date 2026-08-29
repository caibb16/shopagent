package com.example.shopagent.session;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("dev")
public class InMemorySessionStore implements SessionStore {
    private final Map<String, Deque<Message>> histories = new ConcurrentHashMap<>();
    private final Map<String, String> summaries = new ConcurrentHashMap<>();

    @Override
    public void appendMessage(String sessionId, Message msg) {
        Deque<Message> dq = histories.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        synchronized (dq) {
            dq.addLast(msg);
        }
    }

    @Override
    public List<Message> getHistory(String sessionId, int limit) {
        Deque<Message> dq = histories.get(sessionId);
        if (dq == null) return List.of();
        synchronized (dq) {
            return dq.stream().skip(Math.max(0, dq.size() - limit)).toList();
        }
    }

    @Override
    public void setSummary(String sessionId, String summary) {
        summaries.put(sessionId, summary);
    }

    @Override
    public Optional<String> getSummary(String sessionId) {
        return Optional.ofNullable(summaries.get(sessionId));
    }
}