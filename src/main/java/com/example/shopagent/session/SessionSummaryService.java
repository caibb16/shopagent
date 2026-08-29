package com.example.shopagent.session;

import com.example.shopagent.rag.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Compresses long session histories into a short summary stored back on
 * {@link SessionStore}. Triggered by the controller after each turn; compression
 * runs only when the history length has crossed {@code everyN * 2}.
 *
 * <p>NOTE: Created in Task 16 (out of Task 17 scope) to unblock compile of
 * ChatController. Behaviour matches Task 17's spec: everyN*2 trigger, LLM call
 * via chatClient.call().content(), summary persisted via sessionStore.setSummary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSummaryService {

    private final SessionStore sessionStore;
    private final ChatClient chatClient;

    @Value("${shopagent.session.summary-every-n-turns:5}")
    private int everyN;

    /** Called after every assistant turn. Compresses only when triggered. */
    public void maybeSummarize(String sessionId, int currentHistorySize) {
        int threshold = everyN * 2;
        if (currentHistorySize <= 0 || currentHistorySize % threshold != 0) {
            return;
        }
        try {
            summarize(sessionId);
        } catch (Exception e) {
            // Summary failure must NOT break the user-visible stream.
            log.warn("summary failed for session={}", sessionId, e);
        }
    }

    private void summarize(String sessionId) {
        var history = sessionStore.getHistory(sessionId, 1000);
        if (history.isEmpty()) return;

        String transcript = history.stream()
                .map(m -> m.role() + ": " + m.content())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);

        String prior = sessionStore.getSummary(sessionId).orElse("");

        String compressed = chatClient.prompt()
                .user(PromptTemplates.summary(prior, transcript))
                .call()
                .content();

        if (compressed != null && !compressed.isBlank()) {
            sessionStore.setSummary(sessionId, compressed.trim());
            log.debug("summarized session={} priorLen={} newLen={}",
                    sessionId, prior.length(), compressed.length());
        }
    }
}