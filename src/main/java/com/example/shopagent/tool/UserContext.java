package com.example.shopagent.tool;

import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

/**
 * Per-call user identity passed into every tool method. Spring AI 1.0.0-M6
 * delivers this as a {@link ToolContext} parameter to each tool method
 * (server-side only, never visible to the LLM). For legacy callers and
 * non-tool code (escalation logging, etc.) the ThreadLocal variant is kept
 * so existing tests and the HandoffService still work.
 */
public record UserContext(Long userId, String sessionId) {
    private static final ThreadLocal<UserContext> CTX = new ThreadLocal<>();

    public static void set(UserContext ctx) { CTX.set(ctx); }
    public static UserContext current() {
        UserContext c = CTX.get();
        if (c == null) throw new IllegalStateException("UserContext not set in this thread");
        return c;
    }
    public static void clear() { CTX.remove(); }

    /**
     * Resolves the active userId from a Spring AI {@link ToolContext}. The
     * controller populates this map via {@code ChatClient.prompt().toolContext(...)}
     * so the value travels with the call across worker threads and tool-callback
     * threads, surviving where a ThreadLocal would be lost.
     *
     * <p>Contract: if the ToolContext is null or empty, callers fall back to the
     * ThreadLocal {@link #current()} (used by the existing test suite and the
     * HandoffService, which calls the tool synchronously on its own thread).
     */
    public static Long resolveUserId(ToolContext toolContext) {
        if (toolContext != null) {
            Map<String, Object> map = toolContext.getContext();
            if (map != null) {
                Object v = map.get("userId");
                if (v instanceof Number n) return n.longValue();
            }
        }
        return current().userId();
    }
}
