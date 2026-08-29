package com.example.shopagent.session;

import java.time.Instant;

public record Message(String role, String content, Instant timestamp) {
    public static Message user(String text) {
        return new Message("user", text, Instant.now());
    }
    public static Message assistant(String text) {
        return new Message("assistant", text, Instant.now());
    }
    public static Message system(String text) {
        return new Message("system", text, Instant.now());
    }
}