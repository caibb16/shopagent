package com.example.shopagent.api.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

/**
 * Builds an SSE {@code "error"} event from a Throwable. Used by the WebFlux
 * ChatController to surface stream failures as a final SSE event before the
 * Flux completes. Non-SSE failures go through {@link GlobalExceptionHandler}.
 */
@Slf4j
@Component
public class SseErrorSender {

    public ServerSentEvent<String> toEvent(Throwable err) {
        log.warn("SSE error: {}", err.toString());
        String body = "{\"error\":\"" + err.getClass().getSimpleName() + "\",\"message\":\""
                + safe(err.getMessage()) + "\"}";
        return ServerSentEvent.<String>builder()
                .event("error")
                .data(body)
                .build();
    }

    public static ServerSentEvent<String> dataEvent(String data) {
        return ServerSentEvent.<String>builder().data(data).build();
    }

    public static ServerSentEvent<String> namedEvent(String name, String data) {
        return ServerSentEvent.<String>builder().event(name).data(data).build();
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}