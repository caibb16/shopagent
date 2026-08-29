package com.example.shopagent.session;

import java.util.List;
import java.util.Optional;

public interface SessionStore {
    void appendMessage(String sessionId, Message msg);
    List<Message> getHistory(String sessionId, int limit);
    void setSummary(String sessionId, String summary);
    Optional<String> getSummary(String sessionId);
}