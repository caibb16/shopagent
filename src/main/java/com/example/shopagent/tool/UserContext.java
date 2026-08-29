package com.example.shopagent.tool;

public record UserContext(Long userId, String sessionId) {
    private static final ThreadLocal<UserContext> CTX = new ThreadLocal<>();

    public static void set(UserContext ctx) { CTX.set(ctx); }
    public static UserContext current() {
        UserContext c = CTX.get();
        if (c == null) throw new IllegalStateException("UserContext not set in this thread");
        return c;
    }
    public static void clear() { CTX.remove(); }
}
