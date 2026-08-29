package com.example.shopagent.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserContextTest {
    @AfterEach void cleanup() { UserContext.clear(); }

    @Test
    void setAndCurrentRoundTrips() {
        UserContext.set(new UserContext(42L, "session-1"));
        assertThat(UserContext.current().userId()).isEqualTo(42L);
        assertThat(UserContext.current().sessionId()).isEqualTo("session-1");
    }

    @Test
    void currentWhenUnsetThrows() {
        assertThatThrownBy(UserContext::current)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void clearRemoves() {
        UserContext.set(new UserContext(1L, "x"));
        UserContext.clear();
        assertThatThrownBy(UserContext::current).isInstanceOf(IllegalStateException.class);
    }
}
