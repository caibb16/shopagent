package com.example.shopagent.agent;

public enum Intent {
    CHITCHAT, INQUIRY, ACTION, COMPLAINT;

    public static Intent fromRaw(String raw) {
        if (raw == null) return CHITCHAT;
        try { return Intent.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return CHITCHAT; }
    }
}
