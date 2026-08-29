package com.example.shopagent.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class InMemorySessionStoreTest {
    private final SessionStore store = new InMemorySessionStore();

    @Test
    void appendAndGetHistoryReturnsInOrder() {
        store.appendMessage("s1", Message.user("hi"));
        store.appendMessage("s1", Message.assistant("hello"));
        List<Message> history = store.getHistory("s1", 10);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).role()).isEqualTo("user");
        assertThat(history.get(1).role()).isEqualTo("assistant");
    }

    @Test
    void getHistoryLimitTruncates() {
        for (int i = 0; i < 20; i++) store.appendMessage("s", Message.user("m" + i));
        assertThat(store.getHistory("s", 5)).hasSize(5);
    }

    @Test
    void summaryRoundTrip() {
        store.setSummary("s", "discussed refund");
        assertThat(store.getSummary("s")).hasValueSatisfying(value -> assertThat(value).contains("refund"));
    }

    @Test
    void summaryMissingReturnsEmpty() {
        assertThat(store.getSummary("nope")).isEmpty();
    }

    @Test
    void separateSessionsAreIsolated() {
        store.appendMessage("a", Message.user("x"));
        store.appendMessage("b", Message.user("y"));
        assertThat(store.getHistory("a", 10)).hasSize(1);
        assertThat(store.getHistory("b", 10)).hasSize(1);
    }
}