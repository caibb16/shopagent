package com.example.shopagent.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Prod-profile {@link SessionStore} backed by Redis.
 *
 * <p>History is stored as a Redis list under {@code session:hist:<sid>};
 * summaries as a string under {@code session:sum:<sid>}. Both keys are
 * scoped per session id so multiple sessions coexist cleanly.
 */
@Component
@RequiredArgsConstructor
public class RedisSessionStore implements SessionStore {

    private static final String HIST_KEY = "session:hist:";
    private static final String SUM_KEY = "session:sum:";

    private final RedisTemplate<String, String> redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void appendMessage(String sessionId, Message msg) {
        try {
            redis.opsForList().rightPush(HIST_KEY + sessionId, mapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    @Override
    public List<Message> getHistory(String sessionId, int limit) {
        List<String> raw = redis.opsForList().range(HIST_KEY + sessionId, -limit, -1);
        if (raw == null || raw.isEmpty()) return List.of();
        List<Message> out = new ArrayList<>(raw.size());
        for (String s : raw) {
            try {
                out.add(mapper.readValue(s, Message.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize message", e);
            }
        }
        return out;
    }

    @Override
    public void setSummary(String sessionId, String summary) {
        redis.opsForValue().set(SUM_KEY + sessionId, summary);
    }

    @Override
    public Optional<String> getSummary(String sessionId) {
        return Optional.ofNullable(redis.opsForValue().get(SUM_KEY + sessionId));
    }
}